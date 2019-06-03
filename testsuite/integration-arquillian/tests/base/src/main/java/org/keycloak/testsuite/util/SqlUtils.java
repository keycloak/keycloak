/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.SQLExec;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SqlUtils {

    protected static final Logger log = Logger.getLogger(SqlUtils.class);


    /**
     * Run given SQL Script against specified DB
     *
     * @param sqlFilePath absolute path to the SQL file
     * @param jdbcDriverClass must be on the classpath
     * @param dbUrl
     * @param dbUsername
     * @param dbPassword
     */
    public static void runSqlScript(String sqlFilePath, String jdbcDriverClass,
                               String dbUrl, String dbUsername, String dbPassword) {
        log.infof("Running SQL script from file '%s'\n jdbcDriverClass=%s\n dbUrl=%s\n dbUsername=%s\n dbPassword=%s\n",
                sqlFilePath, jdbcDriverClass, dbUrl, dbUsername, dbPassword);

        final class SqlExecuter extends SQLExec {
            public SqlExecuter() {
                Project project = new Project();
                project.init();
                setProject(project);
                setTaskType("sql");
                setTaskName("sql");
            }
        }

        SqlExecuter executer = new SqlExecuter();
        executer.setSrc(new File(sqlFilePath));
        executer.setDriver(jdbcDriverClass);
        executer.setPassword(dbPassword);
        executer.setUserid(dbUsername);
        executer.setUrl(dbUrl);

        if (dbUrl.contains("mssql") || jdbcDriverClass.contains("mssql")) {
            log.info("Using alternative delimiter due the MSSQL");
            executer.setDelimiter("go");
            SQLExec.DelimiterType dt = new SQLExec.DelimiterType();
            dt.setValue(SQLExec.DelimiterType.ROW);
            executer.setDelimiterType(dt);
        }

        // See KEYCLOAK-3876
        if (dbUrl.contains("oracle") || jdbcDriverClass.contains("oracle")) {
            log.info("Removing 'SET DEFINE OFF' from the SQL script due the Oracle");
            try {
                String content = IOUtils.toString(new FileInputStream(sqlFilePath), StandardCharsets.UTF_8);
                content = content.replaceAll("SET DEFINE OFF;", "");
                IOUtils.write(content, new FileOutputStream(sqlFilePath), StandardCharsets.UTF_8);
            } catch (IOException fnfe) {
                throw new RuntimeException(fnfe);
            }
        }

        executer.execute();
    }
}
