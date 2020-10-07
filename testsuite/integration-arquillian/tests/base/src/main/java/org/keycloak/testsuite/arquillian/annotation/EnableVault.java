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

    ;

    enum PROVIDER_ID {

        PLAINTEXT("files-plaintext",
                new String[] {
                    "/subsystem=keycloak-server/spi=vault/provider=files-plaintext/:add(enabled=true, " +
                        "properties={dir => \"${jboss.home.dir}/standalone/configuration/vault\"})"},
                new String[] {}),

        ELYTRON_CS_KEYSTORE("elytron-cs-keystore",
                new String[] {
                    // create and populate an elytron credential store on the fly.
                    "/subsystem=elytron/credential-store=test-cred-store:add(location=standalone/configuration/vault/cred-store.jceks, create=true," +
                            "relative-to=jboss.home.dir, credential-reference={clear-text => \"secretpwd1!\"})",
                    "/subsystem=elytron/credential-store=test-cred-store:add-alias(alias=master_smtp__key, secret-value=secure_master_smtp_secret)",
                    "/subsystem=elytron/credential-store=test-cred-store:add-alias(alias=test_smtp__key, secret-value=secure_test_smtp_secret)",
                    // create the elytron-cs-keystore provider (using the masked form of the credential store password.
                    "/subsystem=keycloak-server/spi=vault/provider=elytron-cs-keystore/:add(enabled=true, " +
                            "properties={location => \"${jboss.home.dir}/standalone/configuration/vault/cred-store.jceks\", " +
                            "secret => \"MASK-2RukbhkyMOXq1WzXkcUcuK;abcd9876;321\", keyStoreType => \"JCEKS\"})"},
                new String[] {
                    // remove the aliases from the credential store.
                    "/subsystem=elytron/credential-store=test-cred-store:remove-alias(alias=test_smtp__key)",
                    "/subsystem=elytron/credential-store=test-cred-store:remove-alias(alias=master_smtp__key)",
                    // remove the elytron credential store.
                    "/subsystem=elytron/credential-store=test-cred-store:remove"
                });


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

    PROVIDER_ID providerId() default PROVIDER_ID.PLAINTEXT;
}

