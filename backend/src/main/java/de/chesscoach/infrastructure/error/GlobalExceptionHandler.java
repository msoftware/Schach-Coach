package de.chesscoach.infrastructure.error;

import de.chesscoach.api.ApiMapper;
import de.chesscoach.application.ErrorCode;
import de.chesscoach.application.GameException;
import de.chesscoach.infrastructure.logging.AccessLogFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;

/**
 * Einheitliches Fehlerformat nach RFC 9457 ({@code application/problem+json}). Stacktraces und interne
 * Prozessinformationen werden nie an das Frontend ausgegeben.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ApiMapper mapper;

    public GlobalExceptionHandler(ApiMapper mapper) {
        this.mapper = mapper;
    }

    @ExceptionHandler(GameException.class)
    public ResponseEntity<ProblemDetail> handleGame(GameException e) {
        // Fachliche Fehler (veraltete Version, Partie nach Neustart nicht mehr vorhanden ...) sind normaler Ablauf.
        log.debug("Fachlicher Fehler {}: {}", e.code(), e.getMessage());
        ProblemDetail problem = problem(e.code(), e.getMessage());
        if (e.current() != null) {
            problem.setProperty("positionVersion", e.current().positionVersion());
            problem.setProperty("currentState", mapper.game(e.current()));
        }
        return respond(e.code(), problem);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ProblemDetail> handleValidation(Exception e) {
        log.info("Ungültige Anfragedaten ({})", e.getClass().getSimpleName());
        return respond(ErrorCode.VALIDATION_FAILED, problem(ErrorCode.VALIDATION_FAILED,
                "Die Anfrage ist unvollständig oder enthält ungültige Werte."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NoResourceFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Die angeforderte Ressource existiert nicht.");
        problem.setTitle("Nicht gefunden");
        problem.setProperty("code", "NOT_FOUND");
        withRequestId(problem);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }

    /**
     * Fehler, die Spring selbst einem HTTP-Status zuordnet (falsche Methode, falscher Content-Type, ...). Sie entstehen
     * typischerweise durch falsch konfigurierte Proxys oder Clients und sind kein interner Fehler – als 500 gemeldet
     * würden sie die eigentliche Ursache verschleiern.
     */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ProblemDetail> handleRequestError(Exception e) {
        HttpStatusCode status = ((ErrorResponse) e).getStatusCode();
        String detail = switch (e) {
            case HttpRequestMethodNotSupportedException m -> "Die HTTP-Methode " + m.getMethod() + " ist hier nicht erlaubt"
                    + (m.getSupportedHttpMethods() == null ? "." : " (erlaubt: " + m.getSupportedHttpMethods() + ").")
                    + " Prüfe, ob ein Proxy oder eine Weiterleitung die Anfrage verändert.";
            case HttpMediaTypeNotSupportedException m -> "Der Inhaltstyp " + m.getContentType()
                    + " wird nicht unterstützt; erwartet wird application/json.";
            default -> "Die Anfrage ist ungültig.";
        };
        log.warn("Ungültige Anfrage: {}", e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle("Ungültige Anfrage");
        problem.setProperty("code", status.value() == 405 ? "METHOD_NOT_ALLOWED"
                : status.value() == 415 ? "UNSUPPORTED_MEDIA_TYPE" : "BAD_REQUEST");
        withRequestId(problem);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e) {
        log.error("Unerwarteter Fehler", e);
        return respond(ErrorCode.INTERNAL_ERROR, problem(ErrorCode.INTERNAL_ERROR, "Es ist ein interner Fehler aufgetreten."));
    }

    public static ProblemDetail problem(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(code.status()), detail);
        problem.setType(URI.create(code.type()));
        problem.setTitle(code.title());
        problem.setProperty("code", code.name());
        return withRequestId(problem);
    }

    /** Nennt die Request-ID der Anfrage, damit sich ein im Browser gemeldeter Fehler im Log wiederfinden lässt. */
    private static ProblemDetail withRequestId(ProblemDetail problem) {
        String requestId = MDC.get(AccessLogFilter.REQUEST_ID);
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }
        return problem;
    }

    private static ResponseEntity<ProblemDetail> respond(ErrorCode code, ProblemDetail problem) {
        return ResponseEntity.status(code.status()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }
}
