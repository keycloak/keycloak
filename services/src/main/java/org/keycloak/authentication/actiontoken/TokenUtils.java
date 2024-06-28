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
package org.keycloak.authentication.actiontoken;

import org.keycloak.TokenVerifier;
import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.representations.JsonWebToken;
import java.util.function.BooleanSupplier;

/**
 *
 * @author hmlnarik
 */
public class TokenUtils {
    /**
     * Returns a predicate for use in {@link TokenVerifier} using the given boolean-returning function.
     * When the function return {@code false}, this predicate throws a {@link ExplainedTokenVerificationException}
     * with {@code message} and {@code errorEvent} set from {@code errorMessage} and {@code errorEvent}, .
     *
     * @param function 
     * @param errorEvent
     * @param errorMessage
     * @return
     */
    public static Predicate<JsonWebToken> checkThat(BooleanSupplier function, String errorEvent, String errorMessage) {
        return (JsonWebToken t) -> {
            if (! function.getAsBoolean()) {
                throw new ExplainedTokenVerificationException(t, errorEvent, errorMessage);
            }

            return true;
        };
    }

    /**
     * Returns a predicate for use in {@link TokenVerifier} using the given boolean-returning function.
     * When the function return {@code false}, this predicate throws a {@link ExplainedTokenVerificationException}
     * with {@code message} and {@code errorEvent} set from {@code errorMessage} and {@code errorEvent}, .
     *
     * @param function
     * @param errorEvent
     * @param errorMessage
     * @return
     */
    public static <T extends JsonWebToken> Predicate<T> checkThat(java.util.function.Predicate<T> function, String errorEvent, String errorMessage) {
        return (T t) -> {
            if (! function.test(t)) {
                throw new ExplainedTokenVerificationException(t, errorEvent, errorMessage);
            }

            return true;
        };
    }

    
    /**
     * Returns a predicate that is applied only if the given {@code condition} evaluates to {@true}. In case
     * it evaluates to {@code false}, the predicate passes.
     * @param <T>
     * @param condition Condition guarding execution of the predicate
     * @param predicate Predicate that gets tested if the condition evaluates to {@code true}
     * @return 
     */
    public static <T extends JsonWebToken> Predicate<T> onlyIf(java.util.function.Predicate<T> condition, Predicate<T> predicate) {
        return t -> (! condition.test(t)) || predicate.test(t);
    }

    public static <T extends JsonWebToken> Predicate<? super T>[] predicates(Predicate<? super T>... predicate) {
        return predicate;
    }
}
