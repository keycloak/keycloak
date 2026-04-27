/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.sdjwt.consumer;

import org.keycloak.common.VerificationException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Presentation requirements to constrain the kind of credential expected.
 *
 * <p>
 * This mirrors the idea of the expressive
 * <a href="https://identity.foundation/presentation-exchange/#presentation-definition">DIF Presentation Definition</a>,
 * while enabling simpler alternatives.
 * </p>
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public interface PresentationRequirements {

    /**
     * Ensures that the configured requirements are satisfied by the presentation.
     *
     * @param disclosedPayload The fully disclosed Issuer-signed JWT of the presented token.
     * @throws VerificationException if the configured requirements are not satisfied.
     */
    void checkIfSatisfiedBy(JsonNode disclosedPayload) throws VerificationException;
}
