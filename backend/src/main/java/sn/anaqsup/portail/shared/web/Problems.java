package sn.anaqsup.portail.shared.web;

import java.net.URI;

/** Catalogue des types d'erreur publiés dans le contrat ({@code components.schemas.Problem}). */
public final class Problems {

    private static final String BASE = "https://api.anaqsup.sn/problems/";

    private Problems() {
    }

    public static URI type(String slug) {
        return URI.create(BASE + slug);
    }
}
