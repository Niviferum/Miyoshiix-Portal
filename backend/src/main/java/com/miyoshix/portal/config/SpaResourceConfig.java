package com.miyoshix.portal.config;

import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Sert les fichiers statiques du SPA et fait retomber les routes Angular inconnues sur
 * {@code index.html}. Les préfixes réservés et les chemins de fichiers gardent leur 404.
 */
@Configuration
class SpaResourceConfig implements WebMvcConfigurer {

    private static final List<String> RESERVED_PREFIXES =
            List.of("api/", "actuator/", "oauth2/", "login/", "logout");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaFallbackResolver());
    }

    private static final class SpaFallbackResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = super.getResource(resourcePath, location);
            if (requested != null) {
                return requested;
            }
            if (isReserved(resourcePath) || looksLikeFile(resourcePath)) {
                return null; // laisse Spring répondre 404
            }
            Resource shell = location.createRelative("index.html");
            return shell.exists() && shell.isReadable() ? shell : null;
        }

        private static boolean isReserved(String resourcePath) {
            return RESERVED_PREFIXES.stream().anyMatch(resourcePath::startsWith);
        }

        /** Un point dans le dernier segment désigne un fichier, pas une route Angular. */
        private static boolean looksLikeFile(String resourcePath) {
            int lastSlash = resourcePath.lastIndexOf('/');
            return resourcePath.indexOf('.', lastSlash + 1) >= 0;
        }
    }
}
