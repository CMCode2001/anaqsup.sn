package sn.anaqsup.portail.medias;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vignette d'un document : la première page du PDF, en JPEG de {@value #WIDTH} pixels de large
 * (assez net pour une carte affichée sur un écran haute définition).
 */
final class PdfThumbnails {

    /** Résultat de l'ouverture d'un PDF : lisible (avec ou sans vignette), protégé ou illisible. */
    sealed interface Result {
        record Readable(Optional<byte[]> thumbnail) implements Result {
        }

        record PasswordProtected() implements Result {
        }

        record Unreadable() implements Result {
        }
    }

    private static final Logger log = LoggerFactory.getLogger(PdfThumbnails.class);

    private static final int WIDTH = 800;
    /** Garde-fou pour les pages très allongées (affiches, dépliants). */
    private static final int MAX_HEIGHT = 2400;
    /** Un PDF piégé ou démesuré ne doit pas bloquer le téléversement. */
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private static final ExecutorService RENDERING = Executors.newVirtualThreadPerTaskExecutor();

    private PdfThumbnails() {
    }

    static Result open(byte[] pdf) {
        // Fichiers temporaires plutôt que mémoire : un gros PDF ne sature pas le tas de la JVM.
        try (PDDocument document = Loader.loadPDF(pdf, "", null, null, IOUtils.createTempFileOnlyStreamCache())) {
            return new Result.Readable(firstPage(document));
        } catch (InvalidPasswordException e) {
            return new Result.PasswordProtected();
        } catch (IOException e) {
            log.info("PDF refusé, illisible : {}", e.getMessage());
            return new Result.Unreadable();
        }
    }

    private static Optional<byte[]> firstPage(PDDocument document) {
        if (document.getNumberOfPages() == 0) {
            return Optional.empty();
        }
        Future<byte[]> rendering = RENDERING.submit(() -> render(document));
        try {
            return Optional.of(rendering.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS));
        } catch (TimeoutException e) {
            rendering.cancel(true);
            log.warn("Vignette abandonnée : rendu de la première page trop long");
            return Optional.empty();
        } catch (ExecutionException e) {
            log.warn("Vignette impossible : {}", e.getCause().toString());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private static byte[] render(PDDocument document) throws IOException {
        PDPage page = document.getPage(0);
        PDRectangle box = page.getCropBox();
        boolean turned = page.getRotation() % 180 != 0;
        float width = turned ? box.getHeight() : box.getWidth();
        float height = turned ? box.getWidth() : box.getHeight();
        float scale = Math.min(WIDTH / width, MAX_HEIGHT / height);

        PDFRenderer renderer = new PDFRenderer(document);
        renderer.setSubsamplingAllowed(true);
        BufferedImage image = renderer.renderImage(0, scale, ImageType.RGB);
        return jpeg(image);
    }

    private static byte[] jpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.85f); // texte net sans alourdir la page
        var bytes = new ByteArrayOutputStream();
        try (var output = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }
}
