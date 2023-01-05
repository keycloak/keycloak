/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.broker;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;

import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_CONS_NAME;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PRIVATE_KEY;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PUBLIC_KEY;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KcFooTest extends AbstractKeycloakTest {


    private static final String PRIVATE_KEY = "MIIBVQIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEAs46ICYPRIkmr8diECmyT59cChTWIEiXYBY3T6OLlZrF8ofVCzbEeoUOmhrtHijxxuKSoqLWP4nNOt3rINtQNBQIDAQABAkBL2nyxuFQTLhhLdPJjDPd2y6gu6ixvrjkSL5ZEHgZXWRHzhTzBT0eRxg/5rJA2NDRMBzTTegaEGkWUt7lF5wDJAiEA5pC+h9NEgqDJSw42I52BOml3II35Z6NlNwl6OMfnD1sCIQDHXUiOIJy4ZcSgv5WGue1KbdNVOT2gop1XzfuyWgtjHwIhAOCjLb9QC3PqC7Tgx8azcnDiyHojWVesTrTsuvQPcAP5AiAkX5OeQrr1NbQTNAEe7IsrmjAFi4T/6stUOsOiPaV4NwIhAJIeyh4foIXIVQ+M4To2koaDFRssxKI9/O72vnZSJ+uA";
    private static final String PUBLIC_KEY = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBALOOiAmD0SJJq/HYhApsk+fXAoU1iBIl2AWN0+ji5WaxfKH1Qs2xHqFDpoa7R4o8cbikqKi1j+JzTrd6yDbUDQUCAwEAAQ==";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

    }

    @Test
    public void test1() {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setEnabled(true);
        realm.setRealm(REALM_CONS_NAME);
        realm.setResetPasswordAllowed(true);
        realm.setEventsListeners(Arrays.asList("jboss-logging", "event-queue"));

        importRealm(realm);
    }

    @Test
    public void test2() {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setEnabled(true);
        realm.setRealm(REALM_CONS_NAME);
        realm.setResetPasswordAllowed(true);
        realm.setEventsListeners(Arrays.asList("jboss-logging", "event-queue"));

        realm.setPublicKey(REALM_PUBLIC_KEY);
        realm.setPrivateKey(REALM_PRIVATE_KEY);

        importRealm(realm);
    }

    @Test
    public void test3() {
        test2();
    }
}
