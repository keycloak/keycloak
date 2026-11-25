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

/**
 * Gives access to the implementation behind an object that may be wrapped, similar to
 * {@code jakarta.persistence.EntityManager#unwrap(Class)} and {@code java.sql.Wrapper#unwrap(Class)}.
 * <p>
 * Code that needs a specific implementation, or an interface beyond the one it obtained the object by, calls
 * {@link #unwrap(Class)} instead of casting, so a wrapper around the object keeps working.
 * <p>
 * Guidance for callers:
 * <ul>
 * <li>Use it only for operations that are specific to an implementation and not part of the SPI interface. Call the
 * methods of the SPI interface on the object obtained from the session, as calls on the unwrapped object bypass
 * every wrapper in between.</li>
 * <li>Prefer an interface to an implementation class, so a wrapper can implement and intercept it itself.</li>
 * <li>Check for {@code null} where the implementation can be configured away.</li>
 * <li>Do not keep the result beyond the session, compare it by identity, or close it.</li>
 * </ul>
 * <p>
 * Code that reaches its own implementation, rather than whatever implementation is configured, does not need this
 * method:
 * <ul>
 * <li>Hold a direct reference where the code already creates or owns the object, for example a transaction created
 * by the provider it belongs to.</li>
 * <li>Otherwise look the provider up by its factory's ID, for example
 * {@code session.getProvider(RealmProvider.class, getId())}, which no wrapper replaces. The session keeps a provider
 * looked up by ID apart from the default one, so this creates a second instance even when both come from the same
 * factory. Do this only for providers without state of their own in the session, and use this method for the
 * others, such as caches that collect invalidations.</li>
 * </ul>
 * <p>
 * An implementation that calls a method of its SPI interface on {@code this} bypasses the wrappers around it. Call
 * back through the session, for example {@code session.groups()}, which hands out the outermost provider, so the
 * wrappers and caches around it take part.
 * <p>
 * A wrapper that delegates to another object overrides {@link #unwrap(Class)} to try itself first and then its
 * delegate, so several wrappers can stack. A wrapper that does not override it hides its delegate's types:
 * <pre>{@code
 * @Override
 * public <T> T unwrap(Class<T> type) {
 *     T self = UserProvider.super.unwrap(type);
 *     return self != null ? self : delegate.unwrap(type);
 * }
 * }</pre>
 */
public interface Unwrappable {

    /**
     * Returns this object, or the object it wraps, as an instance of the given type.
     *
     * @param type the class or interface to return
     * @return this object or a wrapped object of the given type, or {@code null} if there is none
     * @param <T> the type to return
     */
    default <T> T unwrap(Class<T> type) {
        return type.isInstance(this) ? type.cast(this) : null;
    }

}
