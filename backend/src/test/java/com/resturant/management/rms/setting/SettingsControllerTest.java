package com.resturant.management.rms.setting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Milestone 13 — settings and self-service profile. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional   // roll back after each test: one H2 instance is shared by every test class
class SettingsControllerTest {

	@Autowired MockMvc mvc;
	@Autowired ObjectMapper json;

	private String admin;
	private String cashier;

	@BeforeEach
	void signIn() throws Exception {
		admin = token("admin");
		cashier = token("cashier");
	}

	private String token(String username) throws Exception {
		MvcResult res = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"%s\",\"password\":\"ChangeMe123!\"}".formatted(username)))
				.andReturn();
		return "Bearer " + json.readTree(res.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();
	}

	/* ---- Settings ---------------------------------------------------------- */

	@Test
	@DisplayName("any authenticated user can read settings — the POS needs the rates")
	void readableByAnyone() throws Exception {
		mvc.perform(get("/api/settings").header("Authorization", cashier))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data['sales.vatRate']").value("10"))
				.andExpect(jsonPath("$.data['currency.khrRate']").value("4100"))
				.andExpect(jsonPath("$.data['restaurant.nameEn']").value("Angkor Restaurant"));

		mvc.perform(get("/api/settings")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("only an admin can write settings")
	void writeIsAdminOnly() throws Exception {
		mvc.perform(put("/api/settings").header("Authorization", cashier)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sales.vatRate":"0"}"""))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("a partial update leaves the other keys alone")
	void partialUpdate() throws Exception {
		mvc.perform(put("/api/settings").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"restaurant.phone":"099 888 777"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data['restaurant.phone']").value("099 888 777"))
				.andExpect(jsonPath("$.data['currency.khrRate']").value("4100"));
	}

	@Test
	@DisplayName("a new key can be added")
	void addsNewKey() throws Exception {
		mvc.perform(put("/api/settings").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"option.newThing":"true"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data['option.newThing']").value("true"));
	}

	@Test
	@DisplayName("a non-numeric VAT rate is rejected before it can corrupt bills")
	void rejectsNonNumericRate() throws Exception {
		mvc.perform(put("/api/settings").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sales.vatRate":"ten percent"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("must be a number")));
	}

	@Test
	@DisplayName("out-of-range rates are rejected")
	void rejectsOutOfRange() throws Exception {
		mvc.perform(put("/api/settings").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sales.vatRate":"150"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("cannot exceed 100")));

		mvc.perform(put("/api/settings").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"currency.khrRate":"0"}"""))
				.andExpect(status().isBadRequest());

		mvc.perform(put("/api/settings").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sales.vatRate":"-5"}"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("changing the VAT rate changes what the next bill charges")
	void vatRateAffectsNewBills() throws Exception {
		mvc.perform(put("/api/settings").header("Authorization", admin)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"sales.vatRate":"0"}"""));

		MvcResult opened = mvc.perform(post("/api/orders").header("Authorization", cashier)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"tableId":1}"""))
				.andReturn();
		long id = json.readTree(opened.getResponse().getContentAsString())
				.path("data").path("id").asLong();

		// 2 × 4.50 with VAT switched off → total is the subtotal.
		mvc.perform(put("/api/orders/" + id + "/items").header("Authorization", cashier)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"items":[{"productId":1,"qty":2}]}"""))
				.andExpect(jsonPath("$.data.vatRate").value(0))
				.andExpect(jsonPath("$.data.vatAmount").value(0))
				.andExpect(jsonPath("$.data.total").value(9.00));
	}

	/* ---- Profile ----------------------------------------------------------- */

	@Test
	@DisplayName("a user can edit their own name, email and phone")
	void updatesOwnProfile() throws Exception {
		mvc.perform(put("/api/auth/me").header("Authorization", cashier)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Sok Dara Jr","email":"dara@rms.local","phone":"012 000 111"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.fullName").value("Sok Dara Jr"))
				.andExpect(jsonPath("$.data.email").value("dara@rms.local"))
				.andExpect(jsonPath("$.data.phone").value("012 000 111"));

		mvc.perform(get("/api/auth/me").header("Authorization", cashier))
				.andExpect(jsonPath("$.data.fullName").value("Sok Dara Jr"));
	}

	@Test
	@DisplayName("a profile edit cannot change the caller's role")
	void cannotEscalateRole() throws Exception {
		mvc.perform(put("/api/auth/me").header("Authorization", cashier)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Sneaky","role":"ADMIN","username":"admin"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.role").value("CASHIER"))
				.andExpect(jsonPath("$.data.username").value("cashier"));
	}

	@Test
	@DisplayName("taking another user's email is a 409")
	void rejectsDuplicateEmail() throws Exception {
		mvc.perform(put("/api/auth/me").header("Authorization", cashier)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Sok Dara","email":"admin@rms.local"}"""))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("a blank name is rejected")
	void rejectsBlankName() throws Exception {
		mvc.perform(put("/api/auth/me").header("Authorization", cashier)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"","email":"x@y.com"}"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("editing a profile requires authentication")
	void profileRequiresAuth() throws Exception {
		mvc.perform(put("/api/auth/me")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Nobody"}"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("changing a password then signing in with the new one works")
	void changePasswordEndToEnd() throws Exception {
		mvc.perform(post("/api/auth/change-password").header("Authorization", cashier)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"currentPassword":"ChangeMe123!","newPassword":"BrandNew123"}"""))
				.andExpect(status().isOk());

		mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"cashier","password":"BrandNew123"}"""))
				.andExpect(status().isOk());

		mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"cashier","password":"ChangeMe123!"}"""))
				.andExpect(status().isUnauthorized());
	}
}
