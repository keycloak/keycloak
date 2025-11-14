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

package org.keycloak.theme.beans;

import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class MessageFormatterMethodTest {

    @Test
    public void test() throws TemplateModelException {

        Locale locale = Locale.US;

        Properties properties = new Properties();
        properties.setProperty("backToApplication", "Back to application");
        properties.setProperty("backToClient", "Back to {0}");
        properties.setProperty("client_admin-console", "Admin Console");
        properties.setProperty("realm_example-realm", "Example Realm");
        properties.setProperty("key", "foo {0,choice,0#foo|1#bar|1<{0} foobar} bar");


        MessageFormatterMethod fmt = new MessageFormatterMethod(locale, properties);

        String msg = (String) fmt.exec(Arrays.asList("backToClient", "${client_admin-console}"));
        Assert.assertEquals("Back to Admin Console", msg);

        msg = (String) fmt.exec(Arrays.asList("backToClient", "client_admin-console"));
        Assert.assertEquals("Back to client_admin-console", msg);

        msg = (String) fmt.exec(Arrays.asList("backToClient", "client '${client_admin-console}' from '${realm_example-realm}'."));
        Assert.assertEquals("Back to client 'Admin Console' from 'Example Realm'.", msg);

        msg = (String) fmt.exec(Arrays.asList(new SimpleScalar("key"),new SimpleNumber(0)));
        Assert.assertEquals("foo foo bar", msg);

        msg = (String) fmt.exec(Arrays.asList(new SimpleScalar("key"),new SimpleNumber(1)));
        Assert.assertEquals("foo bar bar", msg);

        msg = (String) fmt.exec(Arrays.asList(new SimpleScalar("key"),new SimpleNumber(2)));
        Assert.assertEquals("foo 2 foobar bar", msg);
    }

}
