package com.example.catalogo.professional.application.exception;

public class ProfessionalNotFoundException extends RuntimeException {

	public ProfessionalNotFoundException(Long id) {
		super("Could not find professional with id: " + id);
	}

}
