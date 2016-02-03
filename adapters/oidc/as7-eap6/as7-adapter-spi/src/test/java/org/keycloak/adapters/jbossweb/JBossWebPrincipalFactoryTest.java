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

package org.keycloak.adapters.jbossweb;

import org.apache.catalina.Realm;
import org.junit.Assert;
import org.junit.Test;

import javax.security.auth.Subject;
import java.lang.reflect.Constructor;
import java.security.Principal;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JBossWebPrincipalFactoryTest {

    @Test
    public void test() {
        Constructor constructor = JBossWebPrincipalFactory.findJBossGenericPrincipalConstructor();
        Assert.assertNotNull(constructor);
        Assert.assertEquals(Realm.class, constructor.getParameterTypes()[0]);
        Assert.assertEquals(String.class, constructor.getParameterTypes()[1]);
        Assert.assertEquals(List.class, constructor.getParameterTypes()[3]);
        Assert.assertEquals(Principal.class, constructor.getParameterTypes()[4]);
        Assert.assertEquals(Object.class, constructor.getParameterTypes()[6]);
        Assert.assertEquals(Subject.class, constructor.getParameterTypes()[8]);
    }

}
