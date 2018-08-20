/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.clean.start;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CleanStartTest {


    @Test
    public void cleanStartTest() {
        //empty test - container is started via arquillian and logs are checked 
        //by org.keycloak.testsuite.arquillian.AuthServerTestEnricher#checkServerLogs
        
        //verify that checkServerLogs is not skipped
        assertTrue("checkServerLogs is skipped.", Boolean.parseBoolean(System.getProperty("auth.server.log.check", "true")));
    }
}
