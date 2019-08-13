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

package org.keycloak.models;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DeviceModel {

    public static final String DEVICE_ID = "DEVICE_ID";
    private static final String UNKNOWN = "unknown";

    private String id;
    private String ip;
    private String os;
    private String osVersion;
    private String browser;
    private String device;
    private int lastAccess;
    private int created;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public Object getOsVersionOrDefault() {
        if (getOsVersion() == null) {
            return UNKNOWN;
        }
        return getOsVersion();
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public int getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(int lastAccess) {
        this.lastAccess = lastAccess;
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public void addBrowser(String browser, String version) {
        if (this.browser == null) {
            this.browser = new StringBuilder(browser).append("/").append(version).toString();
        } else {
            this.browser = new StringBuilder(this.browser).append(",").append(browser).append("/").append(version).toString();
        }
    }

    public void addBrowser(String browser) {
        if (browser == null) {
            return;
        }
        if (getBrowser() == null) {
            setBrowser(new StringBuilder(browser).toString());
        } else if (!getBrowser().contains(browser)) {
            setBrowser(new StringBuilder(getBrowser()).append(",").append(browser).toString());
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("ip=").append(getIp()).append(", os=").append(getOs()).append(", osVersion=")
                .append(getOsVersionOrDefault()).append(", browser=").append(getBrowser()).append(", device=")
                .append(getDevice()).toString();
    }
}
