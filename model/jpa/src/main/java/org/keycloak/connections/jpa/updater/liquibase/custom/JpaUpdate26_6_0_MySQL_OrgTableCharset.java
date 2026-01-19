package org.keycloak.connections.jpa.updater.liquibase.custom;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import liquibase.exception.CustomChangeException;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.statement.core.RawSqlStatement;
import liquibase.structure.core.Table;

public class JpaUpdate26_6_0_MySQL_OrgTableCharset extends CustomKeycloakTask {

    @Override
    protected boolean isApplicable() throws CustomChangeException {
        try {
            // Correct the table name based on the database dialect (quotes, case-sensitivity)
            String correctedTableName = database.correctObjectName("ORG", Table.class);

            // Use the Liquibase Snapshot API to check if the table actually exists in the schema
            if (!SnapshotGeneratorFactory.getInstance().has(new Table().setName(correctedTableName), database)) {
                return false;
            }
        } catch (Exception e) {
            throw new CustomChangeException("Failed to check for existence of ORG table", e);
        }

        String rawSchema = database.getDefaultSchemaName();
        String schemaCondition = (rawSchema == null) ? "DATABASE()" : "'" + rawSchema + "'";

        String rawOrg = database.correctObjectName("ORG", Table.class);
        String rawOrgDomain = database.correctObjectName("ORG_DOMAIN", Table.class);
        String rawOrgInvitation = database.correctObjectName("ORG_INVITATION", Table.class);

        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = " + schemaCondition +
                " AND (" +
                "  (TABLE_NAME = '" + rawOrg + "' AND COLUMN_NAME = 'ID') OR " +
                "  (TABLE_NAME = '" + rawOrgDomain + "' AND COLUMN_NAME = 'ORG_ID') OR " +
                "  (TABLE_NAME = '" + rawOrgInvitation + "' AND COLUMN_NAME = 'ORGANIZATION_ID')" +
                ")" +
                " AND (" +
                "  CHARACTER_SET_NAME <> 'utf8mb3' OR " +
                "  COLLATION_NAME <> 'utf8mb3_unicode_ci'" +
                ")";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                // If the count is > 0, at least one column needs fixing
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            // If we get an "Access Denied" (Code 1142) or similar metadata error
            if (e.getErrorCode() == 1142 || e.getMessage().contains("information_schema")) {
                // By returning true, we ensure the fix runs anyway
                // this is the safest path for the user.
                return true;
            }
            throw new CustomChangeException("Failed to check ORG table metadata", e);
        }
    }

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        // disable checks to allow conversion of columns already in FK relationships
        statements.add(new RawSqlStatement("SET FOREIGN_KEY_CHECKS=0"));

        // converge the whole "Organization Family"
        String[] tablesToFix = {"ORG", "ORG_DOMAIN", "ORG_INVITATION"};
        for (String table : tablesToFix) {
            String fullName = getTableName(table);
            statements.add(new RawSqlStatement(
                    "ALTER TABLE " + fullName + " CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci"
            ));
        }

        statements.add(new RawSqlStatement("SET FOREIGN_KEY_CHECKS=1"));
        confirmationMessage.append("Converged ORG family metadata to utf8mb3_unicode_ci.");
    }

    @Override
    protected String getTaskId() { return "Converge ORG Charset and Collation"; }
}
