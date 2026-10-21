/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.resources;

import java.time.Instant;

import org.keycloak.provider.ProviderEvent;

/**
 * Published when the shutdown delay phase has started.
 *
 * @param timestamp The instant at which the shutdown delay was initiated.
 */
public record ShutdownDelayInitiatedEvent(Instant timestamp) implements ProviderEvent {
}
