package de.chesscoach.infrastructure.error;

import de.chesscoach.api.ApiMapper;
import de.chesscoach.application.ErrorCode;
import de.chesscoach.application.GameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
        return respond(ErrorCode.VALIDATION_FAILED, problem(ErrorCode.VALIDATION_FAILED,
                "Die Anfrage ist unvollständig oder enthält ungültige Werte."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NoResourceFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Die angeforderte Ressource existiert nicht.");
        problem.setTitle("Nicht gefunden");
        problem.setProperty("code", "NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
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
        return problem;
    }

    private static ResponseEntity<ProblemDetail> respond(ErrorCode code, ProblemDetail problem) {
        return ResponseEntity.status(code.status()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }
}
