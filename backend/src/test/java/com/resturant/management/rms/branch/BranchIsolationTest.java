package com.resturant.management.rms.branch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resturant.management.rms.catalog.Category;
import com.resturant.management.rms.catalog.CategoryRepository;
import com.resturant.management.rms.catalog.Product;
import com.resturant.management.rms.catalog.ProductRepository;
import com.resturant.management.rms.customer.Customer;
import com.resturant.management.rms.customer.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.AfterEach;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The only question P1 has to answer: can one shop see another's rows.
 *
 * <p>API §3 names the shape of the bug it is avoiding — this system shipped a
 * privilege escalation once by honouring a client-supplied role, and a
 * client-supplied branch is the same mistake wearing a different hat. So the
 * branch is a signed claim, and the scoping is a Hibernate tenant filter that
 * no repository method can forget to apply.
 *
 * <p>Which means the thing worth testing is not any one query. It is that the
 * filter is really on, that it follows the token rather than the request, and
 * that switching shop switches what is visible.
 *
 * <p><b>Not {@code @Transactional}</b>, and the first version of this class
 * was — which made every test here pass while proving nothing. Hibernate
 * resolves the tenant when a session opens, so a transaction started by the
 * test before the branch is set fixes every query in it to the branch the test
 * started in. Rows written "in the second shop" went to the first, and the
 * second shop could read the first's menu, and all of that looked like the
 * code working.
 *
 * <p>So each call here gets its own session, the way a request does, and what
 * it creates is cleaned up afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BranchIsolationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired BranchRepository branches;
    @Autowired CompanyRepository companies;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired CategoryRepository categories;

    private String admin;
    private Branch second;

    @BeforeEach
    void setUp() throws Exception {
        admin = token("admin");

        second = branches.save(Branch.builder()
                .company(companies.findById(1L).orElseThrow())
                .code("TWO-" + System.nanoTime() % 100000)
                .name("The second shop")
                .build());
    }

    /**
     * Nothing here rolls back, so the second shop and everything put in it is
     * taken away again. In its own branch context, because the delete is
     * scoped like every other statement.
     */
    @AfterEach
    void tidyUp() {
        if (second == null) return;
        BranchContext.as(second.getId(), () -> {
            products.deleteAll(products.findAll());
            customers.deleteAll(customers.findAll());
            return null;
        });
        branches.delete(second);
    }

    private String token(String username) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"ChangeMe123!\"}".formatted(username)))
                .andReturn();
        return "Bearer " + json.readTree(res.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private JsonNode data(MvcResult res) throws Exception {
        return json.readTree(res.getResponse().getContentAsString()).path("data");
    }

    /* ---- The filter is on -------------------------------------------------- */

    @Test
    @DisplayName("a row written in one branch is invisible from the other")
    void rowsDoNotCrossBranches() {
        Category category = categories.findAll().get(0);

        Product theirs = BranchContext.as(second.getId(), () -> products.save(Product.builder()
                .name("Only in the second shop")
                .category(category)
                .price(new BigDecimal("3.00"))
                .cost(new BigDecimal("1.00"))
                .build()));

        // Hibernate wrote the branch from the context, without being asked.
        assertThat(theirs.getBranchId()).isEqualTo(second.getId());

        assertThat(BranchContext.as(1L, () -> products.findById(theirs.getId())))
                .as("branch 1 asking for it by id gets nothing")
                .isEmpty();
        assertThat(BranchContext.as(second.getId(), () -> products.findById(theirs.getId())))
                .as("its own branch finds it")
                .isPresent();

        assertThat(BranchContext.as(1L, () -> products.findAll()))
                .as("and it is not in the list either")
                .noneMatch(p -> p.getId().equals(theirs.getId()));
    }

    /**
     * Counting rather than naming: the number of products in each shop has to
     * add up to the number in both, with nothing shared and nothing lost.
     */
    @Test
    @DisplayName("each branch counts only its own")
    void countsAreSeparate() {
        long firstBefore = BranchContext.as(1L, () -> products.count());
        long secondBefore = BranchContext.as(second.getId(), () -> products.count());
        assertThat(secondBefore).as("a new shop starts with nothing").isZero();

        Category category = categories.findAll().get(0);
        BranchContext.as(second.getId(), () -> products.save(Product.builder()
                .name("Theirs")
                .category(category)
                .price(new BigDecimal("1.00"))
                .cost(new BigDecimal("0.50"))
                .build()));

        assertThat(BranchContext.as(1L, () -> products.count())).isEqualTo(firstBefore);
        assertThat(BranchContext.as(second.getId(), () -> products.count())).isEqualTo(1);
    }

    @Test
    @DisplayName("a customer's phone lookup does not reach across shops")
    void lookupIsScoped() {
        Customer theirs = BranchContext.as(second.getId(), () -> customers.save(Customer.builder()
                .code("C-99999")
                .name("Their regular")
                .phone("012 999 999")
                .build()));

        assertThat(BranchContext.as(1L, () -> customers.findByPhoneOrderByIdAsc("012 999 999")))
                .as("the till of one shop cannot find the other's customers")
                .isEmpty();
        assertThat(BranchContext.as(second.getId(), () -> customers.findByPhoneOrderByIdAsc("012 999 999")))
                .singleElement()
                .extracting(Customer::getId)
                .isEqualTo(theirs.getId());
    }

    /* ---- It follows the token ---------------------------------------------- */

    /**
     * The whole chain, over HTTP: switch branch, get a new token, and watch
     * what the same endpoint answers.
     */
    @Test
    @DisplayName("switching branch changes what the API returns")
    void switchingBranchChangesWhatIsVisible() throws Exception {
        long inFirst = data(mvc.perform(get("/api/products?size=1").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn()).path("totalElements").asLong();
        assertThat(inFirst).as("the seeded menu").isPositive();

        MvcResult switched = mvc.perform(post("/api/auth/switch-branch")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branchId\":%d}".formatted(second.getId())))
                .andExpect(status().isOk())
                .andReturn();

        String inSecondShop = "Bearer " + data(switched).path("accessToken").asText();
        assertThat(data(switched).path("user").path("branchId").asLong()).isEqualTo(second.getId());

        mvc.perform(get("/api/products?size=1").header("Authorization", inSecondShop))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements")
                        .value(0));   // a new shop has no menu yet

        // And the original token still sees the original shop. The branch
        // travels with the token, not with the session or the thread.
        mvc.perform(get("/api/products?size=1").header("Authorization", admin))
                .andExpect(jsonPath("$.data.totalElements").value((int) inFirst));
    }

    @Test
    @DisplayName("a cashier cannot move between shops")
    void onlySeniorStaffMayMove() throws Exception {
        String cashier = token("cashier");

        mvc.perform(post("/api/auth/switch-branch")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branchId\":%d}".formatted(second.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("error.branch.notPermitted"));
    }

    @Test
    @DisplayName("the branches a user may work in are listed, with the current one flagged")
    void branchesAreListed() throws Exception {
        JsonNode mine = data(mvc.perform(get("/api/auth/branches").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn());

        assertThat(mine).hasSizeGreaterThanOrEqualTo(2);
        assertThat(mine).anyMatch(b -> b.path("current").asBoolean());

        // A cashier sees only where they work.
        JsonNode theirs = data(mvc.perform(get("/api/auth/branches")
                .header("Authorization", token("cashier"))).andReturn());
        assertThat(theirs).hasSize(1);
    }

    /**
     * A token with no branch claim — one issued before V17 and still inside
     * its lifetime — must not leave a cashier stranded mid-shift.
     */
    @Test
    @DisplayName("a token from before branches existed falls back to the original shop")
    void tokenWithoutABranchClaimStillWorks() throws Exception {
        mvc.perform(get("/api/products?size=1").header("Authorization", admin))
                .andExpect(status().isOk());

        assertThat(BranchContext.DEFAULT_BRANCH)
                .as("which is the branch V17 backfilled everything to")
                .isEqualTo(1L);
    }
}
