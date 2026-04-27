/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jgroups.certificates;

import java.util.function.Function;

import org.keycloak.marshalling.Marshalling;

import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * Reloads the JGroups certificate
 */
@ProtoTypeId(Marshalling.RELOAD_CERTIFICATE_FUNCTION)
public final class ReloadCertificateFunction implements Function<EmbeddedCacheManager, Void> {

    private static final ReloadCertificateFunction INSTANCE = new ReloadCertificateFunction();

    private ReloadCertificateFunction() {}

    @ProtoFactory
    public static ReloadCertificateFunction getInstance() {
        return INSTANCE;
    }

    @Override
    public Void apply(EmbeddedCacheManager embeddedCacheManager) {
        var crm = GlobalComponentRegistry.componentOf(embeddedCacheManager, CertificateReloadManager.class);
        if (crm != null) {
            crm.reloadCertificate();
        }
        return null;
    }
}
