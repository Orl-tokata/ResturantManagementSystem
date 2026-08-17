package com.resturant.management.rms.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Milestone 7 — CRUD, role enforcement and the delete guards. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional   // roll back after each test: one H2 instance is shared by every test class
class MasterDataControllerTest {

	@Autowired MockMvc mvc;
	@Autowired ObjectMapper json;

	private String adminToken;
	private String cashierToken;

	@BeforeEach
	void signIn() throws Exception {
		adminToken = token("admin");
		cashierToken = token("cashier");
	}

	private String token(String username) throws Exception {
		MvcResult res = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"%s\",\"password\":\"ChangeMe123!\"}".formatted(username)))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode node = json.readTree(res.getResponse().getContentAsString());
		return node.path("data").path("accessToken").asText();
	}

	/* ---- Reads ----------------------------------------------------------- */

	@Test
	@DisplayName("seeded categories and products are listed")
	void listsSeedData() throws Exception {
		mvc.perform(get("/api/categories").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(8));

		mvc.perform(get("/api/products").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(16));

		mvc.perform(get("/api/tables").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(12));
	}

	@Test
	@DisplayName("product list filters by category and by search term")
	void productFilters() throws Exception {
		mvc.perform(get("/api/products?categoryId=7").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(4));   // drinks

		mvc.perform(get("/api/products?search=Angkor").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	@DisplayName("/categories/active excludes the inactive one")
	void activeCategoriesOnly() throws Exception {
		mvc.perform(get("/api/categories/active").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(7));        // 8 seeded, 1 INACTIVE
	}

	@Test
	@DisplayName("table summary matches the seeded statuses")
	void tableSummary() throws Exception {
		mvc.perform(get("/api/tables/summary").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.free").value(6))
				.andExpect(jsonPath("$.data.occupied").value(5))
				.andExpect(jsonPath("$.data.reserved").value(1));
	}

	/* ---- Role enforcement ------------------------------------------------ */

	@Test
	@DisplayName("a cashier can read the catalog but not write to it")
	void cashierIsReadOnly() throws Exception {
		mvc.perform(get("/api/products").header("Authorization", "Bearer " + cashierToken))
				.andExpect(status().isOk());

		mvc.perform(post("/api/categories")
						.header("Authorization", "Bearer " + cashierToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"ថ្មី","nameEn":"New"}"""))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("staff records are ADMIN-only, even for reads")
	void staffIsAdminOnly() throws Exception {
		mvc.perform(get("/api/staff").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(7));

		mvc.perform(get("/api/staff").header("Authorization", "Bearer " + cashierToken))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("no token means 401, not 403")
	void anonymousRejected() throws Exception {
		mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
	}

	/* ---- Write path ------------------------------------------------------- */

	@Test
	@DisplayName("create, read back, update, then delete a category")
	void categoryLifecycle() throws Exception {
		MvcResult created = mvc.perform(post("/api/categories")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"អាហារសម្រន់","nameEn":"Snacks","icon":"🍿","sortOrder":9}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.nameEn").value("Snacks"))
				.andExpect(jsonPath("$.data.productCount").value(0))
				.andReturn();

		long id = json.readTree(created.getResponse().getContentAsString())
				.path("data").path("id").asLong();

		mvc.perform(put("/api/categories/" + id)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"អាហារសម្រន់","nameEn":"Snacks & Sides","icon":"🍿","sortOrder":9,"status":"INACTIVE"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.nameEn").value("Snacks & Sides"))
				.andExpect(jsonPath("$.data.status").value("INACTIVE"));

		mvc.perform(delete("/api/categories/" + id)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());

		mvc.perform(get("/api/categories/" + id).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("deleting a category that still has products is a 409 with a useful message")
	void cannotDeleteCategoryInUse() throws Exception {
		mvc.perform(delete("/api/categories/1").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("still used by")));
	}

	@Test
	@DisplayName("duplicate table name is rejected, but renaming a table to itself is allowed")
	void tableNameUniqueness() throws Exception {
		mvc.perform(post("/api/tables")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Table 01","seats":4}"""))
				.andExpect(status().isConflict());

		// Table 01 keeping its own name must not collide with itself.
		mvc.perform(put("/api/tables/1")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Table 01","seats":6,"zone":"OUTDOOR"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.seats").value(6))
				.andExpect(jsonPath("$.data.zone").value("OUTDOOR"));
	}

	@Test
	@DisplayName("an occupied table cannot be deleted")
	void cannotDeleteOccupiedTable() throws Exception {
		mvc.perform(delete("/api/tables/2").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("occupied")));
	}

	@Test
	@DisplayName("a cashier may change table status, since seating is their job")
	void cashierCanChangeTableStatus() throws Exception {
		mvc.perform(patch("/api/tables/3/status")
						.header("Authorization", "Bearer " + cashierToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"OCCUPIED"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("OCCUPIED"));

		mvc.perform(patch("/api/tables/3/status")
				.header("Authorization", "Bearer " + cashierToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status":"FREE"}"""))
				.andExpect(status().isOk());
	}

	/* ---- Validation -------------------------------------------------------- */

	@Test
	@DisplayName("a negative price is rejected with a field-level message")
	void rejectsNegativePrice() throws Exception {
		mvc.perform(post("/api/products")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Bad","categoryId":1,"price":-5}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("price")));
	}

	@Test
	@DisplayName("a product pointing at a missing category is a 404, not a 500")
	void rejectsUnknownCategory() throws Exception {
		mvc.perform(post("/api/products")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Orphan","categoryId":9999,"price":1.00}"""))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("an unknown enum value is a 400, not a 500")
	void rejectsBadEnum() throws Exception {
		mvc.perform(get("/api/tables?zone=BASEMENT").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("duplicate staff code is rejected")
	void duplicateStaffCode() throws Exception {
		mvc.perform(post("/api/staff")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"staffCode":"EMP-001","staffName":"Someone Else","role":"WAITER"}"""))
				.andExpect(status().isConflict());
	}
}
