/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.validate;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;

import jakarta.ws.rs.core.Response;

/**
 * Denotes an error found during validation.
 */
public class ValidationError implements Serializable {

    private static final long serialVersionUID = 4950708316675951914L;

    /**
     * A generic invalid value message.
     */
    public static final String MESSAGE_INVALID_VALUE = "error-invalid-value";

    /**
     * Empty message parameters fly-weight.
     */
    private static final Object[] EMPTY_PARAMETERS = {};

    /**
     * Holds the name of the validator that reported the {@link ValidationError}.
     */
    private final String validatorId;

    /**
     * Holds an inputHint.
     * <p>
     * This could be a attribute name, a nested field path or a logical key.
     */
    private final String inputHint;

    /**
     * Holds the message key for translation.
     */
    private final String message;

    /**
     * Optional parameters for the message translation.
     */
    private final Object[] messageParameters;

    /**
     * The status code associated with this error. This information serves as a hint so that
     * callers can choose whether they want to respect the status defined for the error.
     *
     * TODO: Should be better to refactor {@code Messages} to bing messages to status code as well as any other metadata that might be associated with the message.
     */
    private Response.Status statusCode = Response.Status.BAD_REQUEST;

    public ValidationError(String validatorId, String inputHint, String message) {
        this(validatorId, inputHint, message, EMPTY_PARAMETERS);
    }

    public ValidationError(String validatorId, String inputHint, String message, Object... messageParameters) {
        this.validatorId = validatorId;
        this.inputHint = inputHint;
        this.message = message;
        this.messageParameters = messageParameters == null ? EMPTY_PARAMETERS : messageParameters.clone();
    }

    public String getValidatorId() {
        return validatorId;
    }

    public String getInputHint() {
        return inputHint;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Returns the raw message parameters, e.g. the actual input that was given for validation.
     *
     * @return
     * @see #getInputHintWithMessageParameters()
     */
    public Object[] getMessageParameters() {
        return messageParameters;
    }

    /**
     * Formats the current {@link ValidationError} with the given formatter {@link java.util.function.Function}.
     * <p>
     * The formatter {@link java.util.function.Function} will be called with the {@link #message} and
     * {@link #getInputHintWithMessageParameters()} to render the error message.
     *
     * @param formatter
     * @return
     */
    public String formatMessage(BiFunction<String, Object[], String> formatter) {
        Objects.requireNonNull(formatter, "formatter must not be null");
        return formatter.apply(message, getInputHintWithMessageParameters());
    }

    /**
     * Returns an array where the first element is the {@link #inputHint} followed by the {@link #messageParameters}.
     *
     * @return
     */
    public Object[] getInputHintWithMessageParameters() {

        // insert to current input hint into the message
        Object[] args = new Object[messageParameters.length + 1];
        args[0] = getInputHint();
        System.arraycopy(messageParameters, 0, args, 1, messageParameters.length);

        return args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ValidationError)) {
            return false;
        }
        ValidationError that = (ValidationError) o;
        return Objects.equals(validatorId, that.validatorId) && Objects.equals(inputHint, that.inputHint) && Objects.equals(message, that.message) && Arrays.equals(messageParameters, that.messageParameters);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(validatorId, inputHint, message);
        result = 31 * result + Arrays.hashCode(messageParameters);
        return result;
    }

    @Override
    public String toString() {
        return "ValidationError{" + "validatorId='" + validatorId + '\'' + ", inputHint='" + inputHint + '\'' + ", message='" + message + '\'' + ", messageParameters=" + Arrays.toString(messageParameters) + '}';
    }

    public ValidationError setStatusCode(Response.Status statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public Response.Status getStatusCode() {
        return statusCode;
    }
}
