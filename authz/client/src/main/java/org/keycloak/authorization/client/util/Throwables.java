/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.client.util;

import org.keycloak.authorization.client.AuthorizationDeniedException;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class Throwables {

    /**
     * Handles an {@code exception} and wraps it into a {@link RuntimeException}. The resulting exception contains
     * more details in case the given {@code exception} is of a {@link HttpResponseException}.
     *
     * @param message the message
     * @param exception the root exception
     * @return a {@link RuntimeException} wrapping the given {@code exception}
     */
    public static RuntimeException handleAndWrapException(String message, Exception exception) {
        if (exception instanceof HttpResponseException) {
            throw handleAndWrapHttpResponseException(message, HttpResponseException.class.cast(exception));
        }

        return new RuntimeException(message, exception);
    }

    private static RuntimeException handleAndWrapHttpResponseException(String message, HttpResponseException exception) {
        HttpResponseException hre = HttpResponseException.class.cast(exception);
        StringBuilder detail = new StringBuilder(message);
        byte[] bytes = hre.getBytes();

        if (bytes != null) {
            detail.append(". Server message: ").append(new String(bytes));
        }

        if (403 == exception.getStatusCode()) {
            throw new AuthorizationDeniedException(detail.toString(), exception);
        }

        return new RuntimeException(detail.toString(), exception);
    }
}
