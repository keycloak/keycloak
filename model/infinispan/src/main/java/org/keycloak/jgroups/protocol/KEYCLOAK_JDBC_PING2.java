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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.keycloak.connections.jpa.JpaConnectionProviderFactory;

import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.PhysicalAddress;
import org.jgroups.View;
import org.jgroups.protocols.JDBC_PING2;
import org.jgroups.protocols.PingData;
import org.jgroups.stack.Protocol;
import org.jgroups.util.ExtendedUUID;
import org.jgroups.util.NameCache;
import org.jgroups.util.Responses;
import org.jgroups.util.UUID;

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

        // This is an updated logic where we do not call removeAll but instead remove those obsolete entries.
        // This avoids the short moment where the table is empty and a new node might not see any other node.
        if (is_coord) {
            if (remove_old_coords_on_view_change) {
                Address old_coord = old_view != null ? old_view.getCreator() : null;
                if (old_coord != null)
                    remove(cluster_name, old_coord);
            }
            Address[] left = View.diff(old_view, new_view)[1];
            if (coord_changed || update_store_on_view_change || left.length > 0) {
                writeAll(left);
                if (remove_all_data_on_view_change) {
                    removeAllNotInCurrentView();
                }
                if (remove_all_data_on_view_change || remove_old_coords_on_view_change) {
                    startInfoWriter();
                }
            }
        } else if (coord_changed && !remove_all_data_on_view_change) {
            // I'm no longer the coordinator, usually due to a merge.
            // The new coordinator will update my status to non-coordinator, and remove me fully
            // if 'remove_all_data_on_view_change' is enabled and I'm no longer part of the view.
            // Maybe this branch even be removed completely, but for JDBC_PING 'remove_all_data_on_view_change' is always set to true.
            PhysicalAddress physical_addr = (PhysicalAddress) down(new Event(Event.GET_PHYSICAL_ADDRESS, local_addr));
            PingData coord_data = new PingData(local_addr, true, NameCache.get(local_addr), physical_addr).coord(is_coord);
            write(Collections.singletonList(coord_data), cluster_name);
        }
    }

    @Override
    protected void removeAll(String clustername) {
        // This is unsafe as even if we would fill the table a moment later, a new node might see an empty table and become a coordinator
        throw new RuntimeException("Not implemented as it is unsafe");
    }

    private void removeAllNotInCurrentView() {
        try {
            List<PingData> list = readFromDB(getClusterName());
            for (PingData data : list) {
                Address addr = data.getAddress();
                if (view != null && !view.containsMember(addr)) {
                    addDiscoveryResponseToCaches(addr, data.getLogicalName(), data.getPhysicalAddr());
                    remove(cluster_name, addr);
                }
            }
        } catch (Exception e) {
            log.error(String.format("%s: failed reading from the DB", local_addr), e);
        }
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
    public synchronized boolean isInfoWriterRunning() {
        // Do not rely on the InfoWriter, instead always write the missing information on find if it is missing. Find is also triggered by MERGE.
        return false;
    }

    @Override
    public void findMembers(List<Address> members, boolean initial_discovery, Responses responses) {
        if (initial_discovery) {
            try {
                List<PingData> pingData = readFromDB(cluster_name);
                PhysicalAddress physical_addr = (PhysicalAddress) down(new Event(Event.GET_PHYSICAL_ADDRESS, local_addr));
                // Sending the discovery here, as parent class will not execute it once there is data in the table
                sendDiscoveryResponse(local_addr, physical_addr, NameCache.get(local_addr), null, is_coord);
                PingData coord_data = new PingData(local_addr, true, NameCache.get(local_addr), physical_addr).coord(is_coord);
                write(Collections.singletonList(coord_data), cluster_name);
                while (pingData.stream().noneMatch(PingData::isCoord)) {
                    // Do a quick check if more nodes have arrived, to have a more complete list of nodes to start with.
                    List<PingData> newPingData = readFromDB(cluster_name);
                    if (newPingData.stream().map(PingData::getAddress).collect(Collectors.toSet()).equals(pingData.stream().map(PingData::getAddress).collect(Collectors.toSet()))
                            || pingData.stream().anyMatch(PingData::isCoord)) {
                        break;
                    }
                    pingData = newPingData;
                }
            } catch (Exception e) {
                log.error(String.format("%s: failed reading from the DB", local_addr), e);
            }
        }

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
                    } else {
                        log.warn("Autocommit is disabled. This indicates a transaction context that might batch statements and can lead to deadlocks.");
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

    /* START: JDBC_PING2 does not handle ExtendedUUID yet, see
       https://github.com/belaban/JGroups/pull/901 - until this is backported, we convert all of them.
     */

    @Override
    public <T extends Protocol> T addr(Address addr) {
        addr = toUUID(addr);
        return super.addr(addr);
    }

    @Override
    public <T extends Protocol> T setAddress(Address addr) {
        addr = toUUID(addr);
        return super.setAddress(addr);
    }

    @Override
    protected void delete(Connection conn, String clustername, Address addressToDelete) throws SQLException {
        super.delete(conn, clustername, toUUID(addressToDelete));
    }

    @Override
    protected void delete(String clustername, Address addressToDelete) throws SQLException {
        super.delete(clustername, toUUID(addressToDelete));
    }

    @Override
    protected void insert(Connection connection, PingData data, String clustername) throws SQLException {
        if (data.getAddress() instanceof ExtendedUUID) {
            data = new PingData(toUUID(data.getAddress()), data.isServer(), data.getLogicalName(), data.getPhysicalAddr()).coord(data.isCoord());
        }
        super.insert(connection, data, clustername);
    }

    private static Address toUUID(Address addr) {
        if (addr instanceof ExtendedUUID eUUID) {
            addr = new UUID(eUUID.getMostSignificantBits(), eUUID.getLeastSignificantBits());
        }
        return addr;
    }

    /* END: JDBC_PING2 does not handle ExtendedUUID yet, see
       https://github.com/belaban/JGroups/pull/901 - until this is backported, we convert all of them.
    */

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

    /**
     * Detects a network partition and decides if the node belongs to the winning partition.
     * <p>
     * The algorithm performs the following steps
     *
     * <ul>
     *     <li>Reads the data from the database</li>
     *     <li>If an error occurs fetching the data, it returns {@link HealthStatus#ERROR}</li>
     *     <li>Filters out non coordinator members</li>
     *     <li>If no coordinator is found, it return {@link HealthStatus#NO_COORDINATOR}</li>
     *     <li>If multiple coordinators are found, it compares them and uses the coordinator with the lowest {@link UUID}</li>
     *     <li>Finally, it compares if the coordinator is the same as the current view coordinator. If so, it returns {@link HealthStatus#HEALTHY}, otherwise {@link HealthStatus#UNHEALTHY}</li>
     * </ul>
     *
     * @return The {@link HealthStatus}.
     * @see HealthStatus
     */
    public HealthStatus healthStatus() {
        try {
            // maybe create an index, and a query to return coordinators only?
            return readFromDB(cluster_name)
                    .stream()
                    .filter(PingData::isCoord)
                    .map(PingData::getAddress)
                    .sorted()
                    .findFirst()
                    .map(view.getCoord()::equals)
                    .map(isCoordinatorInView -> isCoordinatorInView ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY)
                    .orElse(HealthStatus.NO_COORDINATOR);
        } catch (Exception e) {
            // database failed?
            log.warn("Failed to fetch the cluster members from the database.", e);
            return HealthStatus.ERROR;
        }
    }

    public enum HealthStatus {
        /**
         * No partition detected or this instance is in the right partition.
         */
        HEALTHY,
        /**
         * Partition detected and this instance is not in the right partition. It should stop handling requests.
         */
        UNHEALTHY,
        /**
         * No coordinator present in the database table.
         */
        NO_COORDINATOR,
        /**
         * If an error occurs when reading from the database.
         */
        ERROR
    }
}
