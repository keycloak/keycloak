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

package org.keycloak.proxy;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.representations.adapters.config.AdapterConfig;

import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProxyConfig {
    @JsonProperty("bind-address")
    protected String bindAddress = "localhost";
    @JsonProperty("http-port")
    protected Integer httpPort;
    @JsonProperty("https-port")
    protected Integer httpsPort;
    @JsonProperty("keystore")
    protected String keystore;
    @JsonProperty("keystore-password")
    protected String keystorePassword;
    @JsonProperty("key-password")
    protected String keyPassword;
    @JsonProperty("buffer-size")
    protected Integer bufferSize;
    @JsonProperty("buffers-per-region")
    protected Integer buffersPerRegion;
    @JsonProperty("io-threads")
    protected Integer ioThreads;
    @JsonProperty("worker-threads")
    protected Integer workerThreads;
    @JsonProperty("direct-buffers")
    protected Boolean directBuffers;
    @JsonProperty("target-url")
    protected String targetUrl;
    @JsonProperty("send-access-token")
    protected boolean sendAccessToken;
    @JsonProperty("applications")
    protected List<Application> applications = new LinkedList<Application>();
    @JsonProperty("header-names")
    private Map<String,String> headerNames = new HashMap<>();

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    public Integer getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(Integer httpsPort) {
        this.httpsPort = httpsPort;
    }

    public String getKeystore() {
        return keystore;
    }

    public void setKeystore(String keystore) {
        this.keystore = keystore;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public Integer getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(Integer bufferSize) {
        this.bufferSize = bufferSize;
    }

    public Integer getBuffersPerRegion() {
        return buffersPerRegion;
    }

    public void setBuffersPerRegion(Integer buffersPerRegion) {
        this.buffersPerRegion = buffersPerRegion;
    }

    public Integer getIoThreads() {
        return ioThreads;
    }

    public void setIoThreads(Integer ioThreads) {
        this.ioThreads = ioThreads;
    }

    public Integer getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(Integer workerThreads) {
        this.workerThreads = workerThreads;
    }

    public Boolean getDirectBuffers() {
        return directBuffers;
    }

    public void setDirectBuffers(Boolean directBuffers) {
        this.directBuffers = directBuffers;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public List<Application> getApplications() {
        return applications;
    }

    public void setApplications(List<Application> applications) {
        this.applications = applications;
    }

    public boolean isSendAccessToken() {
        return sendAccessToken;
    }

    public void setSendAccessToken(boolean sendAccessToken) {
        this.sendAccessToken = sendAccessToken;
    }

    public void setHeaderNames(Map<String, String> headerNames) {
        this.headerNames = headerNames;
    }

    public Map<String, String> getHeaderNames() {
        return headerNames;
    }

    public static class Application {
        @JsonProperty("base-path")
        protected String basePath;
        @JsonProperty("adapter-config")
        protected AdapterConfig adapterConfig;
        @JsonProperty("error-page")
        protected String errorPage;
        @JsonProperty("proxy-address-forwarding")
        protected boolean proxyAddressForwarding;
        @JsonProperty("constraints")
        protected List<Constraint> constraints = new LinkedList<Constraint>();

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }

        public AdapterConfig getAdapterConfig() {
            return adapterConfig;
        }

        public void setAdapterConfig(AdapterConfig adapterConfig) {
            this.adapterConfig = adapterConfig;
        }

        public String getErrorPage() {
            return errorPage;
        }

        public void setErrorPage(String errorPage) {
            this.errorPage = errorPage;
        }
		
        public boolean isProxyAddressForwarding() {
            return proxyAddressForwarding;
        }

        public void setProxyAddressForwarding(boolean proxyAddressForwarding) {
            this.proxyAddressForwarding = proxyAddressForwarding;
        }

        public List<Constraint> getConstraints() {
            return constraints;
        }

        public void setConstraints(List<Constraint> constraints) {
            this.constraints = constraints;
        }
    }

    public static class Constraint {
        @JsonProperty("pattern")
        protected String pattern;
        @JsonProperty("roles-allowed")
        protected Set<String> rolesAllowed = new HashSet<String>();
        @JsonProperty("methods")
        protected Set<String> methods = new HashSet<String>();
        @JsonProperty("excluded-methods")
        protected Set<String> excludedMethods = new HashSet<String>();
        @JsonProperty("deny")
        protected boolean deny;
        @JsonProperty("permit")
        protected boolean permit;
        @JsonProperty("authenticate")
        protected boolean authenticate;
        @JsonProperty("permit-and-inject")
        protected boolean permitAndInject;

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public Set<String> getRolesAllowed() {
            return rolesAllowed;
        }

        public void setRolesAllowed(Set<String> rolesAllowed) {
            this.rolesAllowed = rolesAllowed;
        }

        public boolean isDeny() {
            return deny;
        }

        public void setDeny(boolean deny) {
            this.deny = deny;
        }

        public boolean isPermit() {
            return permit;
        }

        public void setPermit(boolean permit) {
            this.permit = permit;
        }

        public boolean isAuthenticate() {
            return authenticate;
        }

        public void setAuthenticate(boolean authenticate) {
            this.authenticate = authenticate;
        }

        public boolean isPermitAndInject() {
            return permitAndInject;
        }

        public void setPermitAndInject(boolean permitAndInject) {
            this.permitAndInject = permitAndInject;
        }

        public Set<String> getMethods() {
            return methods;
        }

        public void setMethods(Set<String> methods) {
            this.methods = methods;
        }

        public Set<String> getExcludedMethods() {
            return excludedMethods;
        }

        public void setExcludedMethods(Set<String> excludedMethods) {
            this.excludedMethods = excludedMethods;
        }
    }
}
