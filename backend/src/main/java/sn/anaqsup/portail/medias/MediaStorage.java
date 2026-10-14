package sn.anaqsup.portail.medias;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.ErrorResponseException;

import sn.anaqsup.portail.shared.web.Problems;

/**
 * Stockage des images et des documents PDF : fichier {@code <dir>/<uuid>} sur le volume sauvegardé,
 * métadonnées en base. Le format est reconnu d'après les premiers octets du fichier : un fichier renommé
 * en .png ou en .pdf ne passe pas. Un PDF reçoit sa vignette (première page) au téléversement.
 */
@Service
public class MediaStorage {

    public record Media(UUID id, String contentType, int sizeBytes, String sha256, @Nullable String originalName,
                        @Nullable UUID derivedFrom, Instant createdAt) {

        public boolean isImage() {
            return contentType.startsWith("image/");
        }

        public boolean isPdf() {
            return PDF.equals(contentType);
        }
    }

    /** Un PDF téléversé et sa vignette (absente si la première page n'a pas pu être rendue). */
    public record Document(Media file, @Nullable Media thumbnail) {
    }

    public record StoredFile(Media media, Path path) {
    }

    private static final String PDF = "application/pdf";
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_PDF_BYTES = 30 * 1024 * 1024;

    private final JdbcClient jdbc;
    private final TransactionTemplate transaction;
    private final Path dir;

    MediaStorage(JdbcClient jdbc, PlatformTransactionManager transactions, MediaProperties properties) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactions);
        this.dir = properties.dir();
    }

    /** Image d'un contenu (photo, logo, couverture) : PNG, JPEG ou WebP. */
    @Transactional
    public Media store(byte[] content, @Nullable String originalName, String uploadedBy) {
        if (content.length == 0 || content.length > MAX_IMAGE_BYTES) {
            throw rejected("IMAGE_TOO_LARGE", "L'image doit peser au plus 5 Mo.");
        }
        String contentType = detect(content);
        if (contentType == null || !contentType.startsWith("image/")) {
            throw rejected("UNSUPPORTED_IMAGE", "Format accepté : PNG, JPEG ou WebP.");
        }
        return insert(content, contentType, originalName, uploadedBy, null);
    }

    /**
     * Document PDF des ressources documentaires, avec sa vignette (première page). Le rendu de la vignette
     * se fait avant d'ouvrir la transaction : aucune connexion à la base n'est retenue pendant ce calcul.
     */
    public Document storeDocument(byte[] content, @Nullable String originalName, String uploadedBy) {
        if (content.length == 0 || content.length > MAX_PDF_BYTES) {
            throw rejected("DOCUMENT_TOO_LARGE", "Le document doit peser au plus 30 Mo.");
        }
        if (!PDF.equals(detect(content))) {
            throw rejected("UNSUPPORTED_DOCUMENT", "Format accepté : PDF.");
        }
        byte[] thumbnail = switch (PdfThumbnails.open(content)) {
            case PdfThumbnails.Result.Readable readable -> readable.thumbnail().orElse(null);
            case PdfThumbnails.Result.PasswordProtected protectedPdf -> throw rejected("PROTECTED_DOCUMENT",
                    "Ce PDF est protégé par un mot de passe : le public ne pourrait pas l'ouvrir.");
            case PdfThumbnails.Result.Unreadable unreadable -> throw rejected("UNREADABLE_DOCUMENT",
                    "Ce fichier PDF est illisible ou endommagé.");
        };
        return Objects.requireNonNull(transaction.execute(status -> {
            Media file = insert(content, PDF, originalName, uploadedBy, null);
            Media cover = thumbnail == null ? null
                    : insert(thumbnail, "image/jpeg", thumbnailName(originalName), uploadedBy, file.id());
            return new Document(file, cover);
        }));
    }

    public Optional<Media> find(UUID id) {
        return jdbc.sql("""
                        SELECT id, content_type, size_bytes, sha256, original_name, derived_from, created_at
                        FROM media WHERE id = :id""")
                .param("id", id)
                .query((rs, n) -> new Media(rs.getObject("id", UUID.class), rs.getString("content_type"),
                        rs.getInt("size_bytes"), rs.getString("sha256"), rs.getString("original_name"),
                        rs.getObject("derived_from", UUID.class), rs.getTimestamp("created_at").toInstant()))
                .optional();
    }

    private Media insert(byte[] content, String contentType, @Nullable String originalName, String uploadedBy,
                         @Nullable UUID derivedFrom) {
        UUID id = jdbc.sql("""
                        INSERT INTO media (content_type, size_bytes, sha256, original_name, uploaded_by, derived_from)
                        VALUES (:type, :size, :sha, :name, :by, :from) RETURNING id""")
                .param("type", contentType).param("size", content.length).param("sha", sha256(content))
                .param("name", originalName == null || originalName.isBlank() ? null : originalName.strip())
                .param("by", uploadedBy).param("from", derivedFrom)
                .query(UUID.class).single();
        write(id, content);
        return find(id).orElseThrow();
    }

    private static String thumbnailName(@Nullable String pdfName) {
        String base = pdfName == null || pdfName.isBlank() ? "document" : pdfName.strip().replaceFirst("(?i)\\.pdf$", "");
        return base + " (vignette).jpg";
    }

    public Optional<StoredFile> open(UUID id) {
        return find(id).map(media -> new StoredFile(media, dir.resolve(id.toString()))).filter(f -> Files.isReadable(f.path()));
    }

    private void write(UUID id, byte[] content) {
        try {
            Files.createDirectories(dir);
            // Écriture atomique : jamais de fichier à moitié écrit servi au public.
            Path temporary = Files.createTempFile(dir, id.toString(), ".part");
            Files.write(temporary, content);
            Files.move(temporary, dir.resolve(id.toString()), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Écriture du fichier impossible", e);
        }
    }

    /** Signature des formats acceptés (« magic numbers »). */
    static @Nullable String detect(byte[] b) {
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        if (b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') {
            return PDF;
        }
        return null;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ErrorResponseException rejected(String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(Problems.type("media/" + code.toLowerCase().replace('_', '-')));
        problem.setTitle(code.contains("DOCUMENT") ? "Document refusé" : "Image refusée");
        problem.setProperty("code", code);
        return new ErrorResponseException(HttpStatus.BAD_REQUEST, problem, null);
    }
}
