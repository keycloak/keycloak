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

package org.keycloak.adapters.jaas;

import org.jboss.logging.Logger;
import org.keycloak.common.VerificationException;

/**
 * Login module, which allows to authenticate Keycloak access token in environments, which rely on JAAS
 * <p/>
 * It expects login based on username and password where username doesn't matter and password is keycloak access token.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BearerTokenLoginModule extends AbstractKeycloakLoginModule {

    private static final Logger log = Logger.getLogger(BearerTokenLoginModule.class);

    @Override
    protected Auth doAuth(String username, String password) throws VerificationException {
        // Should do some checking of authenticated username if it's equivalent to passed value?
        return bearerAuth(password);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
