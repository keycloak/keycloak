/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization;

import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.Evaluation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface Decision<D extends Evaluation> {

    enum Effect {
        PERMIT,
        DENY
    }

    void onDecision(D evaluation);

    default void onError(Throwable cause) {
        throw new RuntimeException("Not implemented.", cause);
    }

    default void onComplete() {
    }

    default void onComplete(ResourcePermission permission) {
    }

    /**
     * Checks if the given {@code scope} is associated with any policy processed in this decision.
     *
     * @param scope the scope name
     * @return {@code true} if the scope is associated with a policy. Otherwise, {@code false}.
     */
    default boolean isEvaluated(String scope) {
        return false;
    }
}
