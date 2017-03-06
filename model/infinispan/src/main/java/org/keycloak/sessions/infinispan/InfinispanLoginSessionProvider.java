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

package org.keycloak.sessions.infinispan;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.LoginSessionModel;
import org.keycloak.sessions.LoginSessionProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanLoginSessionProvider implements LoginSessionProvider {

    @Override
    public LoginSessionModel createLoginSession(RealmModel realm, ClientModel client, boolean browser) {
        return null;
    }

    @Override
    public LoginSessionModel getCurrentLoginSession(RealmModel realm) {
        return null;
    }

    @Override
    public LoginSessionModel getLoginSession(RealmModel realm, String loginSessionId) {
        return null;
    }

    @Override
    public void removeLoginSession(RealmModel realm, LoginSessionModel loginSession) {

    }

    @Override
    public void removeExpired(RealmModel realm) {

    }

    @Override
    public void onRealmRemoved(RealmModel realm) {

    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {

    }

    @Override
    public void close() {

    }
}
