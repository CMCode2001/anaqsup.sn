package sn.anaqsup.portail.shared.web;

import java.util.List;

/** Enveloppe de pagination du contrat ({@code components.schemas.PageMeta}). */
public record PageResponse<T>(List<T> content, PageMeta page) {

    public record PageMeta(int page, int size, long totalElements, int totalPages) {
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(content, new PageMeta(page, size, totalElements, totalPages));
    }
}
