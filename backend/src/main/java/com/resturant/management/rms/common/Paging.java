package com.resturant.management.rms.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * One place that decides how big a page may be.
 *
 * <p>The cap used to be written out at each call site, and had already drifted:
 * most endpoints allowed 100 and two allowed 200, for no reason anyone recorded.
 *
 * <p>It also clamps the page number. {@code PageRequest.of} throws on a negative
 * one, which reached the catch-all handler and answered {@code ?page=-1} with a
 * 500 — telling the caller the server broke when in fact the request did.
 */
public final class Paging {

    /**
     * The largest page the API will serve.
     *
     * <p>Chosen so the client's "All" really does show everything for the lists
     * people want whole — products, categories, staff, suppliers, tables — while
     * still refusing to stream an entire order history into a browser. When a
     * list is longer than this the client keeps paging and says so, rather than
     * quietly showing a slice labelled "All".
     */
    public static final int MAX_SIZE = 500;

    public static final int DEFAULT_SIZE = 20;

    private Paging() {
    }

    public static Pageable of(int page, int size) {
        return PageRequest.of(clampPage(page), clampSize(size));
    }

    public static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(clampPage(page), clampSize(size), sort);
    }

    private static int clampPage(int page) {
        return Math.max(0, page);
    }

    private static int clampSize(int size) {
        // A size of zero or less is a caller mistake, not a request for nothing:
        // answer with the default rather than an empty page they cannot explain.
        return size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }
}
