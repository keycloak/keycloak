/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

	public FormMessage() {
	}

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
