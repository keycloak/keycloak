/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.utils;

import javax.ws.rs.BadRequestException;
import org.jboss.logging.Logger;

/**
 *
 * @author Stan Silvert
 */
public class ReservedCharValidator {
    protected static final Logger logger = Logger.getLogger(ReservedCharValidator.class);
    
    // https://tools.ietf.org/html/rfc3986#section-2.2
    private static final String[] RESERVED_CHARS = { ":", "/", "?", "#", "[", "@", "!", "$", 
                                                   "&", "(", ")", "*", "+", ",", ";", "=", 
                                                   "]", "[", "\\" };
    private ReservedCharValidator() {}
    
    public static void validate(String str) throws ReservedCharException {
        if (str == null) return;
        
        for (String c : RESERVED_CHARS) {
            if (str.contains(c)) {
                String message = "Character '" + c + "' not allowed.";
                ReservedCharException e = new ReservedCharException(message);
                logger.warn(message, e);
                throw e;
            } 
        }
    }
    
    public static class ReservedCharException extends BadRequestException {
        ReservedCharException(String msg) {
            super(msg);
        }
    }
}
