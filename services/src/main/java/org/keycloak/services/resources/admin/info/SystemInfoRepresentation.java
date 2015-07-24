package org.keycloak.services.resources.admin.info;

import org.keycloak.Version;
import org.keycloak.models.KeycloakSession;

import java.util.Date;
import java.util.Locale;

public class SystemInfoRepresentation {

    private String version;
    private String serverTime;
    private String uptime;
    private long uptimeMillis;
    private String javaVersion;
    private String javaVendor;
    private String javaVm;
    private String javaVmVersion;
    private String javaRuntime;
    private String javaHome;
    private String osName;
    private String osArchitecture;
    private String osVersion;
    private String fileEncoding;
    private String userName;
    private String userDir;
    private String userTimezone;
    private String userLocale;

    public static SystemInfoRepresentation create(KeycloakSession session) {
        SystemInfoRepresentation rep = new SystemInfoRepresentation();
        rep.version = Version.VERSION;
        rep.serverTime = new Date().toString();
        rep.uptimeMillis = System.currentTimeMillis() - session.getKeycloakSessionFactory().getServerStartupTimestamp();
        rep.uptime = formatUptime(rep.uptimeMillis);
        rep.javaVersion = System.getProperty("java.version");
        rep.javaVendor = System.getProperty("java.vendor");
        rep.javaVm = System.getProperty("java.vm.name");
        rep.javaVmVersion = System.getProperty("java.vm.version");
        rep.javaRuntime = System.getProperty("java.runtime.name");
        rep.javaHome = System.getProperty("java.home");
        rep.osName = System.getProperty("os.name");
        rep.osArchitecture = System.getProperty("os.arch");
        rep.osVersion = System.getProperty("os.version");
        rep.fileEncoding = System.getProperty("file.encoding");
        rep.userName = System.getProperty("user.name");
        rep.userDir = System.getProperty("user.dir");
        rep.userTimezone = System.getProperty("user.timezone");
        rep.userLocale = (new Locale(System.getProperty("user.country"), System.getProperty("user.language")).toString());
        return rep;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getServerTime() {
        return serverTime;
    }

    public void setServerTime(String serverTime) {
        this.serverTime = serverTime;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    public long getUptimeMillis() {
        return uptimeMillis;
    }

    public void setUptimeMillis(long uptimeMillis) {
        this.uptimeMillis = uptimeMillis;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getJavaVendor() {
        return javaVendor;
    }

    public void setJavaVendor(String javaVendor) {
        this.javaVendor = javaVendor;
    }

    public String getJavaVm() {
        return javaVm;
    }

    public void setJavaVm(String javaVm) {
        this.javaVm = javaVm;
    }

    public String getJavaVmVersion() {
        return javaVmVersion;
    }

    public void setJavaVmVersion(String javaVmVersion) {
        this.javaVmVersion = javaVmVersion;
    }

    public String getJavaRuntime() {
        return javaRuntime;
    }

    public void setJavaRuntime(String javaRuntime) {
        this.javaRuntime = javaRuntime;
    }

    public String getJavaHome() {
        return javaHome;
    }

    public void setJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getOsArchitecture() {
        return osArchitecture;
    }

    public void setOsArchitecture(String osArchitecture) {
        this.osArchitecture = osArchitecture;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getFileEncoding() {
        return fileEncoding;
    }

    public void setFileEncoding(String fileEncoding) {
        this.fileEncoding = fileEncoding;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserDir() {
        return userDir;
    }

    public void setUserDir(String userDir) {
        this.userDir = userDir;
    }

    public String getUserTimezone() {
        return userTimezone;
    }

    public void setUserTimezone(String userTimezone) {
        this.userTimezone = userTimezone;
    }

    public String getUserLocale() {
        return userLocale;
    }

    public void setUserLocale(String userLocale) {
        this.userLocale = userLocale;
    }

    private static String formatUptime(long uptime) {
        long diffInSeconds = uptime / 1000;
        long diff[] = new long[]{0, 0, 0, 0}; // sec
        diff[3] = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds); // min
        diff[2] = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds; // hours
        diff[1] = (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds; // days
        diff[0] = (diffInSeconds = (diffInSeconds / 24));

        return String.format(
                "%d day%s, %d hour%s, %d minute%s, %d second%s",
                diff[0],
                diff[0] != 1 ? "s" : "",
                diff[1],
                diff[1] != 1 ? "s" : "",
                diff[2],
                diff[2] != 1 ? "s" : "",
                diff[3],
                diff[3] != 1 ? "s" : "");
    }

}
