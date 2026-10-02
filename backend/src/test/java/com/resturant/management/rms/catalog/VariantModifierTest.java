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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Sizes and the things people ask for.
 *
 * <p>ARCHITECTURE §1.2 keeps these apart: a large coffee is a different thing
 * to sell, "no ice" is a property of one line. The tests that matter are the
 * ones about price — a size replaces the dish's price, a modifier moves it,
 * and neither figure may come from the request.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VariantModifierTest {

    /** Fried rice, 4.50, VAT 10%. */
    private static final long PRODUCT = 1;

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

    private JsonNode data(MvcResult res) throws Exception {
        return json.readTree(res.getResponse().getContentAsString()).path("data");
    }

    private long addVariant(String name, String price) throws Exception {
        MvcResult res = mvc.perform(post("/api/products/" + PRODUCT + "/variants")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"price\":%s}".formatted(name, price)))
                .andExpect(status().isCreated())
                .andReturn();
        return data(res).path("id").asLong();
    }

    /** A question with two answers, attached to the seeded dish. */
    private JsonNode addGroup(String body) throws Exception {
        MvcResult res = mvc.perform(post("/api/modifier-groups")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode group = data(res);

        mvc.perform(post("/api/products/" + PRODUCT + "/modifier-groups/" + group.path("id").asLong())
                        .header("Authorization", admin))
                .andExpect(status().isOk());
        return group;
    }

    /** Puts one line on a bill and returns the bill. */
    private MvcResult order(long tableId, String itemJson) throws Exception {
        MvcResult opened = mvc.perform(post("/api/orders")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tableId\":%d,\"guestCount\":1}".formatted(tableId)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = data(opened).path("id").asLong();

        return mvc.perform(put("/api/orders/" + id + "/items")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[%s]}".formatted(itemJson)))
                .andReturn();
    }

    /* ---- Variants ---------------------------------------------------------- */

    @Test
    @DisplayName("a size is priced in its own right, not as an adjustment")
    void variantReplacesThePrice() throws Exception {
        long large = addVariant("Large", "6.50");

        MvcResult res = order(1, """
                {"productId":%d,"qty":1,"variantId":%d}""".formatted(PRODUCT, large));
        assertThat(res.getResponse().getStatus()).isEqualTo(200);

        JsonNode line = data(res).path("items").get(0);
        assertThat(line.path("unitPrice").decimalValue())
                .as("the size's price, not the dish's 4.50")
                .isEqualByComparingTo("6.50");
        assertThat(line.path("variantName").asText()).isEqualTo("Large");
        assertThat(data(res).path("subtotal").decimalValue()).isEqualByComparingTo("6.50");
    }

    @Test
    @DisplayName("a size from another dish is refused")
    void variantMustBelongToTheProduct() throws Exception {
        long large = addVariant("Large", "6.50");

        MvcResult res = order(2, """
                {"productId":2,"qty":1,"variantId":%d}""".formatted(large));

        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat(json.readTree(res.getResponse().getContentAsString()).path("code").asText())
                .isEqualTo("error.order.variantNotOnProduct");
    }

    @Test
    @DisplayName("no size means the dish's own price, which is most of the menu")
    void noVariantIsOrdinary() throws Exception {
        MvcResult res = order(3, """
                {"productId":%d,"qty":2}""".formatted(PRODUCT));

        JsonNode line = data(res).path("items").get(0);
        assertThat(line.path("unitPrice").decimalValue()).isEqualByComparingTo("4.50");
        assertThat(line.path("variantId").isMissingNode()).isTrue();
    }

    /* ---- Modifiers ---------------------------------------------------------- */

    @Test
    @DisplayName("a modifier moves the line's price and is recorded on it")
    void modifierMovesThePrice() throws Exception {
        JsonNode group = addGroup("""
                {"name":"Extras","minSelect":0,"maxSelect":2,
                 "modifiers":[{"name":"Extra egg","priceDelta":1.00},
                              {"name":"No chilli","priceDelta":0}]}""");
        long extraEgg = group.path("modifiers").get(0).path("id").asLong();

        MvcResult res = order(5, """
                {"productId":%d,"qty":2,"modifierIds":[%d]}""".formatted(PRODUCT, extraEgg));

        JsonNode line = data(res).path("items").get(0);
        assertThat(line.path("unitPrice").decimalValue())
                .as("4.50 plus the extra egg")
                .isEqualByComparingTo("5.50");
        assertThat(line.path("lineTotal").decimalValue()).isEqualByComparingTo("11.00");

        JsonNode mods = line.path("modifiers");
        assertThat(mods).hasSize(1);
        assertThat(mods.get(0).path("name").asText()).isEqualTo("Extra egg");
        assertThat(mods.get(0).path("priceDelta").decimalValue()).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("a size and a modifier compose")
    void variantAndModifierTogether() throws Exception {
        long large = addVariant("Large", "6.50");
        JsonNode group = addGroup("""
                {"name":"Extras","minSelect":0,"maxSelect":1,
                 "modifiers":[{"name":"Extra egg","priceDelta":1.00}]}""");
        long extraEgg = group.path("modifiers").get(0).path("id").asLong();

        MvcResult res = order(6, """
                {"productId":%d,"qty":1,"variantId":%d,"modifierIds":[%d]}"""
                .formatted(PRODUCT, large, extraEgg));

        assertThat(data(res).path("items").get(0).path("unitPrice").decimalValue())
                .as("the size's price, then the modifier's delta")
                .isEqualByComparingTo("7.50");
    }

    /**
     * The price never travels in the request. ARCHITECTURE §5: the server
     * recalculates everything, and a figure that arrived from a client is a
     * figure somebody could have chosen.
     */
    @Test
    @DisplayName("a modifier the dish does not offer is refused, not priced")
    void unofferedModifierIsRefused() throws Exception {
        JsonNode group = addGroup("""
                {"name":"Extras","minSelect":0,"maxSelect":1,
                 "modifiers":[{"name":"Extra egg","priceDelta":1.00}]}""");
        long extraEgg = group.path("modifiers").get(0).path("id").asLong();

        // Product 2 never had the group attached.
        MvcResult res = order(7, """
                {"productId":2,"qty":1,"modifierIds":[%d]}""".formatted(extraEgg));

        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat(json.readTree(res.getResponse().getContentAsString()).path("code").asText())
                .isEqualTo("error.order.modifierNotOffered");
    }

    @Test
    @DisplayName("a question that must be answered is enforced on the line")
    void requiredGroupMustBeAnswered() throws Exception {
        addGroup("""
                {"name":"Spice level","minSelect":1,"maxSelect":1,
                 "modifiers":[{"name":"Mild","priceDelta":0},
                              {"name":"Hot","priceDelta":0}]}""");

        MvcResult res = order(8, """
                {"productId":%d,"qty":1}""".formatted(PRODUCT));

        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat(json.readTree(res.getResponse().getContentAsString()).path("code").asText())
                .isEqualTo("error.order.modifierCount");
    }

    @Test
    @DisplayName("more answers than the question allows is refused")
    void tooManyChosen() throws Exception {
        JsonNode group = addGroup("""
                {"name":"Spice level","minSelect":1,"maxSelect":1,
                 "modifiers":[{"name":"Mild","priceDelta":0},
                              {"name":"Hot","priceDelta":0}]}""");
        long mild = group.path("modifiers").get(0).path("id").asLong();
        long hot = group.path("modifiers").get(1).path("id").asLong();

        MvcResult res = order(9, """
                {"productId":%d,"qty":1,"modifierIds":[%d,%d]}""".formatted(PRODUCT, mild, hot));

        assertThat(res.getResponse().getStatus()).isEqualTo(400);
    }

    /* ---- The catalog screens ------------------------------------------------ */

    @Test
    @DisplayName("a question asking for more than it offers is refused")
    void maxCannotExceedTheOptions() throws Exception {
        mvc.perform(post("/api/modifier-groups")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Impossible","minSelect":0,"maxSelect":3,
                                 "modifiers":[{"name":"Only one","priceDelta":0}]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.modifier.maxAboveOptions"));
    }

    @Test
    @DisplayName("a cashier may read the menu's questions but not rewrite them")
    void cashiersRead() throws Exception {
        mvc.perform(get("/api/modifier-groups").header("Authorization", cashier))
                .andExpect(status().isOk());

        mvc.perform(post("/api/modifier-groups")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mine","minSelect":0,"maxSelect":1,
                                 "modifiers":[{"name":"Free","priceDelta":-100}]}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("sizes are listed under their product")
    void variantsAreListedUnderTheProduct() throws Exception {
        addVariant("Small", "3.50");
        addVariant("Large", "6.50");

        mvc.perform(get("/api/products/" + PRODUCT + "/variants").header("Authorization", cashier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
