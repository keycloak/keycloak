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

import java.util.concurrent.Callable;

import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class Throwables {

    /**
     * Handles an {@code cause} and wraps it into a {@link RuntimeException}. The resulting cause contains
     * more details in case the given {@code cause} is of a {@link HttpResponseException}.
     *
     *
     * @param callable
     * @param pat
     * @param message the message
     * @param cause the root cause
     * @return a {@link RuntimeException} wrapping the given {@code cause}
     */
    public static RuntimeException handleWrapException(String message, Throwable cause) {
        if (cause instanceof HttpResponseException) {
            throw handleAndWrapHttpResponseException(HttpResponseException.class.cast(cause));
        }

        return new RuntimeException(message, cause);
    }

    /**
     * <p>Retries the given {@code callable} after obtaining a fresh {@code token} from the server. If the attempt to retry fails
     * the exception is handled as defined by {@link #handleWrapException(String, Throwable)}.
     *
     * <p>A retry is only attempted in case the {@code cause} is a {@link HttpResponseException} with a 403 status code. In some cases the
     * session associated with the token is no longer valid and a new token must be issues.
     *
     * @param callable the callable to retry
     * @param token the token
     * @param message the message
     * @param cause the cause
     * @param <V> the result of the callable
     * @return the result of the callable
     * @throws RuntimeException in case the attempt to retry fails
     */
    public static <V> V retryAndWrapExceptionIfNecessary(Callable<V> callable, TokenCallable token, String message, Throwable cause) throws RuntimeException {
        if (token == null || !token.isRetry()) {
            throw handleWrapException(message, cause);
        }

        if (cause instanceof HttpResponseException) {
            HttpResponseException httpe = HttpResponseException.class.cast(cause);

            if (httpe.getStatusCode() == 403) {
                TokenIntrospectionResponse response = token.getHttp().<TokenIntrospectionResponse>post(token.getServerConfiguration().getTokenIntrospectionEndpoint())
                        .authentication()
                        .client()
                        .param("token", token.call())
                        .response().json(TokenIntrospectionResponse.class).execute();

                if (!response.getActive()) {
                    token.clearToken();
                    try {
                        return callable.call();
                    } catch (Exception e) {
                        throw handleWrapException(message, e);
                    }
                }

                throw handleWrapException(message, cause);
            }
        }

        throw new RuntimeException(message, cause);
    }

    private static RuntimeException handleAndWrapHttpResponseException(HttpResponseException exception) {
        if (403 == exception.getStatusCode()) {
            throw new AuthorizationDeniedException(exception);
        }

        return new RuntimeException(exception);
    }
}
