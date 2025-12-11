package org.keycloak.testsuite.arquillian.containers;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.keycloak.common.crypto.FipsMode;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;
import org.jboss.logging.Logger;

/**
 * @author mhajas
 */
public class KeycloakQuarkusConfiguration implements ContainerConfiguration {

    protected static final Logger log = Logger.getLogger(KeycloakQuarkusConfiguration.class);

    private int bindHttpPortOffset = 100;
    private int bindHttpPort = 8080;
    private int bindHttpsPortOffset = 0;
    private int bindHttpsPort = Integer.getInteger("auth.server.https.port", 8543);
    private int managementPort = 9000;

    private String keystoreFile = System.getProperty("auth.server.keystore");

    private String keystorePassword = System.getProperty("auth.server.keystore.password");


    private String truststoreFile = System.getProperty("auth.server.truststore");

    private String truststorePassword = System.getProperty("auth.server.truststore.password");

    private int debugPort = -1;
    private Path providersPath = Paths.get(System.getProperty("auth.server.home"));
    private int startupTimeoutInSeconds = 300;
    private String route;
    private String keycloakConfigPropertyOverrides;
    private String profile;
    private String javaOpts;
    private boolean reaugmentBeforeStart;
    private String importFile = System.getProperty("migration.import.file.name");

    private FipsMode fipsMode = FipsMode.valueOfOption(System.getProperty("auth.server.fips.mode"));

    private String enabledFeatures;
    private String disabledFeatures;

    @Override
    public void validate() throws ConfigurationException {
        int basePort = getBindHttpPort();
        int newPort = basePort + bindHttpPortOffset;
        setBindHttpPort(newPort);

        int baseHttpsPort = getBindHttpsPort();
        int newHttpsPort = baseHttpsPort + bindHttpsPortOffset;
        setBindHttpsPort(newHttpsPort);

        log.infof("Keycloak will listen for http on port: %d, for https on port: %d, and for management on port: %d\n", newPort, newHttpsPort, managementPort);
    }

    public int getBindHttpPortOffset() {
        return bindHttpPortOffset;
    }

    public void setBindHttpPortOffset(int bindHttpPortOffset) {
        this.bindHttpPortOffset = bindHttpPortOffset;
    }

    public int getBindHttpsPortOffset() {
        return bindHttpsPortOffset;
    }

    public void setBindHttpsPortOffset(int bindHttpsPortOffset) {
        this.bindHttpsPortOffset = bindHttpsPortOffset;
    }

    public int getBindHttpsPort() {
        return this.bindHttpsPort;
    }

    public void setBindHttpsPort(int bindHttpsPort) {
        this.bindHttpsPort = bindHttpsPort;
    }

    public int getBindHttpPort() {
        return bindHttpPort;
    }

    public void setBindHttpPort(int bindHttpPort) {
        this.bindHttpPort = bindHttpPort;
    }

    public int getManagementPort() {
        return managementPort;
    }

    public void setManagementPort(int managementPort) {
        this.managementPort = managementPort;
    }

    public String getKeystoreFile() {
        return keystoreFile;
    }

    public void setKeystoreFile(String keystoreFile) {
        this.keystoreFile = keystoreFile;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getTruststoreFile() {
        return truststoreFile;
    }

    public void setTruststoreFile(String truststoreFile) {
        this.truststoreFile = truststoreFile;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public Path getProvidersPath() {
        return providersPath;
    }

    public void setProvidersPath(Path providersPath) {
        this.providersPath = providersPath;
    }

    // https://github.com/keycloak/keycloak/issues/20455 Overloading fails time to time with a mismatch error, most probably an Arquillian class reflection bug.
    public void setProvidersPathString(String providersPath) {
        this.providersPath = Paths.get(providersPath);
    }

    public int getStartupTimeoutInSeconds() {
        return startupTimeoutInSeconds;
    }

    public void setStartupTimeoutInSeconds(int startupTimeoutInSeconds) {
        this.startupTimeoutInSeconds = startupTimeoutInSeconds;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public void setJavaOpts(String javaOpts) {
        this.javaOpts = javaOpts;
    }

    public String getJavaOpts() {
        return javaOpts;
    }

    public void appendJavaOpts(String javaOpts) {
        if (javaOpts == null) {
            setJavaOpts(javaOpts);
        } else {
            setJavaOpts(this.javaOpts + " " + javaOpts);
        }
    }

    public boolean isReaugmentBeforeStart() {
        return reaugmentBeforeStart;
    }

    public void setReaugmentBeforeStart(boolean reaugmentBeforeStart) {
        this.reaugmentBeforeStart = reaugmentBeforeStart;
    }

    public int getDebugPort() {
        return debugPort;
    }

    public void setDebugPort(int debugPort) {
        this.debugPort = debugPort;
    }

    public String getImportFile() {
        return importFile;
    }

    public void setImportFile(String importFile) {
        this.importFile = importFile;
    }

    public FipsMode getFipsMode() {
        return fipsMode;
    }

    public void setFipsMode(FipsMode fipsMode) {
        this.fipsMode = fipsMode;
    }

    public void setEnabledFeatures(String enabledFeatures) {
        this.enabledFeatures = enabledFeatures;
    }

    public String getEnabledFeatures() {
        return enabledFeatures;
    }

    public String getDisabledFeatures() {
        return disabledFeatures;
    }

    public void setDisabledFeatures(String disabledFeatures) {
        this.disabledFeatures = disabledFeatures;
    }
}
