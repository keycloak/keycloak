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

package org.keycloak.federation.sssd.impl;

import org.jboss.logging.Logger;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;

/**
 * PAMAuthenticator for Unix users
 *
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 * @version $Revision: 1 $
 */
public class PAMAuthenticator {

    private static final String PAM_SERVICE = "keycloak";
    private static final Logger logger = Logger.getLogger(PAMAuthenticator.class);
    private final String username;
    private final String[] factors;

    public PAMAuthenticator(String username, String... factors) {
        this.username = username;
        this.factors = factors;
    }

    /**
     * Returns true if user was successfully authenticated against PAM
     *
     * @return UnixUser object if user was successfully authenticated
     */
    public UnixUser authenticate() {
        PAM pam = null;
        UnixUser user = null;
        try {
            pam = new PAM(PAM_SERVICE);
            user = pam.authenticate(username, factors);
        } catch (PAMException e) {
            logger.error("Authentication failed", e);
            e.printStackTrace();
        } finally {
            if (pam != null) {
                pam.dispose();
            }
        }
        return user;
    }
}
