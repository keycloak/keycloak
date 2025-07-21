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

package org.keycloak.jgroups.protocol;

import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.PhysicalAddress;
import org.jgroups.View;
import org.jgroups.protocols.JDBC_PING2;
import org.jgroups.protocols.PingData;
import org.jgroups.util.NameCache;
import org.jgroups.util.Responses;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Enhanced JDBC_PING2 to handle entries transactionally.
 * <p>
 * Workaround for issue <a href="https://issues.redhat.com/browse/JGRP-2870">JGRP-2870</a>
 */
public class KEYCLOAK_JDBC_PING2 extends JDBC_PING2 {

    private JpaConnectionProviderFactory factory;

    @Override
    protected void handleView(View new_view, View old_view, boolean coord_changed) {
        // If we are the coordinator, it is good to learn about new entries that have been added before we delete them.
        // If we are not the coordinator, it is good to learn the new entries added by the coordinator.
        // This avoids a "JGRP000032: %s: no physical address for %s, dropping message" that leads to split clusters at concurrent startup.
        learnExistingAddresses();
        super.handleView(new_view, old_view, coord_changed);
    }

    protected void learnExistingAddresses() {
        try {
            List<PingData> list = readFromDB(getClusterName());
            for (PingData data : list) {
                Address addr = data.getAddress();
                if (local_addr != null && !local_addr.equals(addr)) {
                    addDiscoveryResponseToCaches(addr, data.getLogicalName(), data.getPhysicalAddr());
                }
            }
        } catch (Exception e) {
            log.error(String.format("%s: failed reading from the DB", local_addr), e);
        }
    }

    @Override
    public void findMembers(List<Address> members, boolean initial_discovery, Responses responses) {
        // Insert ourselves before reading, to ensure that concurrently starting nodes see each other.
        // Also re-add ourselves in case the coordinator removed us.
         PhysicalAddress physical_addr = (PhysicalAddress) down(new Event(Event.GET_PHYSICAL_ADDRESS, local_addr));
        PingData coord_data = new PingData(local_addr, true, NameCache.get(local_addr), physical_addr).coord(is_coord);
        write(Collections.singletonList(coord_data), cluster_name);
        super.findMembers(members, initial_discovery, responses);
    }

    @Override
    protected void writeToDB(PingData data, String clustername) throws SQLException {
        lock.lock();
        try (Connection connection = getConnection()) {
            if(call_insert_sp != null && insert_sp != null)
                callInsertStoredProcedure(connection, data, clustername);
            else {
                boolean isAutocommit = connection.getAutoCommit();
                try {
                    if (isAutocommit) {
                        // Always use a transaction for the delete+insert to make it atomic
                        // to avoid the short moment where there is no entry in the table.
                        connection.setAutoCommit(false);
                    }
                    delete(connection, clustername, data.getAddress());
                    insert(connection, data, clustername);
                    if (isAutocommit) {
                        connection.commit();
                    }
                } catch (SQLException e) {
                    if (isAutocommit) {
                        connection.rollback();
                    }
                    throw e;
                } finally {
                    if (isAutocommit) {
                        connection.setAutoCommit(true);
                    }
                }
            }
        } finally {
            lock.unlock();
        }

    }

    @Override
    protected void loadDriver() {
        //no-op, using JpaConnectionProviderFactory
    }

    @Override
    protected Connection getConnection() {
        return factory.getConnection();
    }

    public void setJpaConnectionProviderFactory(JpaConnectionProviderFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }
}
