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
package org.keycloak.storage.federated;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserConsentFederatedStorage {
    void addConsent(RealmModel realm, String userId, UserConsentModel consent);
    UserConsentModel getConsentByClient(RealmModel realm, String userId, String clientInternalId);
    List<UserConsentModel> getConsents(RealmModel realm, String userId);
    void updateConsent(RealmModel realm, String userId, UserConsentModel consent);
    boolean revokeConsentForClient(RealmModel realm, String userId, String clientInternalId);
}
