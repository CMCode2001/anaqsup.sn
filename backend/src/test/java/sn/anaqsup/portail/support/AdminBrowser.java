package sn.anaqsup.portail.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import jakarta.servlet.http.Cookie;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

/**
 * Un onglet du back-office dans les tests, comme un vrai navigateur : cookie de session ANAQSUP_SESSION
 * (sessions enregistrées en base par Spring Session) et dernier jeton CSRF (cookie XSRF-TOKEN renvoyé dans
 * X-XSRF-TOKEN). Pas de raccourci {@code csrf()} de spring-security-test, qui remplacerait le dépôt de jetons
 * pour tout le contexte.
 */
public final class AdminBrowser {

    public static final String API = "/api/v1/admin";

    public static final String SESSION_COOKIE = "ANAQSUP_SESSION";

    private final MockMvcTester mvc;
    public @Nullable Cookie session;
    public @Nullable Cookie xsrf;

    public AdminBrowser(MockMvcTester mvc) {
        this.mvc = mvc;
        get("/auth/me");
    }

    /** Connexion ; avec {@code newPassword}, remplace aussi le mot de passe provisoire. */
    public static AdminBrowser login(MockMvcTester mvc, String email, String password, @Nullable String newPassword) {
        var browser = new AdminBrowser(mvc);
        assertThat(browser.post("/auth/login", """
                {"email": "%s", "password": "%s"}""".formatted(email, password))).hasStatusOk();
        if (newPassword != null) {
            assertThat(browser.post("/auth/password", """
                    {"currentPassword": "%s", "newPassword": "%s"}""".formatted(password, newPassword)))
                    .hasStatus2xxSuccessful();
        }
        return browser;
    }

    public MvcTestResult send(HttpMethod method, String path, @Nullable String json) {
        var request = withCookies(mvc.method(method).uri(API + path));
        if (json != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return remember(request.exchange());
    }

    public MvcTestResult upload(String path, MockMultipartFile file) {
        return remember(withCookies(mvc.post().uri(API + path).multipart().file(file)).exchange());
    }

    /** Cookies de l'onglet (session, jeton CSRF) et en-tête X-XSRF-TOKEN. */
    private <B extends AbstractMockHttpServletRequestBuilder<B>> B withCookies(B request) {
        var cookies = new ArrayList<Cookie>();
        if (session != null) {
            cookies.add(session);
        }
        if (xsrf != null) {
            cookies.add(xsrf);
            request.header("X-XSRF-TOKEN", xsrf.getValue());
        }
        if (!cookies.isEmpty()) {
            request.cookie(cookies.toArray(Cookie[]::new));
        }
        return request;
    }

    public MvcTestResult get(String path) {
        return send(HttpMethod.GET, path, null);
    }

    public MvcTestResult post(String path, @Nullable String json) {
        return send(HttpMethod.POST, path, json);
    }

    public MvcTestResult put(String path, String json) {
        return send(HttpMethod.PUT, path, json);
    }

    public MvcTestResult delete(String path) {
        return send(HttpMethod.DELETE, path, null);
    }

    /** Le dernier cookie de la réponse l'emporte (valeur vide ou durée nulle = cookie supprimé). */
    private MvcTestResult remember(MvcTestResult result) {
        last(result, "XSRF-TOKEN").ifPresent(c -> xsrf = deleted(c) ? null : c);
        last(result, SESSION_COOKIE).ifPresent(c -> session = deleted(c) ? null : c);
        return result;
    }

    private static Optional<Cookie> last(MvcTestResult result, String name) {
        return Arrays.stream(result.getResponse().getCookies()).filter(c -> c.getName().equals(name)).reduce((first, last) -> last);
    }

    private static boolean deleted(Cookie cookie) {
        return cookie.getValue().isEmpty() || cookie.getMaxAge() == 0;
    }
}
