package com.miyoshix.portal.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les exceptions des contrôleurs en réponses JSON. Le détail des erreurs
 * inattendues reste dans les logs, associé à un identifiant de corrélation.
 */
@RestControllerAdvice(basePackages = "com.miyoshix.portal")
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Erreurs de validation : les messages viennent de nos propres contraintes. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> onValidationError(MethodArgumentNotValidException exception) {
        Map<String, String> fields = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        org.springframework.validation.FieldError::getField,
                        error -> error.getDefaultMessage() == null ? "valeur invalide" : error.getDefaultMessage(),
                        (first, second) -> first));
        return ResponseEntity.badRequest()
                .body(new ApiError("validation_error", "Requête invalide.", null, fields));
    }

    /** Relayée à Spring Security, qui choisit entre 401 et 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public void onAccessDenied(AccessDeniedException exception) {
        throw exception;
    }

    /** Exceptions portant déjà un statut HTTP correct (405, 415, 406...) : relayées. */
    @ExceptionHandler(org.springframework.web.ErrorResponseException.class)
    public void onErrorResponse(org.springframework.web.ErrorResponseException exception) {
        throw exception;
    }

    /** Filet de sécurité : tout le reste devient un 500 opaque. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> onUnexpectedError(Exception exception, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.error("Erreur inattendue [{}] sur {} {}", traceId, request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("internal_error",
                        "Une erreur est survenue. Communique cet identifiant si le problème persiste.",
                        traceId, Map.of()));
    }

    public record ApiError(String code, String message, String traceId, Map<String, String> fields) {

        public ApiError {
            fields = fields == null ? Map.of() : Map.copyOf(fields);
        }
    }
}
