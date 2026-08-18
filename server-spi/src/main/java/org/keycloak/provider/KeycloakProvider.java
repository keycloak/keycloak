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

package org.keycloak.provider;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers a {@link ProviderFactory} implementation for a given SPI without a
 * {@code META-INF/services/...} descriptor.
 * <p>
 * The {@link #value()} names the SPI's provider factory interface the annotated class is registered
 * for — exactly the interface a {@code META-INF/services} file would be named after, for example
 * {@code @KeycloakProvider(EventListenerProviderFactory.class)}. The annotated class must be a public,
 * non-abstract class with a public no-arg constructor that implements that interface. A class is
 * registered for exactly one SPI this way; a class that must serve several SPIs keeps using
 * {@code META-INF/services} descriptors.
 * <p>
 * Annotated classes are discovered at build time by the Quarkus deployment processor and registered
 * with the deployment used for provider discovery. Discovery via {@link java.util.ServiceLoader}
 * continues to work in parallel, so extensions shipping service descriptors remain supported and a
 * factory registered through both mechanisms is loaded only once.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KeycloakProvider {

    /**
     * The provider factory interface of the SPI the annotated class is registered for.
     */
    Class<? extends ProviderFactory> value();

}
