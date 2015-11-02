/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.arquillian.migration;

import java.lang.reflect.Method;
import org.jboss.arquillian.test.spi.execution.ExecutionDecision;
import org.jboss.arquillian.test.spi.execution.TestExecutionDecider;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class MigrationTestExecutionDecider implements TestExecutionDecider {

    @Override
    public ExecutionDecision decide(Method method) {
        
        boolean migrationTest = "true".equals(System.getProperty("migration", "false"));
        Migration migrationAnnotation = method.getAnnotation(Migration.class);
        
        if (migrationTest && migrationAnnotation != null) {
            String versionFrom = migrationAnnotation.versionFrom();
            String version = System.getProperty("version");
            

            if (version.equals(versionFrom)) {
                return ExecutionDecision.execute();
            } else {
                return ExecutionDecision.dontExecute(method.getName() + "doesn't fit with migration version.");
            }
        }
        if ((migrationTest && migrationAnnotation == null) || (!migrationTest && migrationAnnotation != null)) {
            return ExecutionDecision.dontExecute("Migration test and no migration annotation or no migration test and migration annotation");
        }
        return ExecutionDecision.execute();
    }

    @Override
    public int precedence() {
        return 1;
    }

}
