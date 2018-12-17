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
package org.keycloak.testsuite.arquillian;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.testsuite.arquillian.migration.MigrationContext;

import org.keycloak.testsuite.util.TextFileChecker;
import java.util.LinkedList;
import static org.keycloak.testsuite.util.MailServerConfiguration.FROM;
import static org.keycloak.testsuite.util.MailServerConfiguration.HOST;
import static org.keycloak.testsuite.util.MailServerConfiguration.PORT;

/**
 *
 * @author tkyjovsk
 */
public final class SuiteContext {

    private final Set<ContainerInfo> container;

    private List<ContainerInfo> authServerInfo = new LinkedList<>();
    private final List<List<ContainerInfo>> authServerBackendsInfo = new ArrayList<>();

    private final List<ContainerInfo> cacheServersInfo = new ArrayList<>();

    private ContainerInfo migratedAuthServerInfo;
    private final MigrationContext migrationContext = new MigrationContext();

    private boolean adminPasswordUpdated;
    private final Map<String, String> smtpServer = new HashMap<>();

    private TextFileChecker serverLogChecker;

    /**
     * True if the testsuite is running in the adapter backward compatibility testing mode,
     * i.e. if the tests are running against newer auth server
     */
    private static final boolean adapterCompatTesting = Boolean.parseBoolean(System.getProperty("testsuite.adapter.compat.testing"));

    public SuiteContext(Set<ContainerInfo> arquillianContainers) {
        this.container = arquillianContainers;
        this.adminPasswordUpdated = false;
        smtpServer.put("from", FROM);
        smtpServer.put("host", HOST);
        smtpServer.put("port", PORT);
    }

    public TextFileChecker getServerLogChecker() {
        return this.serverLogChecker;
    }

    public void setServerLogChecker(TextFileChecker serverLogChecker) {
        this.serverLogChecker = serverLogChecker;
    }

    public boolean isAdminPasswordUpdated() {
        return adminPasswordUpdated;
    }

    public void setAdminPasswordUpdated(boolean adminPasswordUpdated) {
        this.adminPasswordUpdated = adminPasswordUpdated;
    }

    public Map<String, String> getSmtpServer() {
        return smtpServer;
    }

    public ContainerInfo getAuthServerInfo() {
        return getAuthServerInfo(0);
    }

    public ContainerInfo getAuthServerInfo(int dcIndex) {
        return authServerInfo.get(dcIndex);
    }

    public List<ContainerInfo> getDcAuthServerInfo() {
        return authServerInfo;
    }

    public void setAuthServerInfo(ContainerInfo authServerInfo) {
        this.authServerInfo = new LinkedList<>();
        this.authServerInfo.add(authServerInfo);
    }

    public void addAuthServerInfo(int dcIndex, ContainerInfo serverInfo) {
        while (dcIndex >= authServerInfo.size()) {
            authServerInfo.add(null);
        }
        this.authServerInfo.set(dcIndex, serverInfo);
    }

    public void addCacheServerInfo(int dcIndex, ContainerInfo serverInfo) {
        while (dcIndex >= cacheServersInfo.size()) {
            cacheServersInfo.add(null);
        }
        this.cacheServersInfo.set(dcIndex, serverInfo);
    }

    public List<ContainerInfo> getAuthServerBackendsInfo() {
        return getAuthServerBackendsInfo(0);
    }

    public List<ContainerInfo> getAuthServerBackendsInfo(int dcIndex) {
        return authServerBackendsInfo.get(dcIndex);
    }

    public List<List<ContainerInfo>> getDcAuthServerBackendsInfo() {
        return authServerBackendsInfo;
    }

    public List<ContainerInfo> getCacheServersInfo() {
        return cacheServersInfo;
    }

    public void addAuthServerBackendsInfo(int dcIndex, ContainerInfo container) {
        while (dcIndex >= authServerBackendsInfo.size()) {
            authServerBackendsInfo.add(new LinkedList<>());
        }
        authServerBackendsInfo.get(dcIndex).add(container);
    }

    public ContainerInfo getMigratedAuthServerInfo() {
        return migratedAuthServerInfo;
    }

    public MigrationContext getMigrationContext() {
        return migrationContext;
    }

    public void setMigratedAuthServerInfo(ContainerInfo migratedAuthServerInfo) {
        this.migratedAuthServerInfo = migratedAuthServerInfo;
    }

    public boolean isAuthServerCluster() {
        return ! authServerBackendsInfo.isEmpty();
    }

    public boolean isAuthServerCrossDc() {
        return authServerBackendsInfo.size() > 1;
    }

    public boolean isAuthServerMigrationEnabled() {
        return migratedAuthServerInfo != null;
    }

    public Set<ContainerInfo> getContainers() {
        return container;
    }

    public boolean isAdapterCompatTesting() {
        return adapterCompatTesting;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SUITE CONTEXT:\nAuth server: ");

        if (isAuthServerCrossDc()) {
            for (int i = 0; i < authServerInfo.size(); i ++) {
                ContainerInfo frontend = this.authServerInfo.get(i);
                sb.append("\nFrontend (dc=").append(i).append("): ").append(frontend.getQualifier()).append("\n");
            }

            for (int i = 0; i < authServerBackendsInfo.size(); i ++) {
                int dcIndex = i;
                getDcAuthServerBackendsInfo().get(i).forEach(bInfo -> sb.append("Backend (dc=").append(dcIndex).append("): ").append(bInfo).append("\n"));
            }

            for (int dcIndex=0 ; dcIndex<cacheServersInfo.size() ; dcIndex++) {
                sb.append("CacheServer (dc=").append(dcIndex).append("): ").append(getCacheServersInfo().get(dcIndex)).append("\n");
            }
        } else if (isAuthServerCluster()) {
            sb.append(isAuthServerCluster() ? "\nFrontend: " : "")
              .append(getAuthServerInfo().getQualifier()).append(" - ").append(getAuthServerInfo().getContextRoot().toExternalForm())
              .append("\n");

            getAuthServerBackendsInfo().forEach(bInfo -> sb.append("  Backend: ").append(bInfo).append(" - ").append(bInfo.getContextRoot().toExternalForm()).append("\n"));
        } else {
          sb.append(getAuthServerInfo().getQualifier())
            .append("\n");
        }


        if (isAuthServerMigrationEnabled()) {
            sb.append("Migrated from: ").append(System.getProperty("migrated.auth.server.version")).append("\n");
        }

        if (isAdapterCompatTesting()) {
            sb.append("Adapter backward compatibility testing mode!\n");
        }
        return sb.toString();
    }

}
