package com.example.catalogo.shared.infrastructure.web.advice;

import java.util.List;

import com.example.catalogo.professional.application.exception.DateOutOfRangeException;
import com.example.catalogo.professional.application.exception.ProfessionalNotFoundException;
import com.example.catalogo.sync.application.exception.CatalogNotReadyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce los errores a {@code application/problem+json} con la forma del esquema {@code Problem}
 * del contrato. Los errores propios de Spring MVC (ruta inexistente, método no permitido, JSON mal
 * formado) los resuelve la clase base.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
	// Un valor que no se pudo convertir ("abc" donde va una fecha). El mensaje de Spring nombra clases
	// internas de Java, que no le sirven al cliente y no se exponen.
	private static final String INVALID_FORMAT = "has an invalid format";
	private static final String CATALOG_NOT_READY = "CATALOG_NOT_READY";
	private static final String DATE_OUT_OF_RANGE = "DATE_OUT_OF_RANGE";
	private static final String PROFESSIONAL_NOT_FOUND = "PROFESSIONAL_NOT_FOUND";

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldErrorResponse> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> FieldErrorResponse.builder()
						.objectName(error.getObjectName())
						.field(error.getField())
						.message(error.isBindingFailure() ? INVALID_FORMAT : error.getDefaultMessage())
						.build())
				.toList();
		ProblemDetail problem = ex.getBody();
		problem.setProperty("code", VALIDATION_ERROR);
		problem.setProperty("fieldErrors", fieldErrors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	// Mientras no haya versión local no hay copia con la que responder (docs/arq/contratos.md).
	@ExceptionHandler(CatalogNotReadyException.class)
	public ResponseEntity<Object> handleCatalogNotReady(CatalogNotReadyException ex, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
		problem.setProperty("code", CATALOG_NOT_READY);
		return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE, request);
	}

	// Fecha del filtro de disponibilidad fuera de rango (ADR-0065): el mismo código que usa turnos.
	@ExceptionHandler(DateOutOfRangeException.class)
	public ResponseEntity<Object> handleDateOutOfRange(DateOutOfRangeException ex, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setProperty("code", DATE_OUT_OF_RANGE);
		return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
	}

	// Mismo código que la cátedra (REF §13.1): turnos lo trata igual venga de donde venga.
	@ExceptionHandler(ProfessionalNotFoundException.class)
	public ResponseEntity<Object> handleProfessionalNotFound(ProfessionalNotFoundException ex, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setProperty("code", PROFESSIONAL_NOT_FOUND);
		return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
		// El detalle real queda en el log; al cliente no se le muestran datos internos.
		log.error("Unexpected error", ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
		return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
	}

	// Un parámetro con un tipo que no corresponde ("abc" donde va un número): se informa qué campo es,
	// como en cualquier error de validación.
	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String field = ex.getPropertyName();
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Invalid value for " + field);
		problem.setProperty("fieldErrors", List.of(FieldErrorResponse.builder()
				.field(field)
				.message(INVALID_FORMAT)
				.build()));
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	// Todos los errores pasan por acá: es el lugar para agregar el path a cada respuesta.
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
		if (response != null && response.getBody() instanceof ProblemDetail problem) {
			if (request instanceof ServletWebRequest servletRequest) {
				problem.setProperty("path", servletRequest.getRequest().getRequestURI());
			}
			// Un 400 sin código propio lo arma Spring (JSON mal formado, tipo inválido, parámetro que
			// falta): para el contrato es un VALIDATION_ERROR, y así ningún 400 llega sin code.
			boolean withoutCode = problem.getProperties() == null || !problem.getProperties().containsKey("code");
			if (statusCode.value() == HttpStatus.BAD_REQUEST.value() && withoutCode) {
				problem.setProperty("code", VALIDATION_ERROR);
			}
		}
		return response;
	}

}
