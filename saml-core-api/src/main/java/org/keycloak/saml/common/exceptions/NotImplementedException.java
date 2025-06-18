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
 * <p>An exception that indicates that something is not ready for use. Usually, this exception is thrown from methods not
 * fully implemented or tested but available for invocation.</p>
 *
 * @author Pedro Igor
 */
public class NotImplementedException extends PicketLinkException {

    public NotImplementedException(String message) {
        super(message);
    }

}
