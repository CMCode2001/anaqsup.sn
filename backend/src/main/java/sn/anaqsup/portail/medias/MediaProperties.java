package sn.anaqsup.portail.medias;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Dossier des images téléversées ({@code anaqsup.media.dir}). */
@ConfigurationProperties(prefix = "anaqsup.media")
record MediaProperties(Path dir) {
}
