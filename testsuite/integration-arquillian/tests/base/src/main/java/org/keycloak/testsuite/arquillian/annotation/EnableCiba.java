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
package org.keycloak.testsuite.arquillian.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface EnableCiba {

    ;

    enum PROVIDER_ID {

        CIBA_HTTP_AUTH_CHANNEL("ciba-http-auth-channel",
                new String[] {
                        "/subsystem=keycloak-server/spi=ciba-auth-channel/provider=ciba-http-auth-channel/:add(enabled=true, " +
                            "properties={httpAuthenticationChannelUri => \"https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/request-authentication-channel\"})"},
                    new String[] {});

        final String name;
        final String[] cliInstallationCommands;
        final String[] cliRemovalCommands;

        PROVIDER_ID(final String name, final String[] cliInstallationCommands, final String[] cliRemovalCommands) {
            this.name = name;
            this.cliInstallationCommands = cliInstallationCommands;
            this.cliRemovalCommands = cliRemovalCommands;
        }

        public String getName() {
            return this.name;
        }

        public String[] getCliInstallationCommands() {
            return this.cliInstallationCommands;
        }

        public String[] getCliRemovalCommands() {
            return this.cliRemovalCommands;
        }
    };

    PROVIDER_ID providerId() default PROVIDER_ID.CIBA_HTTP_AUTH_CHANNEL;
}
