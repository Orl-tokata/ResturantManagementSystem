package com.resturant.management.rms.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resturant.management.rms.catalog.CategoryRepository;
import com.resturant.management.rms.dining.DiningTableRepository;
import com.resturant.management.rms.dining.TableStatus;
import com.resturant.management.rms.order.OrderItemRepository;
import com.resturant.management.rms.order.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The log has to answer "who changed this price, from what, to what, when".
 *
 * <p>Not {@code @Transactional}: audit rows are written from an
 * {@code afterCommit} callback, so a test that rolls back would produce none and
 * pass by proving nothing. Everything created here is therefore real, and is
 * named with a marker so it can be told apart.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditLogTest {

	private static final String MARKER = "ZZ-AUDIT-TEST";

	@Autowired MockMvc mvc;
	@Autowired ObjectMapper json;
	@Autowired AuditLogRepository repository;
	@Autowired CategoryRepository categories;
	@Autowired OrderRepository orders;
	@Autowired OrderItemRepository orderItems;
	@Autowired DiningTableRepository tables;

	private String token;
	private final List<Long> createdOrders = new ArrayList<>();

	@BeforeEach
	void signIn() throws Exception {
		MvcResult res = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin","password":"ChangeMe123!"}"""))
				.andReturn();
		token = "Bearer " + json.readTree(res.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();
	}

	/**
	 * Everything this class creates is real, so it has to be taken back out.
	 * One H2 instance is shared by every test class, and a stray category or an
	 * open bill shows up as a wrong count in tests that have nothing to do with
	 * auditing — which is a confusing way to learn you left something behind.
	 */
	@AfterEach
	void removeWhatThisTestCreated() {
		for (Long id : createdOrders) {
			orderItems.deleteAll(orderItems.findAll().stream()
					.filter(i -> i.getOrder() != null && id.equals(i.getOrder().getId()))
					.toList());
			orders.findById(id).ifPresent(o -> {
				if (o.getTable() != null) {
					tables.findById(o.getTable().getId()).ifPresent(t -> {
						t.setStatus(TableStatus.FREE);
						tables.save(t);
					});
				}
				orders.delete(o);
			});
		}
		createdOrders.clear();
		categories.deleteAll(categories.findAll().stream()
				.filter(c -> c.getName() != null && c.getName().startsWith(MARKER))
				.toList());
	}

	private long createCategory(String name) throws Exception {
		MvcResult res = mvc.perform(post("/api/categories")
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"%s\"}".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn();
		return json.readTree(res.getResponse().getContentAsString()).path("data").path("id").asLong();
	}

	private JsonNode auditFor(long id) throws Exception {
		MvcResult res = mvc.perform(get("/api/audit")
						.header("Authorization", token)
						.param("entity", "Category")
						.param("entityId", String.valueOf(id)))
				.andExpect(status().isOk())
				.andReturn();
		return json.readTree(res.getResponse().getContentAsString()).path("data").path("content");
	}

	/* ---- The question the log exists to answer ------------------------------ */

	@Test
	@DisplayName("an edit records the old value and the new one, not just that something changed")
	void updateRecordsBeforeAndAfter() throws Exception {
		String original = MARKER + "-" + UUID.randomUUID().toString().substring(0, 8);
		long id = createCategory(original);

		String renamed = original + "-renamed";
		mvc.perform(put("/api/categories/" + id)
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"%s\"}".formatted(renamed)))
				.andExpect(status().isOk());

		JsonNode entries = auditFor(id);
		JsonNode update = null;
		for (JsonNode e : entries) {
			if ("UPDATE".equals(e.path("action").asText())) { update = e; break; }
		}

		assertThat(update).as("an UPDATE entry for category %d", id).isNotNull();
		assertThat(json.readTree(update.path("before").asText()).path("name").asText())
				.isEqualTo(original);
		assertThat(json.readTree(update.path("after").asText()).path("name").asText())
				.isEqualTo(renamed);
		assertThat(update.path("userId").asText()).isEqualTo("admin");
	}

	@Test
	@DisplayName("a create is recorded with no before state")
	void createIsRecorded() throws Exception {
		long id = createCategory(MARKER + "-" + UUID.randomUUID().toString().substring(0, 8));

		JsonNode entries = auditFor(id);
		assertThat(entries).isNotEmpty();
		JsonNode created = entries.get(entries.size() - 1);
		assertThat(created.path("action").asText()).isEqualTo("CREATE");
		assertThat(created.path("before").isNull() || created.path("before").asText().isEmpty()).isTrue();
		assertThat(created.path("at").asText()).isNotBlank();
	}

	/* ---- What must never appear in it --------------------------------------- */

	/**
	 * A password hash is still a credential, and an audit log is read by more
	 * people than the table it came from.
	 */
	@Test
	@DisplayName("no audit entry anywhere contains a password field")
	void passwordsAreNeverLogged() throws Exception {
		mvc.perform(post("/api/auth/change-password")
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"currentPassword":"ChangeMe123!","newPassword":"ChangeMe123!"}"""))
				.andReturn();   // succeeds or is refused as unchanged; either is fine here

		assertThat(repository.findAll())
				.extracting(a -> (a.getBeforeJson() == null ? "" : a.getBeforeJson())
						+ (a.getAfterJson() == null ? "" : a.getAfterJson()))
				.allSatisfy(body -> assertThat(body)
						.doesNotContain("userPwd")
						.doesNotContain("$2a$")      // a BCrypt hash, whatever the field was called
						.doesNotContain("$2b$"));
	}

	/**
	 * lstLgnDtm and loginFailedCnt change on every single sign-in. Recording
	 * them would write an audit row per login and bury the role and lock changes
	 * that are the reason anyone opens this table.
	 */
	@Test
	@DisplayName("signing in does not write an audit entry")
	void loginIsNotAudited() throws Exception {
		long before = repository.count();

		mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"cashier","password":"ChangeMe123!"}"""))
				.andExpect(status().isOk());

		assertThat(repository.count()).isEqualTo(before);
	}

	/**
	 * Orders are high-volume and already have their own history. If they were
	 * audited, a day of trading would bury every master-data change.
	 */
	@Test
	@DisplayName("opening a bill writes no audit entry")
	void ordersAreNotAudited() throws Exception {
		var before = repository.findAll().stream().map(AuditLog::getId).toList();

		mvc.perform(post("/api/orders")
						.header("Authorization", token)
						.header("Idempotency-Key", UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"tableId":3,"guestCount":2}"""))
				.andExpect(status().isCreated())
				.andDo(res -> createdOrders.add(json.readTree(res.getResponse().getContentAsString())
						.path("data").path("id").asLong()));

		// Named, not counted: when this fails the useful question is which
		// entity leaked in, and a count cannot say.
		var added = repository.findAll().stream()
				.filter(a -> !before.contains(a.getId()))
				.map(a -> "%s %s %s -> %s".formatted(
						a.getAction(), a.getEntity(), a.getBeforeJson(), a.getAfterJson()))
				.toList();

		assertThat(added)
				.as("taking an order must not write to the audit log")
				.isEmpty();
	}

	/* ---- Access ------------------------------------------------------------- */

	@Test
	@DisplayName("only an admin may read it")
	void cashierCannotRead() throws Exception {
		MvcResult res = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"cashier","password":"ChangeMe123!"}"""))
				.andReturn();
		String cashier = "Bearer " + json.readTree(res.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();

		mvc.perform(get("/api/audit").header("Authorization", cashier))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("there is no way to write or remove an entry through the API")
	void theLogIsAppendOnly() throws Exception {
		mvc.perform(post("/api/audit").header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isMethodNotAllowed());
		mvc.perform(delete("/api/audit/1").header("Authorization", token))
				.andExpect(status().isNotFound());
	}
}
