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

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.testsuite.federation.sync.SyncDummyUserFederationProviderFactory;
import org.keycloak.timer.TimerProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SyncDummyFederationProviderCommand extends AbstractCommand {

    @Override
    protected void doRunCommand(KeycloakSession session) {
        int waitTime = getIntArg(0);
        int changedSyncPeriod = getIntArg(1);

        RealmModel realm = session.realms().getRealmByName("master");
        UserFederationProviderModel fedProviderModel = KeycloakModelUtils.findUserFederationProviderByDisplayName("cluster-dummy", realm);
        if (fedProviderModel == null) {
            Map<String, String> cfg = new HashMap<>();
            updateConfig(cfg, waitTime);
            fedProviderModel = realm.addUserFederationProvider(SyncDummyUserFederationProviderFactory.SYNC_PROVIDER_ID, cfg, 1, "cluster-dummy", -1, changedSyncPeriod, -1);
        } else {
            Map<String, String> cfg = fedProviderModel.getConfig();
            updateConfig(cfg, waitTime);
            fedProviderModel.setChangedSyncPeriod(changedSyncPeriod);
            realm.updateUserFederationProvider(fedProviderModel);
        }

        new UsersSyncManager().notifyToRefreshPeriodicSync(session, realm, fedProviderModel, false);

        log.infof("User federation provider created and sync was started", waitTime);
    }

    private void updateConfig(Map<String, String> cfg, int waitTime) {
        cfg.put(SyncDummyUserFederationProviderFactory.WAIT_TIME, String.valueOf(waitTime));
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
