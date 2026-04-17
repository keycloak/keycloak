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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;

import org.jgroups.Address;
import org.jgroups.PhysicalAddress;
import org.jgroups.View;
import org.jgroups.annotations.Property;
import org.jgroups.conf.AttributeType;
import org.jgroups.protocols.JDBC_PING2;
import org.jgroups.protocols.PingData;
import org.jgroups.protocols.relay.SiteUUID;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.NameCache;
import org.jgroups.util.UUID;
import org.jgroups.util.Util;

import static java.sql.ResultSet.CONCUR_UPDATABLE;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;

public class KEYCLOAK_JDBC_PING2 extends JDBC_PING2 {

    private JpaConnectionProviderFactory factory;

    @Property(description="Staleness timeout in milliseconds. The coordinator will update the entries once 50%-75% of the time has passed.", type= AttributeType.TIME)
    protected long staleness_timeout = 60000L;

    @Override
    protected void loadDriver() {
        //no-op, using JpaConnectionProviderFactory
    }

    @Override
    protected Connection getConnection() throws SQLException {
        try {
            return factory.getConnection();
        } catch (Exception e) {
            var cause = e.getCause();
            if (cause instanceof SQLException sql) {
                // it should hit this branch 100% of the time
                throw sql;
            }
            //... but to be future proof ...
            throw new SQLException(e);
        }
    }

    @Override
    public void init() throws Exception {
        if (!write_data_on_find) {
            throw new RuntimeException("Running this without write_data_on_find is not safe");
        }
        if (!remove_all_data_on_view_change) {
            throw new RuntimeException("Running this without remove_all_data_on_view_change is not safe");
        }
        super.init();
    }

    protected void insert(Connection connection, PingData data, String clustername) throws SQLException {
        lock.lock();
        try(PreparedStatement ps=connection.prepareStatement(insert_single_sql)) {
            Address address=data.getAddress();
            String addr= Util.addressToString(address);
            String name=address instanceof SiteUUID ? ((SiteUUID)address).getName() : NameCache.get(address);
            PhysicalAddress ip_addr=data.getPhysicalAddr();
            String ip=ip_addr.toString();
            ps.setString(1, addr);
            ps.setString(2, name);
            ps.setString(3, clustername);
            ps.setString(4, ip);
            ps.setBoolean(5, data.isCoord());
            ps.setLong(6, Time.currentTime());
            ps.setString(7, view != null && view.getCoord() != null ? Util.addressToString(view.getCoord()) : null);
            if (log.isTraceEnabled())
                log.trace("%s: SQL for insertion: %s", local_addr, ps);
            ps.executeUpdate();
            log.debug("%s: inserted %s for cluster %s", local_addr, address, clustername);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void handleView(View new_view, View old_view, boolean coord_changed) {
        super.handleView(new_view, old_view, coord_changed);
        if (coord_changed) {
            try {
                removeStaleEntries();
            } catch (Exception e) {
                log.error(String.format("%s: failed handling view change", local_addr), e);
            }
        }
    }

    protected void removeAllNotInCurrentView() {
        View local_view = view;
        if (local_view == null) {
            return;
        }
        String cluster_name = getClusterName();
        try {
            List<PingData> list = readFromDB(getClusterName());
            PingData my_data = list.stream().filter(p -> Objects.equals(p.getAddress(), addr())).findFirst().orElse(null);
            if (my_data == null || my_data.mbrs() == null) {
                return;
            }
            for (PingData data : list) {
                Address addr = data.getAddress();
                // Only delete an entry if it is currently allocated to us, and not someone else
                if (!local_view.containsMember(addr) && my_data.mbrs().contains(addr)) {
                    try (var conn = getConnection()) {
                        addDiscoveryResponseToCaches(addr, data.getLogicalName(), data.getPhysicalAddr());
                        delete(conn, cluster_name, addr);
                    }
                }
            }
        } catch (Exception e) {
            log.error(String.format("%s: failed reading from the DB", local_addr), e);
        }
    }

    /**
     * The infowriter will run on the coordinator only. It will continue to run while this is the coordinator, not only after the view change
     */
    protected synchronized void startInfoWriter() {
        if(info_writer == null || info_writer.isDone())
            info_writer=timer.scheduleWithDynamicInterval(new InfoWriter(info_writer_max_writes_after_view, info_writer_sleep_time) {
                @Override
                public long nextInterval() {
                    return is_coord ? (staleness_timeout / 2 + Util.random(sleep_interval / 4)) : 0;
                }
            });
    }

    protected List<PingData> readFromDB(String cluster) throws Exception {
        try(Connection conn=getConnection();
            PreparedStatement ps=prepare(conn, select_all_pingdata_sql, TYPE_FORWARD_ONLY, CONCUR_UPDATABLE)) {
            ps.setString(1, cluster);
            if(log.isTraceEnabled())
                log.trace("%s: SQL for reading: %s", local_addr, ps);
            try(ResultSet resultSet=ps.executeQuery()) {
                reads++;
                List<PingData> retval=new LinkedList<>();
                Map<Address, Set<Address>> members = new HashMap<>();
                while(resultSet.next()) {
                    String uuid=resultSet.getString(1);
                    String name=resultSet.getString(2);
                    String ip=resultSet.getString(3);
                    boolean coord=resultSet.getBoolean(4);
                    String coordinated_by=resultSet.getString(5);
                    long last_update=resultSet.getLong(6);
                    if (last_update < getStalenessCutoff()) {
                        continue;
                    }
                    Address addr=Util.addressFromString(uuid);
                    IpAddress ip_addr=new IpAddress(ip);
                    PingData data=new PingData(addr, true, name, ip_addr).coord(coord);
                    retval.add(data);
                    if (coordinated_by != null) {
                        Address coordinate_by_address = Util.addressFromString(coordinated_by);
                        members.computeIfAbsent(coordinate_by_address, address -> new HashSet<>())
                                .add(addr);
                    }
                }
                retval.forEach(a -> a.mbrs(members.get(a.getAddress())));
                return retval;
            }
        }
    }

    protected void removeStaleEntries() throws Exception {
        try(Connection conn=getConnection();
            PreparedStatement ps=prepare(conn, select_all_pingdata_sql, TYPE_FORWARD_ONLY, CONCUR_UPDATABLE)) {
            ps.setString(1, getClusterName());
            if(log.isTraceEnabled())
                log.trace("%s: SQL for reading: %s", local_addr, ps);
            try(ResultSet resultSet=ps.executeQuery()) {
                reads++;
                while(resultSet.next()) {
                    String uuid=resultSet.getString(1);
                    long last_update=resultSet.getLong(6);
                    if (last_update < getStalenessCutoff()) {
                        Address addr=Util.addressFromString(uuid);
                        delete(conn, getClusterName(), addr);
                    }
                }
            }
        }
    }

    private long getStalenessCutoff() {
        return TimeUnit.MILLISECONDS.toSeconds(Time.currentTimeMillis() - staleness_timeout);
    }

    public void setJpaConnectionProviderFactory(JpaConnectionProviderFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    // Pick the largest partition first, then order by address to allow for a stable result
    private final static Comparator<PingData> SPLIT_BRAIN_DECIDER = Comparator
            .<PingData, Integer>comparing(p -> p.mbrs() != null ? p.mbrs().size() : 0).reversed()
            .thenComparing(PingData::getAddress);

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
            return readFromDB(cluster_name)
                    .stream()
                    .filter(PingData::isCoord)
                    .sorted(SPLIT_BRAIN_DECIDER)
                    .map(PingData::getAddress)
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
