package com.miyoshix.portal.e2e;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Navigateur minimal pour les tests E2E : conserve les cookies entre les requêtes et
 * ne suit jamais les redirections, afin de pouvoir inspecter chaque étape.
 */
final class TestBrowser {

    private final String baseUrl;
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /** Nom du cookie vers sa valeur. */
    private final Map<String, String> cookies = new LinkedHashMap<>();

    TestBrowser(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    HttpResponse<String> get(String pathOrUrl) throws IOException, InterruptedException {
        return send(builder(pathOrUrl).GET());
    }

    HttpResponse<String> get(String pathOrUrl, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder builder = builder(pathOrUrl).GET();
        headers.forEach(builder::header);
        return send(builder);
    }

    /** POST d'un formulaire {@code application/x-www-form-urlencoded}. */
    HttpResponse<String> postForm(String path, Map<String, String> form, Map<String, String> headers)
            throws IOException, InterruptedException {
        String body = form.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        HttpRequest.Builder builder = builder(path)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        headers.forEach(builder::header);
        return send(builder);
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    HttpResponse<String> post(String path, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder builder = builder(path).POST(HttpRequest.BodyPublishers.noBody());
        headers.forEach(builder::header);
        return send(builder);
    }

    Optional<String> cookie(String name) {
        return Optional.ofNullable(cookies.get(name));
    }

    void forgetCookie(String name) {
        cookies.remove(name);
    }

    void setCookie(String name, String value) {
        cookies.put(name, value);
    }

    /** Renvoie l'en-tête {@code Set-Cookie} complet émis pour ce cookie, attributs compris. */
    static Optional<String> setCookieHeader(HttpResponse<?> response, String name) {
        return response.headers().allValues("Set-Cookie").stream()
                .filter(header -> header.startsWith(name + "="))
                .findFirst();
    }

    private HttpRequest.Builder builder(String pathOrUrl) {
        String url = pathOrUrl.startsWith("http") ? pathOrUrl : baseUrl + pathOrUrl;
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url));
        if (!cookies.isEmpty()) {
            builder.header("Cookie", cookies.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("; ")));
        }
        return builder;
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        storeCookies(response.headers().allValues("Set-Cookie"));
        return response;
    }

    private void storeCookies(List<String> setCookieHeaders) {
        for (String header : setCookieHeaders) {
            String pair = header.split(";", 2)[0];
            int equals = pair.indexOf('=');
            String name = pair.substring(0, equals).trim();
            String value = pair.substring(equals + 1).trim();
            boolean expired = value.isEmpty() || header.toLowerCase().contains("max-age=0");
            if (expired) {
                cookies.remove(name);
            } else {
                cookies.put(name, value);
            }
        }
    }
}
