/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.storage.infinispan.jgroups.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.remoting.transport.jgroups.EmbeddedJGroupsChannelConfigurator;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.conf.ProtocolConfiguration;
import org.jgroups.protocols.JDBC_PING2;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.storage.infinispan.CacheManagerFactory;
import org.keycloak.quarkus.runtime.storage.infinispan.jgroups.JGroupsStackConfigurator;
import org.keycloak.quarkus.runtime.storage.infinispan.jgroups.JGroupsUtil;

import javax.sql.DataSource;

/**
 * JGroups discovery configuration using {@link JDBC_PING2}.
 */
public class JGroupsJdbcPingStackConfigurator implements JGroupsStackConfigurator {

    public static final JGroupsStackConfigurator INSTANCE = new JGroupsJdbcPingStackConfigurator();

    private JGroupsJdbcPingStackConfigurator() {}

    @Override
    public boolean requiresKeycloakSession() {
        return true;
    }

    @Override
    public void configure(ConfigurationBuilderHolder holder, KeycloakSession session) {
        var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        var stackName = JGroupsUtil.transportStackOf(holder).get();
        var isUdp = stackName.endsWith("udp");
        var tableName = JpaUtils.getTableNameForNativeQuery("JGROUPS_PING", em);
        var stack = getProtocolConfigurations(tableName, isUdp ? "PING" : "MPING");
        holder.addJGroupsStack(new EmbeddedJGroupsChannelConfigurator(stackName, stack, null), isUdp ? "udp" : "tcp");

        Supplier<DataSource> dataSourceSupplier = Arc.container().select(AgroalDataSource.class)::get;
        JGroupsUtil.transportOf(holder).addProperty(JGroupsTransport.DATA_SOURCE, dataSourceSupplier);
        JGroupsUtil.transportOf(holder).stack(stackName);
        CacheManagerFactory.logger.info("JGroups JDBC_PING discovery enabled.");
    }

    private static List<ProtocolConfiguration> getProtocolConfigurations(String tableName, String discoveryProtocol) {
        var attributes = Map.of(
                // Leave initialize_sql blank as table is already created by Keycloak
                "initialize_sql", "",
                // Explicitly specify clear and select_all SQL to ensure "cluster_name" column is used, as the default
                // "cluster" cannot be used with Oracle DB as it's a reserved word.
                "clear_sql", String.format("DELETE from %s WHERE cluster_name=?", tableName),
                "delete_single_sql", String.format("DELETE from %s WHERE address=?", tableName),
                "insert_single_sql", String.format("INSERT INTO %s values (?, ?, ?, ?, ?)", tableName),
                "select_all_pingdata_sql", String.format("SELECT address, name, ip, coord FROM %s WHERE cluster_name=?", tableName),
                "remove_all_data_on_view_change", "true",
                "register_shutdown_hook", "false",
                "stack.combine", "REPLACE",
                "stack.position", discoveryProtocol
        );

        // Use custom Keycloak JDBC_PING implementation that workarounds issue https://issues.redhat.com/browse/JGRP-2870
        // The id 1025 follows this instruction: https://github.com/belaban/JGroups/blob/38219e9ec1c629fa2f7929e3b53d1417d8e60b61/conf/jg-protocol-ids.xml#L85
        ClassConfigurator.addProtocol((short) 1025, KEYCLOAK_JDBC_PING2.class);
        return List.of(new ProtocolConfiguration(KEYCLOAK_JDBC_PING2.class.getName(), attributes));
    }


}
