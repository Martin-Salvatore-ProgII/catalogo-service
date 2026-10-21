package com.example.catalogo.professional.application.exception;

import java.time.LocalDate;

// La fecha del filtro de disponibilidad está antes de hoy o después del horizonte (ADR-0065).
public class DateOutOfRangeException extends RuntimeException {

	public DateOutOfRangeException(LocalDate date, LocalDate from, LocalDate to) {
		super("Date " + date + " must be between " + from + " and " + to);
	}

}
