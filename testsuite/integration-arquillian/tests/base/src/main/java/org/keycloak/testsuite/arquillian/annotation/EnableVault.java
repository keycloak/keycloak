/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

/**
 * @author mhajas
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface EnableVault {

    enum PROVIDER_ID {

        PLAINTEXT("files-plaintext", "/subsystem=keycloak-server/spi=vault/provider=files-plaintext/:add(enabled=true, " +
                        "properties={dir => \"${jboss.home.dir}/standalone/configuration/vault\"})"),

        ELYTRON_CS_KEYSTORE("elytron-cs-keystore", "/subsystem=keycloak-server/spi=vault/provider=elytron-cs-keystore/:add(enabled=true, " +
                        "properties={location => \"${jboss.home.dir}/standalone/configuration/vault/credential-store.p12\", " +
                        "secret => \"MASK-3u2HNQaMogJJ8VP7J6gRIl;12345678;321\", keyStoreType => \"PKCS12\"})");

        final String name;
        final String cliInstallationCommand;

        PROVIDER_ID(final String name, final String cliInstallationCommand) {
            this.name = name;
            this.cliInstallationCommand = cliInstallationCommand;
        }

        public String getName() {
            return this.name;
        }

        public String getCliInstallationCommand() {
            return this.cliInstallationCommand;
        }
    };

    PROVIDER_ID providerId() default PROVIDER_ID.PLAINTEXT;
}

