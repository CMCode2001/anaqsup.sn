package sn.anaqsup.portail.content;

import java.util.List;

/**
 * Un contenu publié a changé. {@code tags} : étiquettes de cache du portail à invalider
 * (ex. {@code page:mot-du-secretaire-executif}, {@code partners}).
 */
public record ContentChanged(List<String> tags) {
}
