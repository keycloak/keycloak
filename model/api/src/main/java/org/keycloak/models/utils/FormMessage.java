/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.keycloak.models.utils;

import java.util.Arrays;

/**
 * Message (eg. error) to be shown in form.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class FormMessage {

	/**
	 * Value used for {@link #field} if message is global (not tied to any specific form field)
	 */
	public static final String GLOBAL = "global";

	private String field;
	private String message;
	private Object[] parameters;

	/**
	 * Create message.
	 * 
	 * @param field this message is for. {@link #GLOBAL} is used if null
	 * @param message key for the message
	 * @param parameters to be formatted into message
	 */
	public FormMessage(String field, String message, Object... parameters) {
		this(field, message);
		this.parameters = parameters;
	}

    public FormMessage(String message, Object...parameters) {
        this(null, message, parameters);
    }
	
	/**
     * Create message without parameters.
     * 
     * @param field this message is for. {@link #GLOBAL} is used if null
     * @param message key for the message
     */
    public FormMessage(String field, String message) {
        super();
        if (field == null)
            field = GLOBAL;
        this.field = field;
        this.message = message;
    }

	public String getField() {
		return field;
	}

	public String getMessage() {
		return message;
	}

	public Object[] getParameters() {
		return parameters;
	}

	@Override
	public String toString() {
		return "FormMessage [field=" + field + ", message=" + message + ", parameters=" + Arrays.toString(parameters) + "]";
	}

}
