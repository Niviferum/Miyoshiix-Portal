package com.miyoshix.portal.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.miyoshix.portal.PostgresTestSupport;
import com.miyoshix.portal.user.AppRole;
import com.miyoshix.portal.user.AppUser;
import com.miyoshix.portal.user.AppUserRepository;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Parcours complet de connexion Discord à travers le vrai serveur HTTP, la vraie chaîne
 * de filtres Spring Security et un vrai PostgreSQL. Discord est remplacé par
 * {@link FakeDiscordServer}.
 *
 * <p>Le fichier {@code static/index.html} utilisé par les tests de coquille est un
 * placeholder, remplacé plus tard par le build Angular.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PostgresTestSupport.class)
@ExtendWith(OutputCaptureExtension.class)
class DiscordLoginE2ETest {

    private static final String ALLOWED_ID = "123456789012345678";
    private static final String UNKNOWN_ID = "987654321098765432";
    private static final String AVATAR_HASH = "0123456789abcdef0123456789abcdef";
    private static final String SESSION_COOKIE = "MIYOSHIX_SESSION";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";

    private static final FakeDiscordServer discord = startFakeDiscord();

    @DynamicPropertySource
    static void discordEndpoints(DynamicPropertyRegistry registry) {
        String prefix = "spring.security.oauth2.client.provider.discord.";
        registry.add(prefix + "authorization-uri", () -> discord.baseUrl() + "/oauth2/authorize");
        registry.add(prefix + "token-uri", () -> discord.baseUrl() + "/api/oauth2/token");
        registry.add(prefix + "user-info-uri", () -> discord.baseUrl() + "/api/v10/users/@me");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private AppUserRepository users;

    private TestBrowser browser;

    @BeforeEach
    void reset() {
        users.deleteAll();
        discord.reset();
        browser = new TestBrowser("http://localhost:" + port);
    }

    @AfterAll
    static void stopFakeDiscord() {
        discord.stop();
    }

    // --- Connexion acceptée -------------------------------------------------------

    @Test
    void connecteUnCompteDeLaListeBlancheEtOuvreUneSession() throws Exception {
        users.save(new AppUser(ALLOWED_ID, "Ancien pseudo", AppRole.USER));

        HttpResponse<String> callback = loginWithDiscord(discordUser(ALLOWED_ID));

        assertThat(callback.statusCode()).isEqualTo(302);
        assertThat(locationPath(callback)).isEqualTo("/");
        assertThat(TestBrowser.setCookieHeader(callback, SESSION_COOKIE))
                .hasValueSatisfying(header -> assertThat(header)
                        .contains("HttpOnly")
                        .containsIgnoringCase("SameSite=Lax"));

        HttpResponse<String> me = browser.get("/api/me");
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body())
                .contains("\"id\":\"" + ALLOWED_ID + "\"")
                .contains("\"displayName\":\"Pseudo Affiché\"")
                .contains("\"role\":\"USER\"")
                .contains("https://cdn.discordapp.com/avatars/" + ALLOWED_ID + "/" + AVATAR_HASH + ".png");
    }

    @Test
    void enregistreLesInformationsDeConnexionEnBase() throws Exception {
        users.save(new AppUser(ALLOWED_ID, "Ancien pseudo", AppRole.USER));

        loginWithDiscord(discordUser(ALLOWED_ID));

        AppUser saved = users.findById(ALLOWED_ID).orElseThrow();
        assertThat(saved.getDisplayName()).isEqualTo("Pseudo Affiché");
        assertThat(saved.getAvatarHash()).isEqualTo(AVATAR_HASH);
        assertThat(saved.getLastLoginAt()).isNotNull();
    }

    @Test
    void echangeLeCodeAvecPkceEtAuthentificationDuClient() throws Exception {
        users.save(new AppUser(ALLOWED_ID, "pseudo", AppRole.USER));

        loginWithDiscord(discordUser(ALLOWED_ID));

        FakeDiscordServer.ReceivedRequest tokenRequest = discord.requests().stream()
                .filter(request -> request.path().equals("/api/oauth2/token"))
                .findFirst()
                .orElseThrow();
        assertThat(tokenRequest.method()).isEqualTo("POST");
        assertThat(tokenRequest.authorization()).startsWith("Basic ");
        assertThat(tokenRequest.body())
                .contains("grant_type=authorization_code")
                .contains("code=code-de-test")
                .contains("code_verifier=");
    }

    @Test
    void appliqueLeRoleDeLaBaseAuxModulesVisibles() throws Exception {
        users.save(new AppUser(ALLOWED_ID, "pseudo", AppRole.ADMIN));

        loginWithDiscord(discordUser(ALLOWED_ID));

        HttpResponse<String> modules = browser.get("/api/modules");
        assertThat(modules.statusCode()).isEqualTo(200);
        assertThat(modules.body())
                .contains("\"key\":\"exemple\"")
                .contains("\"key\":\"admin-only\"")
                .doesNotContain("desactive");
    }

    // --- Connexion refusée --------------------------------------------------------

    @Test
    void refuseUnCompteHorsListeBlancheSansOuvrirDeSession() throws Exception {
        HttpResponse<String> callback = loginWithDiscord(discordUser(UNKNOWN_ID));

        assertThat(callback.statusCode()).isEqualTo(302);
        assertThat(locationPath(callback)).isEqualTo("/login");
        assertThat(URI.create(location(callback)).getQuery()).isEqualTo("error=not_allowed");
        assertThat(browser.get("/api/me").statusCode()).isEqualTo(401);
        assertThat(users.count()).isZero();
    }

    @Test
    void refuseUnCompteDesactive() throws Exception {
        AppUser disabled = new AppUser(ALLOWED_ID, "pseudo", AppRole.ADMIN);
        disabled.setEnabled(false);
        users.save(disabled);

        HttpResponse<String> callback = loginWithDiscord(discordUser(ALLOWED_ID));

        assertThat(URI.create(location(callback)).getQuery()).isEqualTo("error=not_allowed");
        assertThat(browser.get("/api/me").statusCode()).isEqualTo(401);
    }

    @Test
    void rejetteUnStateFalsifieSansContacterDiscord() throws Exception {
        users.save(new AppUser(ALLOWED_ID, "pseudo", AppRole.USER));
        discord.respondWithUser(discordUser(ALLOWED_ID));
        Map<String, String> authorize = startAuthorization();

        HttpResponse<String> callback = browser.get(
                authorize.get("redirect_uri") + "?code=code-de-test&state=state-falsifie");

        assertThat(callback.statusCode()).isEqualTo(302);
        assertThat(URI.create(location(callback)).getQuery()).isEqualTo("error=login_failed");
        assertThat(discord.requests()).isEmpty();
        assertThat(browser.get("/api/me").statusCode()).isEqualTo(401);
    }

    @Test
    void construitLAdresseDeRetourDiscordAvecLesEnTetesDuProxyHttps() throws Exception {
        HttpResponse<String> response = browser.get("/oauth2/authorization/discord",
                Map.of("X-Forwarded-Proto", "https", "X-Forwarded-Host", "miyoshiix.com"));

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(queryParams(URI.create(location(response))))
                .containsEntry("redirect_uri", "https://miyoshiix.com/login/oauth2/code/discord");
    }

    // --- Session ------------------------------------------------------------------

    @Test
    void emetUnNouvelIdentifiantDeSessionALaConnexion() throws Exception {
        users.save(new AppUser(ALLOWED_ID, "pseudo", AppRole.USER));
        discord.respondWithUser(discordUser(ALLOWED_ID));

        Map<String, String> authorize = startAuthorization();
        String sessionBeforeLogin = browser.cookie(SESSION_COOKIE).orElseThrow();
        completeAuthorization(authorize);
        String sessionAfterLogin = browser.cookie(SESSION_COOKIE).orElseThrow();

        assertThat(sessionAfterLogin).isNotEqualTo(sessionBeforeLogin);

        TestBrowser attacker = new TestBrowser("http://localhost:" + port);
        attacker.setCookie(SESSION_COOKIE, sessionBeforeLogin);
        assertThat(attacker.get("/api/me").statusCode()).isEqualTo(401);
    }

    @Test
    void deconnecteAvecJetonCsrfEtInvalideLaSession(CapturedOutput output) throws Exception {
        users.save(new AppUser(ALLOWED_ID, "pseudo", AppRole.USER));
        loginWithDiscord(discordUser(ALLOWED_ID));
        String session = browser.cookie(SESSION_COOKIE).orElseThrow();

        // Le jeton CSRF est renouvelé après la connexion : une requête le fait réémettre.
        assertThat(browser.get("/api/me").statusCode()).isEqualTo(200);
        String csrfToken = browser.cookie(CSRF_COOKIE).orElseThrow();

        assertThat(browser.post("/api/auth/logout", Map.of()).statusCode()).isEqualTo(403);
        assertThat(output).contains("Accès refusé (jeton CSRF absent ou invalide) : POST /api/auth/logout par "
                + ALLOWED_ID);

        assertThat(browser.post("/api/auth/logout", Map.of("X-XSRF-TOKEN", csrfToken)).statusCode())
                .isEqualTo(204);
        assertThat(output).contains("Déconnexion de Pseudo Affiché (" + ALLOWED_ID + ").");

        TestBrowser replay = new TestBrowser("http://localhost:" + port);
        replay.setCookie(SESSION_COOKIE, session);
        assertThat(replay.get("/api/me").statusCode()).isEqualTo(401);
    }

    @Test
    void refuseUnJetonCsrfQuiNeCorrespondPasAuCookie() throws Exception {
        users.save(new AppUser(ALLOWED_ID, "pseudo", AppRole.USER));
        loginWithDiscord(discordUser(ALLOWED_ID));
        browser.get("/api/me");

        HttpResponse<String> logout = browser.post("/api/auth/logout", Map.of("X-XSRF-TOKEN", "jeton-invente"));

        assertThat(logout.statusCode()).isEqualTo(403);
        assertThat(browser.get("/api/me").statusCode()).isEqualTo(200);
    }

    // --- Coquille SPA et routes réservées -------------------------------------------

    @Test
    void sertLaCoquillePourLesRoutesAngular() throws Exception {
        HttpResponse<String> route = browser.get("/modules/exemple");

        assertThat(route.statusCode()).isEqualTo(200);
        assertThat(route.headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).startsWith("text/html"));
    }

    @Test
    void sertLaPageDeConnexionAngularEtNonCelleDeSpringSecurity() throws Exception {
        HttpResponse<String> login = browser.get("/login?error=not_allowed");

        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains("<title>Portail MiYoshiix</title>").doesNotContain("default-ui.css");
    }

    @Test
    void renvoie404PourUnFichierStatiqueAbsent() throws Exception {
        assertThat(browser.get("/main-absent.js").statusCode()).isEqualTo(404);
    }

    @Test
    void neRetombePasSurLaCoquillePourUneRouteApiInconnue() throws Exception {
        assertThat(browser.get("/api/inexistant").statusCode()).isEqualTo(401);

        users.save(new AppUser(ALLOWED_ID, "pseudo", AppRole.USER));
        loginWithDiscord(discordUser(ALLOWED_ID));

        HttpResponse<String> unknown = browser.get("/api/inexistant");
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(unknown.body()).doesNotContain("<!doctype html>");
    }

    // --- Outils ---------------------------------------------------------------------

    /** Enchaîne la redirection vers Discord puis le retour de Discord avec un code. */
    private HttpResponse<String> loginWithDiscord(String userJson) throws IOException, InterruptedException {
        discord.respondWithUser(userJson);
        return completeAuthorization(startAuthorization());
    }

    /** Étape 1 : le portail redirige vers Discord. Renvoie les paramètres de la redirection. */
    private Map<String, String> startAuthorization() throws IOException, InterruptedException {
        HttpResponse<String> response = browser.get("/oauth2/authorization/discord");
        assertThat(response.statusCode()).isEqualTo(302);

        URI redirect = URI.create(location(response));
        assertThat(redirect.toString()).startsWith(discord.baseUrl() + "/oauth2/authorize");

        Map<String, String> params = queryParams(redirect);
        assertThat(params).containsKeys("state", "redirect_uri", "code_challenge");
        assertThat(params).containsEntry("code_challenge_method", "S256");
        return params;
    }

    /** Étape 2 : Discord renvoie le navigateur vers le portail avec un code et le state. */
    private HttpResponse<String> completeAuthorization(Map<String, String> authorize)
            throws IOException, InterruptedException {
        String state = URLEncoder.encode(authorize.get("state"), StandardCharsets.UTF_8);
        return browser.get(authorize.get("redirect_uri") + "?code=code-de-test&state=" + state);
    }

    private static String discordUser(String id) {
        return """
                {"id":"%s","username":"pseudo_technique","global_name":"Pseudo Affiché","avatar":"%s"}
                """.formatted(id, AVATAR_HASH);
    }

    private static String location(HttpResponse<?> response) {
        return response.headers().firstValue("Location").orElseThrow();
    }

    private static String locationPath(HttpResponse<?> response) {
        return URI.create(location(response)).getPath();
    }

    private static Map<String, String> queryParams(URI uri) {
        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(
                        pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : ""));
    }

    private static FakeDiscordServer startFakeDiscord() {
        try {
            return new FakeDiscordServer();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de démarrer le faux serveur Discord", e);
        }
    }
}
