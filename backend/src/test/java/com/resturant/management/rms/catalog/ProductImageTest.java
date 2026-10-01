package com.resturant.management.rms.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Uploading a product photograph.
 *
 * <p>Weighted towards what must not happen. The happy path is one assertion;
 * the rest is a reminder that an upload endpoint takes bytes from whoever can
 * reach it, and that the filename, the extension and the declared content type
 * are all chosen by them.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductImageTest {

	/** A seeded product; the test puts its photograph back when it is done. */
	private static final long PRODUCT = 1;

	@Autowired MockMvc mvc;
	@Autowired ObjectMapper json;

	private String admin;
	private String cashier;

	@BeforeEach
	void signIn() throws Exception {
		admin = "Bearer " + token("admin");
		cashier = "Bearer " + token("cashier");
	}

	private String token(String username) throws Exception {
		MvcResult res = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"%s\",\"password\":\"ChangeMe123!\"}".formatted(username)))
				.andReturn();
		return json.readTree(res.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();
	}

	/** A real PNG, made here rather than checked in as a fixture. */
	private static byte[] png(int width, int height) throws Exception {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		return out.toByteArray();
	}

	private MockMultipartFile file(String name, String type, byte[] bytes) {
		return new MockMultipartFile("file", name, type, bytes);
	}

	private String upload(MockMultipartFile f) throws Exception {
		MvcResult res = mvc.perform(multipart("/api/products/" + PRODUCT + "/image")
						.file(f).header("Authorization", admin))
				.andExpect(status().isOk())
				.andReturn();
		return json.readTree(res.getResponse().getContentAsString())
				.path("data").path("imageFile").asText();
	}

	/* ---- The happy path ---------------------------------------------------- */

	@Test
	@DisplayName("an uploaded image is stored, named by the server, and served back")
	void uploadAndFetch() throws Exception {
		String name = upload(file("lunch.png", MediaType.IMAGE_PNG_VALUE, png(40, 40)));

		assertThat(name)
				.as("the caller's filename is never the stored one")
				.isNotEqualTo("lunch.png")
				.endsWith(".jpg");

		mvc.perform(get("/api/products/images/" + name))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "image/jpeg"));

		mvc.perform(delete("/api/products/" + PRODUCT + "/image").header("Authorization", admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.imageFile").doesNotExist());
	}

	@Test
	@DisplayName("a large photograph is scaled down rather than stored as sent")
	void scalesDown() throws Exception {
		String name = upload(file("big.png", MediaType.IMAGE_PNG_VALUE, png(2000, 1500)));

		MvcResult res = mvc.perform(get("/api/products/images/" + name)).andReturn();
		BufferedImage stored = ImageIO.read(
				new java.io.ByteArrayInputStream(res.getResponse().getContentAsByteArray()));

		// A till downloading 2000px tiles for a 128px grid is bytes thrown away.
		assertThat(Math.max(stored.getWidth(), stored.getHeight()))
				.as("scaled to fit the configured long edge")
				.isLessThanOrEqualTo(600);
		assertThat(stored.getWidth()).isGreaterThan(stored.getHeight());

		mvc.perform(delete("/api/products/" + PRODUCT + "/image").header("Authorization", admin));
	}

	/* ---- What must not get through ----------------------------------------- */

	@Test
	@DisplayName("a file that is not an image is refused however it is labelled")
	void refusesNonImages() throws Exception {
		// Named and declared as a JPEG. Neither is evidence of anything.
		mvc.perform(multipart("/api/products/" + PRODUCT + "/image")
						.file(file("lunch.jpg", MediaType.IMAGE_JPEG_VALUE,
								"#!/bin/sh\nrm -rf /".getBytes()))
						.header("Authorization", admin))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("error.upload.notAnImage"));
	}

	@Test
	@DisplayName("an empty upload is refused")
	void refusesEmpty() throws Exception {
		mvc.perform(multipart("/api/products/" + PRODUCT + "/image")
						.file(file("nothing.png", MediaType.IMAGE_PNG_VALUE, new byte[0]))
						.header("Authorization", admin))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("error.upload.empty"));
	}

	/**
	 * The stored name comes back as a path variable, so it is caller input on
	 * the way out as well as on the way in.
	 */
	@Test
	@DisplayName("a name that climbs out of the uploads directory serves nothing")
	void refusesTraversal() throws Exception {
		for (String attempt : new String[]{"..%2f..%2fapplication.yml", "..%5c..%5cbuild.gradle"}) {
			mvc.perform(get("/api/products/images/" + attempt))
					.andExpect(status().is4xxClientError());
		}
	}

	@Test
	@DisplayName("a name that matches no file is a 404, not an error page")
	void missingImageIsNotFound() throws Exception {
		// The screens fall back to the product's icon, so this must stay quiet.
		mvc.perform(get("/api/products/images/0ca1f4de-0000-0000-0000-000000000000.jpg"))
				.andExpect(status().isNotFound());
	}

	/* ---- Who may ------------------------------------------------------------ */

	@Test
	@DisplayName("a cashier cannot upload or remove a photograph")
	void onlyAdminsMayChangeImages() throws Exception {
		mvc.perform(multipart("/api/products/" + PRODUCT + "/image")
						.file(file("x.png", MediaType.IMAGE_PNG_VALUE, png(10, 10)))
						.header("Authorization", cashier))
				.andExpect(status().isForbidden());

		mvc.perform(delete("/api/products/" + PRODUCT + "/image").header("Authorization", cashier))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("anyone may fetch one, because an <img> carries no token")
	void fetchingNeedsNoToken() throws Exception {
		String name = upload(file("open.png", MediaType.IMAGE_PNG_VALUE, png(20, 20)));

		mvc.perform(get("/api/products/images/" + name))
				.andExpect(status().isOk());

		mvc.perform(delete("/api/products/" + PRODUCT + "/image").header("Authorization", admin));
	}
}
