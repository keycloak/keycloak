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

package org.keycloak.models.sessions.infinispan.stream;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@ProtoTypeId(Marshalling.SESSION_WRAPPER_PREDICATE)
public class SessionWrapperPredicate<K, S extends SessionEntity> extends BaseRealmPredicate<K, SessionEntityWrapper<S>> {

    @ProtoFactory
    SessionWrapperPredicate(String realmId) {
        super(realmId);
    }

    public static <K1, T extends SessionEntity> SessionWrapperPredicate<K1, T> create(String realm) {
        return new SessionWrapperPredicate<>(realm);
    }

    @Override
    String realmIdFrom(SessionEntityWrapper<S> value) {
        return value.getEntity().getRealmId();
    }

}
