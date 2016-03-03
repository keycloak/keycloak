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

package org.keycloak.testsuite.admin;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.Version;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ServerInfoTest extends AbstractClientTest {

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @Test
    public void testServerInfo() {
        ServerInfoRepresentation info = keycloak.serverInfo().getInfo();

        Assert.assertNotNull(info);
        Assert.assertNotNull(info.getProviders());
        Assert.assertNotNull(info.getThemes());
        Assert.assertNotNull(info.getEnums());

        Assert.assertNotNull(info.getMemoryInfo());
        Assert.assertNotNull(info.getSystemInfo());

        Assert.assertEquals(Version.VERSION, info.getSystemInfo().getVersion());
        Assert.assertNotNull(info.getSystemInfo().getServerTime());
        Assert.assertNotNull(info.getSystemInfo().getUptime());
    }

}
