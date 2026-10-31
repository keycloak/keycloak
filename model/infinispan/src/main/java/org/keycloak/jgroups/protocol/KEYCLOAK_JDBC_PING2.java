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
import java.sql.SQLFeatureNotSupportedException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;

import org.jgroups.Address;
import org.jgroups.PhysicalAddress;
import org.jgroups.View;
import org.jgroups.annotations.Property;
import org.jgroups.conf.AttributeType;
import org.jgroups.logging.Log;
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

    private static final int HEALTH_CHECK_INTERVAL_SECONDS = 10;
    private static final int HEALTH_CHECK_REPEAT_LOG_CYCLES = 180; // re-log after 180 * 10s = 30 minutes

    @Property(description = "SQL to count entries in the JDBC_PING table belonging to cluster names other than the current one. "
            + "Used to detect multiple clusters sharing the same database without the stateless feature.")
    protected String select_other_clusters_count_sql =
            "SELECT COUNT(*) FROM jgroups WHERE cluster != ? AND last_update >= ?";

    @Property(description = "Whether multiple cluster names in the JDBC_PING table are permitted. "
            + "Set to true when the stateless feature is enabled, as it provides the cross-cluster cache invalidation "
            + "mechanism that makes multi-cluster setups safe.")
    protected boolean allow_multiple_clusters = false;

    private static final Executor NETWORK_TIMEOUT_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "jdbc-ping-network-timeout");
        t.setDaemon(true);
        return t;
    });

    private JpaConnectionProviderFactory factory;
    private volatile HealthStatus previousHealthStatus = HealthStatus.HEALTHY;
    private volatile int cyclesSinceLastLog = 0;
    private volatile Future<?> healthCheckTask;
    private volatile Runnable onHealthRestored;
    private final ReentrantLock databaseTaskOwnershipLock = new ReentrantLock();
    private final AtomicReference<DatabaseTaskOwnership> databaseTaskOwnership =
            new AtomicReference<>(new DatabaseTaskOwnership(null, 0, false));

    private record DatabaseTaskOwnership(View view, long expiresAt, boolean coordinator) {
        boolean isValid(View currentView) {
            return view == currentView && Time.currentTimeMillis() < expiresAt;
        }
    }

    @Property(description="Staleness timeout in milliseconds. Each member periodically refreshes its own entry before it expires.", type= AttributeType.TIME)
    protected long staleness_timeout = 60000L;

    @Override
    protected void loadDriver() {
        //no-op, using JpaConnectionProviderFactory
    }

    @Override
    protected Connection getConnection() throws SQLException {
        Connection connection = null;
        try {
            connection = factory.getConnection();
            try {
                connection.setNetworkTimeout(NETWORK_TIMEOUT_EXECUTOR, (int) (staleness_timeout / 3));
            } catch (SQLFeatureNotSupportedException e) {
                log.warn("JDBC driver does not support setNetworkTimeout. " +
                        "Health check queries may hang during database outages.");
            }
            return connection;
        } catch (Exception e) {
            if (connection != null) {
                try { connection.close(); } catch (SQLException ignored) {}
            }
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
        invalidateDatabaseTaskOwnership();
        super.handleView(new_view, old_view, coord_changed);
        if (!is_coord) {
            // In contrast to the parent original method, we now need to write our own entry even if we are not the coordinator,
            // because the coordinator may not be able to access the database.
            writeLocalAddress();
        }
        // info-writer is started on all nodes, as the coordinator may not be able to access the database and we need to keep our own entry fresh.
        startInfoWriter();
        if (is_coord && coord_changed) {
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

    @Override
    protected void writeAll(Address[] excluded_mbrs) {
        // A fresh row must reflect this member's database access and view, not another member's publication.
        writeLocalAddress();
    }

    /**
     * Keeps every member's own entry fresh for as long as it participates in the cluster.
     * The inherited writer stops after a bounded number of writes; readiness needs continuous
     * publications, including from non-coordinators when their coordinator cannot access the database.
     * Uses the same lock as {@link #stopInfoWriter()} to protect the task reference without monitor-based locking.
     */
    @Override
    protected void startInfoWriter() {
        lock.lock();
        try {
            if(info_writer == null || info_writer.isDone())
                info_writer=timer.scheduleWithDynamicInterval(new InfoWriter(info_writer_max_writes_after_view, info_writer_sleep_time) {
                    @Override
                    public long nextInterval() {
                        return staleness_timeout / 2 + Util.random(sleep_interval / 4);
                    }
                });
        } finally {
            lock.unlock();
        }
    }

    /**
     * Cancels publication using the same lock as {@link #startInfoWriter()}.
     * The inherited method uses a different, monitor-based lock and would not serialize access to the task reference.
     */
    @Override
    protected void stopInfoWriter() {
        lock.lock();
        try {
            if (info_writer != null) {
                info_writer.cancel(false);
                info_writer = null;
            }
        } finally {
            lock.unlock();
        }
    }

    protected List<PingData> readFromDB(String cluster) throws Exception {
        return readDiscoveryData(cluster).entries();
    }

    protected record DiscoveryData(List<PingData> entries, Map<Address, Set<Address>> partitions) {
        Optional<Map.Entry<Address, Set<Address>>> winningPartition() {
            return partitions.entrySet().stream().min(SPLIT_BRAIN_DECIDER);
        }
    }

    protected DiscoveryData readDiscoveryData(String cluster) throws Exception {
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
                return new DiscoveryData(retval, members);
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

    @Override
    public void start() throws Exception {
        invalidateDatabaseTaskOwnership();
        super.start();
        healthCheckTask = timer.scheduleWithFixedDelay(() -> runHealthCheck(log),
                HEALTH_CHECK_INTERVAL_SECONDS, HEALTH_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        invalidateDatabaseTaskOwnership();
        Future<?> task = healthCheckTask;
        if (task != null) {
            task.cancel(false);
            healthCheckTask = null;
        }
        super.stop();
    }

    protected void runHealthCheck(Log logger) {
        if (view == null) {
            return;
        }
        HealthStatus status = healthStatus();
        boolean statusChanged = status != previousHealthStatus;
        if (statusChanged) {
            previousHealthStatus = status;
            cyclesSinceLastLog = 0;
        }

        if (status == HealthStatus.HEALTHY) {
            if (statusChanged) {
                logger.info("Cluster health restored for cluster '%s'.", cluster_name);
                Runnable callback = onHealthRestored;
                if (callback != null) {
                    try {
                        callback.run();
                    } catch (Throwable t) {
                        logger.error("onHealthRestored callback failed", t);
                    }
                }
            }
            return;
        }

        // For non-healthy states: log on transition and then every HEALTH_CHECK_REPEAT_LOG_CYCLES.
        if (statusChanged || cyclesSinceLastLog >= HEALTH_CHECK_REPEAT_LOG_CYCLES) {
            cyclesSinceLastLog = 0;
            switch (status) {
                case UNHEALTHY -> logger.error("Split-brain detected for cluster '%s'. This node (%s) is in the losing "
                        + "partition and should not be serving requests. Possible causes: (1) nodes within this "
                        + "cluster cannot reach each other over the network — check network connectivity and "
                        + "firewall rules; (2) two separate Keycloak deployments share the same database but use "
                        + "the same cluster name — each deployment must be assigned a distinct cluster name via "
                        + "'cache-embedded-cluster-name'.",
                        cluster_name, local_addr);
                case NO_COORDINATOR -> logger.warn("No fresh member entry reports a coordinator for cluster '%s'. "
                        + "This may be transient while members publish their cluster view.", cluster_name);
                case ERROR -> logger.warn("Failed to determine cluster health for cluster '%s' from the database. "
                        + "This may indicate a database connectivity issue.",
                        cluster_name);
                case MULTIPLE_CLUSTERS -> logger.error("Multiple cluster names detected in the JDBC_PING table "
                        + "while the stateless feature is not enabled. "
                        + "Cross-cluster cache invalidation requires the stateless feature to be enabled. "
                        + "Either enable the stateless feature or ensure all nodes use the same cluster name "
                        + "via 'cache-embedded-cluster-name'.");
            }
        } else {
            cyclesSinceLastLog++;
        }
    }

    public void setJpaConnectionProviderFactory(JpaConnectionProviderFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    public Runnable getOnHealthRestored() {
        return onHealthRestored;
    }

    public void setOnHealthRestored(Runnable onHealthRestored) {
        this.onHealthRestored = onHealthRestored;
    }

    // Count fresh member publications, even when their coordinator's own row has expired.
    private static final Comparator<Map.Entry<Address, Set<Address>>> SPLIT_BRAIN_DECIDER = Comparator
            .<Map.Entry<Address, Set<Address>>>comparingInt(partition -> partition.getValue().size()).reversed()
            .thenComparing(Map.Entry::getKey);

    /**
     * Detects a network partition and decides if the node belongs to the winning partition.
     * <p>
     * The algorithm performs the following steps
     *
     * <ul>
     *     <li>Reads the data from the database</li>
     *     <li>If an error occurs fetching the data, it returns {@link HealthStatus#ERROR}</li>
     *     <li>Groups fresh member entries by their reported coordinator</li>
     *     <li>If no member reports a coordinator, it returns {@link HealthStatus#NO_COORDINATOR}</li>
     *     <li>Chooses the partition with the most fresh entries, breaking ties by the lowest coordinator {@link UUID}</li>
     *     <li>Finally, it compares if the coordinator is the same as the current view coordinator. If so, it returns {@link HealthStatus#HEALTHY}, otherwise {@link HealthStatus#UNHEALTHY}</li>
     * </ul>
     *
     * @return The {@link HealthStatus}.
     * @see HealthStatus
     */
    public HealthStatus healthStatus() {
        try {
            return readDiscoveryData(cluster_name).winningPartition()
                    .map(partition -> healthStatus(partition.getKey()))
                    .orElse(HealthStatus.NO_COORDINATOR);
        } catch (Exception e) {
            return HealthStatus.ERROR;
        }
    }

    private HealthStatus healthStatus(Address coordinator) {
        if (!view.getCoord().equals(coordinator)) {
            return HealthStatus.UNHEALTHY;
        }
        return !allow_multiple_clusters && hasOtherClusters() ? HealthStatus.MULTIPLE_CLUSTERS : HealthStatus.HEALTHY;
    }

    /**
     * Selects a database-task owner only in the partition accepted by the readiness check.
     * Prefers its coordinator while its own publication is fresh, otherwise the lowest-address fresh member.
     * This is not an exclusive lease: publication expiry and concurrent snapshots can cause overlapping workers.
     * Decisions, including failed queries, are cached for the health-check interval.
     */
    public boolean isDatabaseTaskCoordinator() {
        var cached = databaseTaskOwnership.get();
        if (cached.isValid(view)) {
            return cached.coordinator();
        }
        databaseTaskOwnershipLock.lock();
        try {
            cached = databaseTaskOwnership.get();
            View currentView = view;
            if (cached.isValid(currentView)) {
                return cached.coordinator();
            }
            if (currentView == null) {
                return false;
            }
            boolean coordinator = queryDatabaseTaskCoordinator();
            var refreshed = new DatabaseTaskOwnership(currentView,
                    Time.currentTimeMillis() + TimeUnit.SECONDS.toMillis(HEALTH_CHECK_INTERVAL_SECONDS), coordinator);
            // Do not restore a decision invalidated by a concurrent view or lifecycle change.
            return databaseTaskOwnership.compareAndSet(cached, refreshed) && view == currentView && coordinator;
        } finally {
            databaseTaskOwnershipLock.unlock();
        }
    }

    private void invalidateDatabaseTaskOwnership() {
        databaseTaskOwnership.set(new DatabaseTaskOwnership(null, 0, false));
    }

    private boolean queryDatabaseTaskCoordinator() {
        try {
            var winner = readDiscoveryData(cluster_name).winningPartition();
            if (winner.isEmpty()) {
                return false;
            }
            var partition = winner.get();
            if (healthStatus(partition.getKey()) != HealthStatus.HEALTHY) {
                return false;
            }
            Address coordinator = partition.getKey();
            Address owner = partition.getValue().contains(coordinator) ? coordinator
                    : partition.getValue().stream().min(Address::compareTo).orElseThrow();
            return owner.equals(local_addr);
        } catch (Exception e) {
            log.warn(String.format("Failed to determine database task ownership for cluster '%s'.", cluster_name), e);
            return false;
        }
    }

    private boolean hasOtherClusters() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(select_other_clusters_count_sql)) {
            ps.setString(1, cluster_name);
            ps.setLong(2, getStalenessCutoff());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            log.warn("Failed to check for other cluster names in the database.", e);
            return false;
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
         * No fresh member entry reports a coordinator in the database table.
         */
        NO_COORDINATOR,
        /**
         * If an error occurs when reading from the database.
         */
        ERROR,
        /**
         * Multiple cluster names detected in the database table while the stateless feature is not enabled.
         * Cross-cluster cache invalidation requires the stateless feature.
         */
        MULTIPLE_CLUSTERS
    }
}
