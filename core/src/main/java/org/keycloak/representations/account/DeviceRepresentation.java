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

package org.keycloak.representations.account;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DeviceRepresentation {

    public static final String UNKNOWN = "Unknown";
    private static final String OTHER = "Other";
    private static final String BROWSER_VERSION_SEPARATOR = "/";
    
    public static DeviceRepresentation unknown() {
        DeviceRepresentation device = new DeviceRepresentation();

        device.setOs(OTHER);
        device.setDevice(OTHER);
        
        return device;
    }

    private String id;
    private String ipAddress;
    private String os;
    private String osVersion;
    private String browser;
    private String device;
    private int lastAccess;
    private Boolean current;
    private List<SessionRepresentation> sessions;
    private boolean mobile;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ip) {
        this.ipAddress = ip;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getOsVersion() {
        if (osVersion == null) {
            return UNKNOWN;
        }
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public void setBrowser(String browser, String version) {
        if (browser == null) {
            this.browser = OTHER;
        } else {
            this.browser = new StringBuilder(browser).append(BROWSER_VERSION_SEPARATOR).append(version == null ? UNKNOWN : version).toString();
        }
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

    public Boolean getCurrent() {
        return current;
    }

    public void setCurrent(Boolean current) {
        this.current = current;
    }

    public void addSession(SessionRepresentation sessionRep) {
        if (this.sessions == null) {
            this.sessions = new ArrayList<>();
        }
        this.sessions.add(sessionRep);
    }

    public List<SessionRepresentation> getSessions() {
        return sessions;
    }

    public void setMobile(boolean mobile) {
        this.mobile = mobile;
    }

    public boolean isMobile() {
        return mobile;
    }
}
