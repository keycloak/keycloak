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

package org.keycloak.services.scheduled;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserDeviceStore;
import org.keycloak.models.UserProvider;
import org.keycloak.timer.ScheduledTask;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ClearDeviceHistory implements ScheduledTask {

    /**
     * Expires entries in device history that are older than 30 days.
     */
    private static final int DEFAULT_DEVICE_EXPIRATION = 30 * 24 * 360;

    @Override
    public void run(KeycloakSession session) {
        UserProvider users = session.users();

        if (users instanceof UserDeviceStore) {
            UserDeviceStore deviceStore = (UserDeviceStore) users;
            deviceStore.removeDevices(session.getContext().getRealm(), getExpirationTime());
        }
    }

    private int getExpirationTime() {
        // TODO: We should probably allow admins to override the default value
        return Time.currentTime() - DEFAULT_DEVICE_EXPIRATION;
    }

}
