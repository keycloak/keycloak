/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.connections.jpa.updater.liquibase.conn;

import org.keycloak.common.util.reflections.Reflections;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import liquibase.ContextExpression;
import liquibase.Labels;
import liquibase.change.CheckSum;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.RanChangeSet;
import liquibase.changelog.StandardChangeLogHistoryService;
import liquibase.database.Database;
import liquibase.database.core.MySQLDatabase;
import liquibase.exception.DatabaseException;
import liquibase.logging.LogFactory;

/**
 *
 * @author hmlnarik
 */
public class CustomChangeLogHistoryService extends StandardChangeLogHistoryService {

    private List<RanChangeSet> ranChangeSetList;

    @Override
    public List<RanChangeSet> getRanChangeSets() throws DatabaseException {
        Database database = getDatabase();
        if (! (database instanceof MySQLDatabase)) {
            return super.getRanChangeSets();
        }
        if (this.ranChangeSetList == null) {
            String databaseChangeLogTableName = getDatabase().escapeTableName(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName());
            List<RanChangeSet> ranChangeSetList = new ArrayList<>();
            if (hasDatabaseChangeLogTable()) {
                LogFactory.getLogger().info("Reading from " + databaseChangeLogTableName);
                List<Map<String, ?>> results = queryDatabaseChangeLogTable(database);
                for (Map rs : results) {
                    String fileName = rs.get("FILENAME").toString();
                    String author = rs.get("AUTHOR").toString();
                    String id = rs.get("ID").toString();
                    String md5sum = rs.get("MD5SUM") == null || getDatabaseChecksumsCompatible() ? null : rs.get("MD5SUM").toString();
                    String description = rs.get("DESCRIPTION") == null ? null : rs.get("DESCRIPTION").toString();
                    String comments = rs.get("COMMENTS") == null ? null : rs.get("COMMENTS").toString();
                    Object tmpDateExecuted = rs.get("DATEEXECUTED");
                    Date dateExecuted = null;
                    if (tmpDateExecuted instanceof Date) {
                        dateExecuted = (Date) tmpDateExecuted;
                    } else if (tmpDateExecuted instanceof LocalDateTime) {
                        dateExecuted = Date.from(((LocalDateTime) tmpDateExecuted).atZone(ZoneId.systemDefault()).toInstant());
                    } else {
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        try {
                            dateExecuted = df.parse((String) tmpDateExecuted);
                        } catch (ParseException e) {
                        }
                    }
                    String tmpOrderExecuted = rs.get("ORDEREXECUTED").toString();
                    Integer orderExecuted = (tmpOrderExecuted == null ? null : Integer.valueOf(tmpOrderExecuted));
                    String tag = rs.get("TAG") == null ? null : rs.get("TAG").toString();
                    String execType = rs.get("EXECTYPE") == null ? null : rs.get("EXECTYPE").toString();
                    ContextExpression contexts = new ContextExpression((String) rs.get("CONTEXTS"));
                    Labels labels = new Labels((String) rs.get("LABELS"));
                    String deploymentId = (String) rs.get("DEPLOYMENT_ID");

                    try {
                        RanChangeSet ranChangeSet = new RanChangeSet(fileName, id, author, CheckSum.parse(md5sum), dateExecuted, tag, ChangeSet.ExecType.valueOf(execType), description, comments, contexts, labels, deploymentId);
                        ranChangeSet.setOrderExecuted(orderExecuted);
                        ranChangeSetList.add(ranChangeSet);
                    } catch (IllegalArgumentException e) {
                        LogFactory.getLogger().severe("Unknown EXECTYPE from database: " + execType);
                        throw e;
                    }
                }
            }

            this.ranChangeSetList = ranChangeSetList;
        }
        return Collections.unmodifiableList(ranChangeSetList);
    }

    private boolean getDatabaseChecksumsCompatible() {
        Field f = Reflections.findDeclaredField(StandardChangeLogHistoryService.class, "databaseChecksumsCompatible");
        if (f != null) {
            f.setAccessible(true);
            Boolean databaseChecksumsCompatible = Reflections.getFieldValue(f, this, Boolean.class);
            return databaseChecksumsCompatible == null ? true : databaseChecksumsCompatible;
        }
        return true;
    }

    @Override
    public int getPriority() {
        return super.getPriority() + 1; // Ensure bigger priority than StandardChangeLogHistoryService
    }
}
