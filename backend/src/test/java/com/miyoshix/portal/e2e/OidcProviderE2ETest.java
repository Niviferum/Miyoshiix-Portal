package com.miyoshix.portal.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.miyoshix.portal.PostgresTestSupport;
import com.miyoshix.portal.user.AppRole;
import com.miyoshix.portal.user.AppUser;
import com.miyoshix.portal.user.AppUserRepository;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Le portail comme fournisseur d'identité OpenID Connect : un faux module suit le parcours
 * complet (autorisation, connexion Discord, code, jetons, userinfo) contre le vrai serveur.
 * Les modules et leurs secrets sont déclarés dans {@code application-test.yml}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PostgresTestSupport.class)
class OidcProviderE2ETest {

    private static final String ISSUER = "http://portal.localhost";
    private static final String USER_ID = "123456789012345678";
    private static final String OUTSIDER_ID = "987654321098765432";

    private static final Module EXEMPLE = new Module(
            "exemple", "secret-exemple", "http://exemple.localhost:9001/login/oauth2/code/miyoshiix");
    private static final Module ADMIN_ONLY = new Module(
            "admin-only", "secret-admin", "http://admin.localhost:9002/login/oauth2/code/miyoshiix");
    private static final Module DISABLED = new Module(
            "desactive", "secret-desactive", "http://desactive.localhost:9003/login/oauth2/code/miyoshiix");

    private static final FakeDiscordServer discord = FakeDiscordServer.startShared();

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
        browser = new TestBrowser(portalUrl());
    }

    // --- Découverte ---------------------------------------------------------------

    @Test
    void publieLaConfigurationOpenIdConnect() throws Exception {
        HttpResponse<String> response = browser.get("/.well-known/openid-configuration");

        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> config = JSONObjectUtils.parse(response.body());
        assertThat(config)
                .containsEntry("issuer", ISSUER)
                .containsEntry("authorization_endpoint", ISSUER + "/oauth2/authorize")
                .containsEntry("token_endpoint", ISSUER + "/oauth2/token")
                .containsEntry("jwks_uri", ISSUER + "/oauth2/jwks")
                .containsEntry("userinfo_endpoint", ISSUER + "/userinfo");
        assertThat(JSONObjectUtils.getStringList(config, "code_challenge_methods_supported")).contains("S256");
        assertThat(response.headers().firstValue("Content-Security-Policy")).hasValue("default-src 'none'; frame-ancestors 'none'");
    }

    // --- Parcours complet -----------------------------------------------------------

    @Test
    void connecteUnModuleSansSessionEnPassantParDiscord() throws Exception {
        users.save(new AppUser(USER_ID, "ancien", AppRole.USER));
        discord.respondWithUser(discordUser(USER_ID));
        Pkce pkce = Pkce.generate();

        // 1. Le module envoie le navigateur vers le portail, sans session.
        HttpResponse<String> authorize = browser.get(authorizeUrl(EXEMPLE, "etat-module", pkce));
        assertThat(authorize.statusCode()).isEqualTo(302);
        assertThat(URI.create(location(authorize)).getPath()).isEqualTo("/oauth2/authorization/discord");

        // 2. Le portail envoie vers Discord, qui revient avec un code.
        HttpResponse<String> toDiscord = browser.get(location(authorize));
        Map<String, String> discordParams = queryParams(URI.create(location(toDiscord)));
        HttpResponse<String> discordCallback = browser.get(discordParams.get("redirect_uri")
                + "?code=code-discord&state=" + URLEncoder.encode(discordParams.get("state"), StandardCharsets.UTF_8));

        // 3. Connexion faite : le portail reprend la demande d'autorisation mémorisée.
        assertThat(discordCallback.statusCode()).isEqualTo(302);
        assertThat(URI.create(location(discordCallback)).getPath()).isEqualTo("/oauth2/authorize");

        // 4. Le portail renvoie au module avec un code et l'état d'origine.
        HttpResponse<String> toModule = browser.get(location(discordCallback));
        Map<String, String> moduleParams = redirectParams(toModule, EXEMPLE);
        assertThat(moduleParams).containsEntry("state", "etat-module").containsKey("code");

        // 5. Le module échange le code contre les jetons.
        HttpResponse<String> tokens = exchangeCode(EXEMPLE, moduleParams.get("code"), pkce.verifier());
        assertThat(tokens.statusCode()).isEqualTo(200);
        Map<String, Object> tokenJson = JSONObjectUtils.parse(tokens.body());
        assertThat(tokenJson).containsKeys("access_token", "id_token").doesNotContainKey("refresh_token");

        JWTClaimsSet idToken = verifiedClaims((String) tokenJson.get("id_token"));
        assertThat(idToken.getIssuer()).isEqualTo(ISSUER);
        assertThat(idToken.getAudience()).containsExactly("exemple");
        assertThat(idToken.getSubject()).isEqualTo(USER_ID);
        assertThat(idToken.getStringClaim("name")).isEqualTo("Pseudo Affiché");
        assertThat(idToken.getStringClaim("role")).isEqualTo("USER");
        assertThat(idToken.getStringClaim("picture")).startsWith("https://cdn.discordapp.com/avatars/" + USER_ID);

        // 6. Le module peut relire le profil avec le jeton d'accès.
        HttpResponse<String> userInfo = browser.get("/userinfo",
                Map.of("Authorization", "Bearer " + tokenJson.get("access_token")));
        assertThat(userInfo.statusCode()).isEqualTo(200);
        assertThat(JSONObjectUtils.parse(userInfo.body()))
                .containsEntry("sub", USER_ID)
                .containsEntry("name", "Pseudo Affiché")
                .containsEntry("role", "USER");
    }

    @Test
    void connecteUnModuleSansRedemanderDiscordQuandLaSessionPortailExiste() throws Exception {
        users.save(new AppUser(USER_ID, "ancien", AppRole.USER));
        loginOnPortal(USER_ID);
        int discordCallsAfterLogin = discord.requests().size();

        HttpResponse<String> authorize = browser.get(authorizeUrl(EXEMPLE, "etat", Pkce.generate()));

        assertThat(redirectParams(authorize, EXEMPLE)).containsKey("code");
        assertThat(discord.requests()).hasSize(discordCallsAfterLogin);
    }

    @Test
    void renvoieToujoursALaRacineApresUneConnexionDirecteAuPortail() throws Exception {
        users.save(new AppUser(USER_ID, "ancien", AppRole.USER));
        // Le garde Angular appelle /api/me avant la connexion : ce 401 ne doit pas être rejoué.
        assertThat(browser.get("/api/me").statusCode()).isEqualTo(401);

        HttpResponse<String> callback = loginOnPortal(USER_ID);

        assertThat(URI.create(location(callback)).getPath()).isEqualTo("/");
    }

    // --- Refus ------------------------------------------------------------------------

    @Test
    void neRedirigeJamaisVersUneAdresseDeRetourNonDeclaree() throws Exception {
        users.save(new AppUser(USER_ID, "ancien", AppRole.USER));
        loginOnPortal(USER_ID);
        Module attacker = new Module(EXEMPLE.clientId(), EXEMPLE.secret(), "http://evil.localhost/vol-de-code");

        HttpResponse<String> authorize = browser.get(authorizeUrl(attacker, "etat", Pkce.generate()));

        assertThat(authorize.statusCode()).isEqualTo(400);
        assertThat(authorize.headers().firstValue("Location")).isEmpty();
    }

    @Test
    void exigePkce() throws Exception {
        users.save(new AppUser(USER_ID, "ancien", AppRole.USER));
        loginOnPortal(USER_ID);
        String withoutPkce = "/oauth2/authorize?response_type=code&client_id=exemple&scope=openid%20profile&state=etat"
                + "&redirect_uri=" + URLEncoder.encode(EXEMPLE.redirectUri(), StandardCharsets.UTF_8);

        HttpResponse<String> authorize = browser.get(withoutPkce);

        Map<String, String> params = redirectParams(authorize, EXEMPLE);
        assertThat(params).containsEntry("error", "invalid_request").doesNotContainKey("code");
    }

    @Test
    void refuseUnModuleReserveAuxAdministrateursAUnUtilisateurSimple() throws Exception {
        users.save(new AppUser(USER_ID, "ancien", AppRole.USER));
        loginOnPortal(USER_ID);

        HttpResponse<String> authorize = browser.get(authorizeUrl(ADMIN_ONLY, "etat", Pkce.generate()));

        assertThat(redirectParams(authorize, ADMIN_ONLY))
                .containsEntry("error", "access_denied")
                .doesNotContainKey("code");
    }

    @Test
    void accepteUnModuleReserveAuxAdministrateursPourUnAdministrateur() throws Exception {
        users.save(new AppUser(USER_ID, "ancien", AppRole.ADMIN));
        loginOnPortal(USER_ID);

        HttpResponse<String> authorize = browser.get(authorizeUrl(ADMIN_ONLY, "etat", Pkce.generate()));

        assertThat(redirectParams(authorize, ADMIN_ONLY)).containsKey("code");
    }

    @Test
    void ignoreUnModuleDesactive() throws Exception {
        users.save(new AppUser(USER_ID, "ancien", AppRole.USER));
        loginOnPortal(USER_ID);

        HttpResponse<String> authorize = browser.get(authorizeUrl(DISABLED, "etat", Pkce.generate()));

        assertThat(authorize.statusCode()).isEqualTo(400);
        assertThat(authorize.headers().firstValue("Location")).isEmpty();
    }

    @Test
    void refuseUnSecretClientIncorrect() throws Exception {
        String code = obtainCode(EXEMPLE, Pkce.generate());

        HttpResponse<String> tokens = exchangeCode(
                new Module(EXEMPLE.clientId(), "mauvais-secret", EXEMPLE.redirectUri()), code, "verifier");

        assertThat(tokens.statusCode()).isEqualTo(401);
    }

    @Test
    void refuseUnCodeDejaUtiliseOuUnVerifierPkceFaux() throws Exception {
        Pkce pkce = Pkce.generate();
        String code = obtainCode(EXEMPLE, pkce);

        assertThat(exchangeCode(EXEMPLE, code, Pkce.generate().verifier()).statusCode()).isEqualTo(400);

        Pkce second = Pkce.generate();
        String secondCode = obtainCode(EXEMPLE, second);
        assertThat(exchangeCode(EXEMPLE, secondCode, second.verifier()).statusCode()).isEqualTo(200);
        assertThat(exchangeCode(EXEMPLE, secondCode, second.verifier()).statusCode()).isEqualTo(400);
    }

    @Test
    void neDelivreRienAUnCompteHorsListeBlanche() throws Exception {
        discord.respondWithUser(discordUser(OUTSIDER_ID));

        HttpResponse<String> authorize = browser.get(authorizeUrl(EXEMPLE, "etat", Pkce.generate()));
        HttpResponse<String> toDiscord = browser.get(location(authorize));
        Map<String, String> discordParams = queryParams(URI.create(location(toDiscord)));
        HttpResponse<String> callback = browser.get(discordParams.get("redirect_uri")
                + "?code=code-discord&state=" + URLEncoder.encode(discordParams.get("state"), StandardCharsets.UTF_8));

        assertThat(URI.create(location(callback)).getPath()).isEqualTo("/login");
        assertThat(URI.create(location(callback)).getQuery()).isEqualTo("error=not_allowed");
    }

    // --- Déconnexion OpenID Connect ------------------------------------------------------

    @Test
    void neDeconnectePasLePortailSurUnSimpleGetSansIdTokenHint() throws Exception {
        users.save(new AppUser(USER_ID, "ancien", AppRole.USER));
        loginOnPortal(USER_ID);

        HttpResponse<String> logout = browser.get("/connect/logout");

        assertThat(logout.statusCode()).isEqualTo(400);
        assertThat(browser.get("/api/me").statusCode()).isEqualTo(200);
    }

    @Test
    void deconnecteAvecIdTokenHintEtRenvoieALAdresseDeclaree() throws Exception {
        String idToken = obtainIdToken(EXEMPLE);
        String url = "/connect/logout?id_token_hint=" + idToken
                + "&post_logout_redirect_uri=" + URLEncoder.encode("http://exemple.localhost:9001/", StandardCharsets.UTF_8)
                + "&state=au-revoir";

        HttpResponse<String> logout = browser.get(url);

        assertThat(logout.statusCode()).isEqualTo(302);
        assertThat(location(logout)).isEqualTo("http://exemple.localhost:9001/?state=au-revoir");
        assertThat(browser.get("/api/me").statusCode()).isEqualTo(401);
    }

    @Test
    void neRenvoiePasVersUneAdresseDeDeconnexionNonDeclaree() throws Exception {
        String idToken = obtainIdToken(EXEMPLE);
        String url = "/connect/logout?id_token_hint=" + idToken
                + "&post_logout_redirect_uri=" + URLEncoder.encode("http://evil.localhost/", StandardCharsets.UTF_8);

        HttpResponse<String> logout = browser.get(url);

        assertThat(logout.headers().firstValue("Location").orElse("")).doesNotContain("evil.localhost");
        assertThat(logout.statusCode()).isEqualTo(400);
    }

    // --- Outils -----------------------------------------------------------------------

    private record Module(String clientId, String secret, String redirectUri) {
    }

    private record Pkce(String verifier, String challenge) {

        static Pkce generate() throws Exception {
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return new Pkce(verifier, Base64.getUrlEncoder().withoutPadding().encodeToString(hash));
        }
    }

    private String portalUrl() {
        return "http://localhost:" + port;
    }

    private static String authorizeUrl(Module module, String state, Pkce pkce) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("response_type", "code");
        params.put("client_id", module.clientId());
        params.put("redirect_uri", module.redirectUri());
        params.put("scope", "openid profile");
        params.put("state", state);
        params.put("code_challenge", pkce.challenge());
        params.put("code_challenge_method", "S256");
        return "/oauth2/authorize?" + params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    /** Connexion Discord directe au portail, comme depuis la page de connexion du SPA. */
    private HttpResponse<String> loginOnPortal(String discordId) throws Exception {
        discord.respondWithUser(discordUser(discordId));
        HttpResponse<String> toDiscord = browser.get("/oauth2/authorization/discord");
        Map<String, String> params = queryParams(URI.create(location(toDiscord)));
        return browser.get(params.get("redirect_uri")
                + "?code=code-discord&state=" + URLEncoder.encode(params.get("state"), StandardCharsets.UTF_8));
    }

    private String obtainCode(Module module, Pkce pkce) throws Exception {
        if (users.count() == 0) {
            users.save(new AppUser(USER_ID, "ancien", AppRole.USER));
            loginOnPortal(USER_ID);
        }
        return redirectParams(browser.get(authorizeUrl(module, "etat", pkce)), module).get("code");
    }

    private String obtainIdToken(Module module) throws Exception {
        Pkce pkce = Pkce.generate();
        String code = obtainCode(module, pkce);
        return (String) JSONObjectUtils.parse(exchangeCode(module, code, pkce.verifier()).body()).get("id_token");
    }

    private HttpResponse<String> exchangeCode(Module module, String code, String verifier) throws Exception {
        String basic = Base64.getEncoder().encodeToString(
                (module.clientId() + ":" + module.secret()).getBytes(StandardCharsets.UTF_8));
        // Appel serveur à serveur : aucun cookie de navigateur.
        return new TestBrowser(portalUrl()).postForm("/oauth2/token",
                Map.of("grant_type", "authorization_code",
                        "code", code,
                        "redirect_uri", module.redirectUri(),
                        "code_verifier", verifier),
                Map.of("Authorization", "Basic " + basic));
    }

    private JWTClaimsSet verifiedClaims(String jwt) throws Exception {
        SignedJWT signed = SignedJWT.parse(jwt);
        JWKSet keys = JWKSet.parse(browser.get("/oauth2/jwks").body());
        var key = keys.getKeyByKeyId(signed.getHeader().getKeyID()).toRSAKey();
        assertThat(signed.verify(new RSASSAVerifier(key))).as("signature du jeton d'identité").isTrue();
        return signed.getJWTClaimsSet();
    }

    /** Vérifie que la réponse redirige exactement vers l'adresse de retour du module et en lit les paramètres. */
    private static Map<String, String> redirectParams(HttpResponse<String> response, Module module) {
        assertThat(response.statusCode()).isEqualTo(302);
        URI target = URI.create(location(response));
        assertThat(target.getScheme() + "://" + target.getAuthority() + target.getPath()).isEqualTo(module.redirectUri());
        return queryParams(target);
    }

    private static String discordUser(String id) {
        return """
                {"id":"%s","username":"pseudo_technique","global_name":"Pseudo Affiché","avatar":"0123456789abcdef0123456789abcdef"}
                """.formatted(id);
    }

    private static String location(HttpResponse<?> response) {
        return response.headers().firstValue("Location").orElseThrow();
    }

    private static Map<String, String> queryParams(URI uri) {
        if (uri.getRawQuery() == null) {
            return Map.of();
        }
        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(
                        pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "",
                        (first, second) -> first));
    }
}
