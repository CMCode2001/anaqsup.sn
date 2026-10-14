package sn.anaqsup.portail.content.admin;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * HTML saisi dans le back-office → HTML sûr, servi tel quel par le portail.
 * Liste blanche stricte : paragraphes, titres de niveau 2-3, gras, italique, souligné, listes, citations, liens
 * (https, http, mailto ou adresses du site). Tout le reste (script, style, iframe, attributs on*…) est retiré.
 */
public final class HtmlSanitizer {

    private static final Safelist SAFELIST = new Safelist()
            .addTags("p", "br", "strong", "b", "em", "i", "u", "s", "a", "ul", "ol", "li", "h2", "h3", "blockquote")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "https", "http", "mailto")
            .addEnforcedAttribute("a", "rel", "noopener noreferrer")
            .preserveRelativeLinks(true);

    private HtmlSanitizer() {
    }

    public static String clean(String html) {
        return Jsoup.clean(html, "https://anaqsup.sn", SAFELIST, new Document.OutputSettings().prettyPrint(false)).strip();
    }
}
