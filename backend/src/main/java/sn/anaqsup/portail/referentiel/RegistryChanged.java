package sn.anaqsup.portail.referentiel;

import java.util.List;

/**
 * Un établissement ou une formation a changé dans le back-office. {@code tags} : étiquettes de cache du
 * portail à invalider (ex. {@code establishments}, {@code establishment:ucad}, {@code programs}).
 */
public record RegistryChanged(List<String> tags) {
}
