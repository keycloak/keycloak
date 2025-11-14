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

package org.keycloak.testsuite.util.cli;

import java.util.Objects;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.StorageProviderRealmModel;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.managers.UserStorageSyncManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SyncDummyFederationProviderCommand extends AbstractCommand {

    @Override
    protected void doRunCommand(KeycloakSession session) {
        int waitTime = getIntArg(0);
        int changedSyncPeriod = getIntArg(1);

        RealmModel realm = session.realms().getRealmByName("master");
        UserStorageProviderModel fedProviderModel = findUserStorageProviderByName(session, "cluster-dummy", realm);
        if (fedProviderModel == null) {
            MultivaluedHashMap<String, String> cfg = fedProviderModel.getConfig();
            updateConfig(cfg, waitTime);

            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setProviderId("sync-dummy");
            model.setPriority(1);
            model.setName("cluster-dummy");
            model.setFullSyncPeriod(-1);
            model.setChangedSyncPeriod(changedSyncPeriod);
            model.setLastSync(-1);
            fedProviderModel = new UserStorageProviderModel(realm.addComponentModel(model));
        } else {
            MultivaluedHashMap<String, String> cfg = fedProviderModel.getConfig();
            updateConfig(cfg, waitTime);
            fedProviderModel.setChangedSyncPeriod(changedSyncPeriod);
            realm.updateComponent(fedProviderModel);
        }

        new UserStorageSyncManager().notifyToRefreshPeriodicSync(session, realm, fedProviderModel, false);

        log.infof("User federation provider created and sync was started", waitTime);
    }

    private void updateConfig(MultivaluedHashMap<String, String> cfg, int waitTime) {
        cfg.putSingle("wait-time", String.valueOf(waitTime));
    }

    public static UserStorageProviderModel findUserStorageProviderByName(KeycloakSession session, String displayName, RealmModel realm) {
        if (displayName == null) {
            return null;
        }

        return ((StorageProviderRealmModel) realm).getUserStorageProvidersStream()
                .filter(fedProvider -> Objects.equals(fedProvider.getName(), displayName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String getName() {
        return "startSyncDummy";
    }

    @Override
    public String printUsage() {
        return super.printUsage() + " <wait-time-before-sync-commit-in-seconds> <changed-sync-period-in-seconds>";
    }
}
