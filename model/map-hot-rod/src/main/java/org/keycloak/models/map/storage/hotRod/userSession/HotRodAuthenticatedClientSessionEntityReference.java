/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.hotRod.userSession;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;

@ProtoDoc("@Indexed")
public class HotRodAuthenticatedClientSessionEntityReference {

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 1)
    public String clientId;

    @ProtoField(number = 2)
    public String clientSessionId;

    public HotRodAuthenticatedClientSessionEntityReference() {}

    public HotRodAuthenticatedClientSessionEntityReference(String clientId, String clientSessionId) {
        this.clientId = clientId;
        this.clientSessionId = clientSessionId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSessionId() {
        return clientSessionId;
    }
}
