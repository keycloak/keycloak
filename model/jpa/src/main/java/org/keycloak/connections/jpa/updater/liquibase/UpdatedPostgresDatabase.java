package org.keycloak.connections.jpa.updater.liquibase;

import liquibase.GlobalConfiguration;
import liquibase.database.ObjectQuotingStrategy;
import liquibase.database.core.PostgresDatabase;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Catalog;
import liquibase.structure.core.Schema;
import liquibase.util.StringUtil;

import java.util.Locale;

/**
 * Workaround for <a href="https://github.com/keycloak/keycloak/issues/39917">keycloak#39917</a>
 * Remove it once Liquibase 4.32.0 is used
 */
public class UpdatedPostgresDatabase extends PostgresDatabase {

    @Override
    public int getPriority() {
        return super.getPriority() + 1; // Always take precedence over factory PostgresDatabase
    }
    
    /**
     * Use updated version present in Liquibase 4.32.0 - <a href="https://github.com/liquibase/liquibase/blob/v4.32.0/liquibase-standard/src/main/java/liquibase/database/core/PostgresDatabase.java#L300">PostgresDatabase.correctObjectName()</a>
     */
    @Override
    public String correctObjectName(String objectName, Class<? extends DatabaseObject> objectType) {
        if ((objectName == null) || (quotingStrategy != ObjectQuotingStrategy.LEGACY)) {
            return super.correctObjectName(objectName, objectType);
        }
        //
        // Check preserve case flag for schema
        //
        if (objectType.equals(Schema.class) && Boolean.TRUE.equals(GlobalConfiguration.PRESERVE_SCHEMA_CASE.getCurrentValue())) {
            return objectName;
        }

        if (objectType.equals(Catalog.class) && !StringUtil.hasLowerCase(objectName)) {
            return objectName;
        }

        if (objectName.contains("-")
                || hasMixedCase(objectName)
                || startsWithNumeric(objectName)
                || isReservedWord(objectName)) {
            return objectName;
        } else {
            return objectName.toLowerCase(Locale.US);
        }
    }


}
