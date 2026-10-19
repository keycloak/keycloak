package org.keycloak.connections.jpa.updater.liquibase.custom;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.RawParameterizedSqlStatement;

/**
 * Back-fills the BIGINT epoch-millisecond columns of {@code OUTBOX_ENTRY}
 * from the TIMESTAMP columns they replace.
 *
 * <p>The old columns were written by Hibernate's UTC timestamp binding,
 * so they are read back with a UTC calendar. Deployments whose database
 * session time zone differed when the rows were written may see a
 * constant offset; to make that harmless, the next attempt of every
 * queued row is clamped to "now" so no row ends up scheduled hours into
 * the future. The outbox is at-least-once, so an early retry is safe.
 */
public class JpaUpdate27_0_0_OutboxTimestampsToEpochMillis extends CustomKeycloakTask {

    private static final Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String table = getTableName("OUTBOX_ENTRY");
        long now = System.currentTimeMillis();
        String select = "SELECT ID, STATUS, CREATED_AT, NEXT_ATTEMPT_AT, DELIVERED_AT FROM " + table;
        String update = "UPDATE " + table
                + " SET CREATED_AT_MS = ?, NEXT_ATTEMPT_AT_MS = ?, DELIVERED_AT_MS = ? WHERE ID = ?";
        int rows = 0;
        try (PreparedStatement ps = connection.prepareStatement(select);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString(1);
                String status = rs.getString(2);
                Long createdAt = millis(rs.getTimestamp(3, UTC));
                Long nextAttemptAt = millis(rs.getTimestamp(4, UTC));
                Long deliveredAt = millis(rs.getTimestamp(5, UTC));
                if (createdAt == null) {
                    createdAt = now;
                }
                boolean queued = "PENDING".equals(status) || "HELD".equals(status);
                if (nextAttemptAt == null || (queued && nextAttemptAt > now)) {
                    nextAttemptAt = now;
                }
                statements.add(new RawParameterizedSqlStatement(update, createdAt, nextAttemptAt, deliveredAt, id));
                rows++;
            }
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": failed to read OUTBOX_ENTRY timestamps", e);
        }
        confirmationMessage.append("Converted timestamps of ").append(rows).append(" OUTBOX_ENTRY row(s) to epoch milliseconds");
    }

    private static Long millis(Timestamp ts) {
        return ts == null ? null : ts.getTime();
    }

    @Override
    protected String getTaskId() {
        return "Convert OUTBOX_ENTRY timestamps to epoch milliseconds";
    }
}
