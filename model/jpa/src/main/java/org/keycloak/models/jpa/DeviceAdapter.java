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

package org.keycloak.models.jpa;

import org.keycloak.models.DeviceModel;
import org.keycloak.models.jpa.entities.UserDeviceInfoEntity;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DeviceAdapter extends DeviceModel {

    private final UserDeviceInfoEntity entity;

    public DeviceAdapter(UserDeviceInfoEntity entity) {
        this.entity = entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public String getIp() {
        return entity.getIp();
    }

    @Override
    public int getCreated() {
        return entity.getCreated();
    }

    @Override
    public String getOs() {
        return entity.getOs();
    }

    @Override
    public int getLastAccess() {
        return entity.getLastAccess();
    }

    @Override
    public String getOsVersion() {
        return entity.getOsVersion();
    }

    @Override
    public String getBrowser() {
        return entity.getBrowser();
    }

    @Override
    public String getDevice() {
        return entity.getDevice();
    }

    @Override
    public void setIp(String ip) {
        entity.setIp(ip);
    }

    @Override
    public void setBrowser(String browser) {
        entity.setBrowser(browser);
    }

    @Override
    public void setLastAccess(int lastAccess) {
        entity.setLastAccess(lastAccess);
    }
}
