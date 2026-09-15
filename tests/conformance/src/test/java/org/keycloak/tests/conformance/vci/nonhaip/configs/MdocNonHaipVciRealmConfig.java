/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.conformance.vci.nonhaip.configs;

import java.util.List;

import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.conformance.vci.VciConformanceRealmUtil;
import org.keycloak.tests.conformance.vci.nonhaip.VciClientKey;

import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.CLIENT;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.CLIENT2;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.attesterX509TrustIdentityProvider;
import static org.keycloak.tests.conformance.vci.nonhaip.configs.NonHaipVciRealmConfig.nonHaipConformanceClient;

public class MdocNonHaipVciRealmConfig implements RealmConfig {

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        VciConformanceRealmUtil.applyCommon(realm, true)
                .clients(nonHaipConformanceClient(CLIENT, false, VciClientKey.publicJwks(), true),
                        nonHaipConformanceClient(CLIENT2, true, VciClientKey.publicJwks2(), true),
                        VciConformanceRealmUtil.appClient())
                .update(rep -> {
                    VciConformanceRealmUtil.applyKeyProviders(rep);
                    rep.setIdentityProviders(List.of(attesterX509TrustIdentityProvider()));
                });
        return realm;
    }
}
