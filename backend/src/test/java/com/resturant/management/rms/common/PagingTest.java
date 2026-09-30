package com.resturant.management.rms.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The one place that decides how big a page may be, and what to do with a
 * request that asks for something impossible.
 */
class PagingTest {

	@Test
	@DisplayName("passes a sensible request through untouched")
	void ordinaryRequest() {
		var pageable = Paging.of(2, 50);
		assertThat(pageable.getPageNumber()).isEqualTo(2);
		assertThat(pageable.getPageSize()).isEqualTo(50);
	}

	@Test
	@DisplayName("caps a page larger than the maximum")
	void capsSize() {
		assertThat(Paging.of(0, 10_000).getPageSize()).isEqualTo(Paging.MAX_SIZE);
	}

	@Test
	@DisplayName("serves the maximum exactly, so the client's All is not clipped short")
	void allowsTheMaximum() {
		assertThat(Paging.of(0, Paging.MAX_SIZE).getPageSize()).isEqualTo(Paging.MAX_SIZE);
	}

	/**
	 * {@code PageRequest.of} throws on a negative page, which reached the
	 * catch-all handler — so {@code ?page=-1} answered 500 and told the caller
	 * the server had broken when in fact the request had.
	 */
	@ParameterizedTest
	@ValueSource(ints = {-1, -100, Integer.MIN_VALUE})
	@DisplayName("a negative page is the first page, not a 500")
	void clampsNegativePage(int page) {
		assertThat(Paging.of(page, 20).getPageNumber()).isZero();
	}

	@ParameterizedTest
	@ValueSource(ints = {0, -1, Integer.MIN_VALUE})
	@DisplayName("a size of nothing gets the default, not an empty page")
	void clampsUnusableSize(int size) {
		// Returning zero rows would be obeying the letter of a request nobody
		// meant to make, and the caller would have no way to tell that from a
		// genuinely empty table.
		assertThat(Paging.of(0, size).getPageSize()).isEqualTo(Paging.DEFAULT_SIZE);
	}

	@Test
	@DisplayName("keeps the sort it was given")
	void keepsSort() {
		var sort = Sort.by("name").ascending();
		assertThat(Paging.of(0, 20, sort).getSort()).isEqualTo(sort);
	}
}
