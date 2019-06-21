package org.keycloak.common;

public class DeviceInfo {
    public static String ID = "DEVICE_INFO";
    private String device;
    private String browser;
    private String browserVersion;
    private String os;
    private String osVersion;
    private String userAgent;

    public DeviceInfo() {
    }

    public DeviceInfo(String device, String browser, String browserVersion, String os, String osVersion, String userAgent) {
        this.device = device;
        this.browser = browser;
        this.browserVersion = browserVersion;
        this.os = os;
        this.osVersion = osVersion;
        this.userAgent = userAgent;
    }

    public static DeviceInfo create(String deviceInfo) {
        if (deviceInfo != null && deviceInfo.matches(".*:.*:.*:.*:.*:.*")) {
            String[] info = deviceInfo.split(":");
            return new DeviceInfo(info[0], info[1], info[2], info[3], info[4], info[5]);
        } else
            return new DeviceInfo("", "", "", "", "", "");
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public void setBrowserVersion(String browserVersion) {
        this.browserVersion = browserVersion;
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

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(device);
        builder.append(':');
        builder.append(browser);
        builder.append(':');
        builder.append(browserVersion);
        builder.append(':');
        builder.append(os);
        builder.append(':');
        builder.append(osVersion);
        builder.append(':');
        builder.append(userAgent);
        return builder.toString();
    }
}
