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
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;

import org.jgroups.Address;
import org.jgroups.BytesMessage;
import org.jgroups.Event;
import org.jgroups.Message;
import org.jgroups.PhysicalAddress;
import org.jgroups.View;
import org.jgroups.annotations.Property;
import org.jgroups.conf.AttributeType;
import org.jgroups.logging.Log;
import org.jgroups.protocols.JDBC_PING2;
import org.jgroups.protocols.PingData;
import org.jgroups.protocols.PingHeader;
import org.jgroups.protocols.relay.SiteUUID;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.MessageBatch;
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

    /**
     * Epoch milliseconds of the first failed publication of this node's coordinator row, or zero when no failure is outstanding.
     * Waiting {@link #staleness_timeout} before quarantining avoids turning transient write failures into a partition and cache rebalancing.
     * It also lets the last successful publication expire before peers take over: ownership-aware cleanup does not necessarily delete
     * a departed coordinator's row, which can otherwise keep the successor not ready in a two-node tie.
     * Successful reads do not reset this timer, since they do not prove that discovery metadata can be refreshed.
     */
    private volatile long coordinatorWriteFailureSince;
    private volatile boolean quarantined;

    @Property(description="Staleness timeout in milliseconds. The coordinator will update the entries once 50%-75% of the time has passed.", type= AttributeType.TIME)
    protected long staleness_timeout = 60000L;

    public boolean isQuarantined() {
        return quarantined;
    }

    protected void setQuarantined(boolean quarantined) {
        this.quarantined = quarantined;
    }

    @Override
    public Object up(Message message) {
        return quarantined && !Objects.equals(local_addr, message.getSrc()) ? null : super.up(message);
    }

    @Override
    public void up(MessageBatch batch) {
        if (quarantined) {
            batch.removeIf(message -> !Objects.equals(local_addr, message.getSrc()), true);
        }
        if (!batch.isEmpty()) {
            super.up(batch);
        }
    }

    @Override
    public Object down(Message message) {
        if (!quarantined) {
            return super.down(message);
        }
        if (message.getDest() == null) {
            if (message.isFlagSet(Message.TransientFlag.DONT_LOOPBACK)) {
                return null;
            }
            if (message.getSrc() == null) {
                message.setSrc(local_addr);
            }
            // Preserve local protocol progress without sending the multicast to remote peers.
            return up(message);
        }
        return Objects.equals(local_addr, message.getDest()) ? super.down(message) : null;
    }

    // Discovery's response methods otherwise bypass down(Message), including delayed responses.
    @Override
    protected void sendDiscoveryResponse(Address address, PhysicalAddress physicalAddress, String name,
            Address destination, boolean coordinator) {
        PingData data = new PingData(address, is_server, name, physicalAddress).coord(coordinator);
        sendDiscoveryMessage(new BytesMessage(destination)
                .setFlag(Message.Flag.OOB, Message.Flag.DONT_BUNDLE)
                .putHeader(id, new PingHeader(PingHeader.GET_MBRS_RSP))
                .setArray(marshal(data)));
    }

    @Override
    protected void sendDiscoveryResponse(List<PingData> members, Address destination) {
        sendDiscoveryMessage(new BytesMessage(destination)
                .setFlag(Message.Flag.OOB, Message.Flag.DONT_BUNDLE)
                .setFlag(Message.TransientFlag.DONT_BLOCK)
                .putHeader(id, new PingHeader(PingHeader.GET_MBRS_RSP))
                .setArray(marshal(members)));
    }

    private void sendDiscoveryMessage(Message message) {
        if (quarantined) {
            return;
        }
        if (stagger_timeout > 0) {
            int viewSize = view != null ? view.size() : 10;
            int rank = Util.getRank(view, local_addr);
            long delay = rank == 0 ? Util.random(stagger_timeout)
                    : stagger_timeout * rank / viewSize - stagger_timeout / viewSize;
            // Quarantine may start after this response is queued, so recheck it through down(Message) when sending.
            timer.schedule(() -> down(message), delay, TimeUnit.MILLISECONDS, sends_can_block);
        } else {
            down(message);
        }
    }

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

    @Override
    public void start() throws Exception {
        super.start();
        healthCheckTask = timer.scheduleWithFixedDelay(() -> runHealthCheck(log),
                HEALTH_CHECK_INTERVAL_SECONDS, HEALTH_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        Future<?> task = healthCheckTask;
        if (task != null) {
            task.cancel(false);
            healthCheckTask = null;
        }
        setQuarantined(false);
        coordinatorWriteFailureSince = 0;
        super.stop();
    }

    protected void runHealthCheck(Log logger) {
        if (view == null) {
            return;
        }
        boolean wasQuarantined = quarantined;
        HealthStatus status = checkQuarantine(queryHealthStatus(), logger);
        boolean statusChanged = status != previousHealthStatus;
        if (statusChanged) {
            previousHealthStatus = status;
            cyclesSinceLastLog = 0;
        }

        if (status == HealthStatus.HEALTHY) {
            if (statusChanged) {
                // Quarantine recovery already completed cache cleanup before reopening traffic.
                if (!wasQuarantined && !notifyHealthRestored(logger)) {
                    previousHealthStatus = HealthStatus.ERROR;
                    return;
                }
                logger.info("Cluster health restored for cluster '%s'.", cluster_name);
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
                case NO_COORDINATOR -> logger.warn("No coordinator found in the database for cluster '%s'. "
                        + "This is most likely transient and will resolve once a coordinator is elected.", cluster_name);
                case ERROR -> {
                    if (quarantined) {
                        logger.warn("Cluster traffic for '%s' is isolated after a sustained discovery database failure.",
                                cluster_name);
                    } else {
                        logger.warn("Failed to determine cluster health for cluster '%s' from the database. "
                                + "This may indicate a database connectivity issue.", cluster_name);
                    }
                }
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

    /**
     * Suppresses publication by the still-running InfoWriter during quarantine. A recovered database must not let
     * this unreachable coordinator advertise itself or overwrite peers' discovery rows using its old view.
     */
    @Override
    protected void writeToDB(PingData data, String cluster) throws SQLException {
        boolean coordinatorPublication = data.isCoord() && Objects.equals(data.getAddress(), local_addr);
        // Finish any in-flight publication before isolation, and keep its failure tracking atomic with that decision.
        lock.lock();
        try {
            if (quarantined) {
                log.debug("Skipping discovery publication while this node is isolated.");
                return;
            }
            super.writeToDB(data, cluster);
            if (coordinatorPublication) {
                coordinatorWriteFailureSince = 0;
            }
        } catch (SQLException e) {
            if (coordinatorPublication && coordinatorWriteFailureSince == 0) {
                coordinatorWriteFailureSince = Time.currentTimeMillis();
            }
            throw e;
        } finally {
            lock.unlock();
        }
    }

    private HealthStatus checkQuarantine(HealthStatus status, Log logger) {
        lock.lock();
        try {
            boolean sustainedFailure = coordinatorWriteFailureSince != 0
                    && Time.currentTimeMillis() - coordinatorWriteFailureSince >= staleness_timeout;
            if (!quarantined && is_coord && view.size() > 1 && sustainedFailure) {
                logger.warn("Coordinator cannot refresh discovery metadata for cluster '%s'. "
                        + "Isolating cluster traffic so a database-connected member can take over.", cluster_name);
                setQuarantined(true);
            }
        } finally {
            lock.unlock();
        }
        if (!quarantined) {
            return status;
        }
        // Even if the DB returns early, finish removing peers from our local view. Reopening traffic could let
        // heartbeats preserve the old view after peers have elected a successor, allowing our publications to
        // overwrite discovery rows for members now belonging to that other partition.
        if (view.size() != 1 || status == HealthStatus.ERROR) {
            return HealthStatus.ERROR;
        }
        PhysicalAddress physicalAddress = (PhysicalAddress) down(new Event(Event.GET_PHYSICAL_ADDRESS, local_addr));
        try {
            // Verify writes and republish our physical address for merge discovery, without advertising a coordinator
            // before cache cleanup.
            super.writeToDB(new PingData(local_addr, true, NameCache.get(local_addr), physicalAddress).coord(false),
                    cluster_name);
        } catch (SQLException e) {
            logger.warn("Discovery database is readable but cannot be written; keeping this node isolated.", e);
            return HealthStatus.ERROR;
        }
        // Clear potentially stale values while traffic and readiness are still blocked; failed cleanup must be retried.
        if (!notifyHealthRestored(logger)) {
            return HealthStatus.ERROR;
        }
        coordinatorWriteFailureSince = 0;
        setQuarantined(false);
        writeAll();
        logger.info("Database access and local caches restored for cluster '%s'. Resuming normal cluster traffic.",
                cluster_name);
        return queryHealthStatus();
    }

    private boolean notifyHealthRestored(Log logger) {
        Runnable callback = onHealthRestored;
        if (callback != null) {
            try {
                callback.run();
            } catch (Throwable t) {
                logger.error("onHealthRestored callback failed", t);
                return false;
            }
        }
        return true;
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
     *     <li>If multiple coordinators are found, it selects the largest partition, breaking ties by the lowest coordinator {@link UUID}</li>
     *     <li>Finally, it compares if the coordinator is the same as the current view coordinator. If so, it returns {@link HealthStatus#HEALTHY}, otherwise {@link HealthStatus#UNHEALTHY}</li>
     * </ul>
     *
     * @return The {@link HealthStatus}.
     *         Quarantined nodes also return {@link HealthStatus#ERROR}.
     * @see HealthStatus
     */
    public HealthStatus healthStatus() {
        return quarantined ? HealthStatus.ERROR : queryHealthStatus();
    }

    protected HealthStatus queryHealthStatus() {
        try {
            HealthStatus status = readFromDB(cluster_name)
                    .stream()
                    .filter(PingData::isCoord)
                    .sorted(SPLIT_BRAIN_DECIDER)
                    .map(PingData::getAddress)
                    .findFirst()
                    .map(view.getCoord()::equals)
                    .map(isCoordinatorInView -> isCoordinatorInView ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY)
                    .orElse(HealthStatus.NO_COORDINATOR);

            if (status == HealthStatus.HEALTHY && !allow_multiple_clusters && hasOtherClusters()) {
                return HealthStatus.MULTIPLE_CLUSTERS;
            }
            return status;
        } catch (Exception e) {
            return HealthStatus.ERROR;
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
         * No coordinator present in the database table.
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
