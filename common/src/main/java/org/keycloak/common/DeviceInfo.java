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

package org.keycloak.common;

public class DeviceInfo {

    public static final String NOTE = "DEVICE_INFO";

    private String ip;
    private String device;
    private String browser;
    private String browserVersion;
    private String os;
    private String osVersion;
    private String userAgent;

    public DeviceInfo() {
        this(null, null, null, null, null, null, null);
    }

    public DeviceInfo(String device, String browser, String browserVersion, String os, String osVersion, String ip, String userAgent) {
        this.device = device;
        this.browser = browser;
        this.browserVersion = browserVersion;
        this.os = os;
        this.osVersion = osVersion;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public String getDevice() {
        return device;
    }

    public String getBrowser() {
        return browser;
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public String getOs() {
        return os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
