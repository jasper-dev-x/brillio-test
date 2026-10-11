package com.listings.search.exception;

/** Requested page is beyond the last page of a non-empty result set. Mapped to HTTP 400. */
public class PageOutOfRangeException extends RuntimeException {

    private final int requestedPage;
    private final int totalPages;

    public PageOutOfRangeException(int requestedPage, int totalPages) {
        super("page " + requestedPage + " is out of range; there "
                + (totalPages == 1 ? "is 1 page" : "are " + totalPages + " pages"));
        this.requestedPage = requestedPage;
        this.totalPages = totalPages;
    }

    public int getRequestedPage() {
        return requestedPage;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
