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
package org.keycloak.saml.common.exceptions;

/**
 * <p>Any exception that is raised by the security module extends from this runtime exception class, making it easy for
 * other modules and extensions to catch all security-related exceptions in a single catch block, if need be.
 * </p>
 *
 * <p>This class is used as the root instead of {@link SecurityException} to avoid confusion and potential conflicts. Eg.: many other
 * frameworks and products (eg.: JEE containers) relies on the {@link SecurityException} to perform some special handling.</p>
 */
public class PicketLinkException extends RuntimeException {

    public PicketLinkException() {
        super();
    }

    public PicketLinkException(String message, Throwable cause) {
        super(message, cause);
    }

    public PicketLinkException(String message) {
        super(message);
    }

    public PicketLinkException(Throwable cause) {
        super(cause);
    }
}
