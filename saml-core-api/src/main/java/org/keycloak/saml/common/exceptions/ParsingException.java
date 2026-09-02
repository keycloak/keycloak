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

import java.security.GeneralSecurityException;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * General Exception indicating parsing exception
 *
 * @author Anil.Saldhana@redhat.com
 * @since May 22, 2009
 */
public class ParsingException extends GeneralSecurityException {

    private Location location;

    public ParsingException() {
        super();
    }

    public ParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParsingException(String message) {
        super(message);
    }

    public ParsingException(Throwable cause) {
        super(cause);
    }

    public ParsingException(XMLStreamException xmle) {
        super(xmle);
        location = xmle.getLocation();
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "ParsingException [location=" + location + "]" + super.toString();
    }
}