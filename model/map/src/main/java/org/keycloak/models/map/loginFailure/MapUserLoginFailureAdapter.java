/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.loginFailure;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.common.TimeAdapter;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapUserLoginFailureAdapter extends AbstractUserLoginFailureModel<MapUserLoginFailureEntity> {
    public MapUserLoginFailureAdapter(KeycloakSession session, RealmModel realm, MapUserLoginFailureEntity entity) {
        super(session, realm, entity);
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public String getUserId() {
        return entity.getUserId();
    }

    @Override
    public int getFailedLoginNotBefore() {
        Long failedLoginNotBefore = entity.getFailedLoginNotBefore();
        return failedLoginNotBefore == null ? 0 : TimeAdapter.fromLongWithTimeInSecondsToIntegerWithTimeInSeconds(failedLoginNotBefore);
    }

    @Override
    public void setFailedLoginNotBefore(int notBefore) {
        entity.setFailedLoginNotBefore(TimeAdapter.fromIntegerWithTimeInSecondsToLongWithTimeAsInSeconds(notBefore));
    }

    @Override
    public int getNumFailures() {
        Integer numFailures = entity.getNumFailures();
        return numFailures == null ? 0 : numFailures;
    }

    @Override
    public void incrementFailures() {
        entity.setNumFailures(getNumFailures() + 1);
    }

    @Override
    public void clearFailures() {
        entity.clearFailures();
    }

    @Override
    public long getLastFailure() {
        Long lastFailure = entity.getLastFailure();
        return lastFailure == null ? 0l : lastFailure;
    }

    @Override
    public void setLastFailure(long lastFailure) {
        entity.setLastFailure(lastFailure);
    }

    @Override
    public String getLastIPFailure() {
        return entity.getLastIPFailure();
    }

    @Override
    public void setLastIPFailure(String ip) {
        entity.setLastIPFailure(ip);
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", entity.getId(), hashCode());
    }
}
