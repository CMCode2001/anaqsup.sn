package sn.anaqsup.portail.medias.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import sn.anaqsup.portail.medias.MediaKeys;
import sn.anaqsup.portail.medias.MediaStorage;
import sn.anaqsup.portail.shared.web.ResourceNotFoundException;

/**
 * Téléversement (back-office) et diffusion publique des images et des documents PDF.
 * Images : toute personne connectée ; documents : permission DOCUMENTS (module comptes).
 */
@RestController
class MediaControllers {

    record Uploaded(String key, String url, String contentType, int sizeBytes, @Nullable String originalName) {
        static Uploaded from(MediaStorage.Media media) {
            String key = media.id().toString();
            return new Uploaded(key, MediaKeys.publicUrl(key), media.contentType(), media.sizeBytes(), media.originalName());
        }
    }

    /** PDF téléversé et sa vignette, générée depuis la première page (absente si le rendu a échoué). */
    record UploadedDocument(Uploaded file, @Nullable Uploaded thumbnail) {
    }

    private final MediaStorage storage;

    MediaControllers(MediaStorage storage) {
        this.storage = storage;
    }

    @PostMapping(path = "/api/v1/admin/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Uploaded> upload(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        var media = storage.store(file.getBytes(), file.getOriginalFilename(), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(Uploaded.from(media));
    }

    @PostMapping(path = "/api/v1/admin/media/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<UploadedDocument> uploadDocument(@RequestParam("file") MultipartFile file, Authentication authentication)
            throws IOException {
        var document = storage.storeDocument(file.getBytes(), file.getOriginalFilename(), authentication.getName());
        var thumbnail = document.thumbnail();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UploadedDocument(Uploaded.from(document.file()), thumbnail == null ? null : Uploaded.from(thumbnail)));
    }

    /**
     * Une clé ne change jamais de contenu : cache navigateur et CDN d'un an.
     * Un PDF s'ouvre dans le lecteur du navigateur (« Consulter ») sous son nom d'origine, repris au téléchargement.
     */
    @GetMapping("/api/v1/public/media/{id}")
    ResponseEntity<Resource> serve(@PathVariable UUID id) {
        var file = storage.open(id).orElseThrow(() -> new ResourceNotFoundException("Fichier", id.toString()));
        var response = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.media().contentType()))
                .contentLength(file.media().sizeBytes())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .eTag(file.media().sha256());
        if (file.media().isPdf()) {
            response.header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.inline().filename(pdfName(file.media().originalName()), StandardCharsets.UTF_8).build().toString());
        }
        return response.body(new FileSystemResource(file.path()));
    }

    private static String pdfName(@Nullable String originalName) {
        String name = originalName == null ? "" : originalName.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").strip();
        if (name.isEmpty()) {
            return "document.pdf";
        }
        return name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf") ? name : name + ".pdf";
    }
}
