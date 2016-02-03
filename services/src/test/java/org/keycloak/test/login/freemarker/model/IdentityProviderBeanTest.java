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
package org.keycloak.test.login.freemarker.model;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean.IdentityProvider;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean.IdentityProviderComparator;

/**
 * Unit test for {@link org.keycloak.forms.login.freemarker.model.IdentityProviderBean}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class IdentityProviderBeanTest {


    @Test
    public void testIdentityProviderComparator() {

        IdentityProvider o1 = new IdentityProvider("alias1", "id1", "ur1", null);
        IdentityProvider o2 = new IdentityProvider("alias2", "id2", "ur2", null);

        // guiOrder not defined at any object - first is always lower
        Assert.assertEquals(1, IdentityProviderComparator.INSTANCE.compare(o1, o2));
        Assert.assertEquals(1, IdentityProviderComparator.INSTANCE.compare(o2, o1));

        // guiOrder is not a number so it is same as not defined - first is always lower
        o1 = new IdentityProvider("alias1", "id1", "ur1", "not a number");
        Assert.assertEquals(1, IdentityProviderComparator.INSTANCE.compare(o1, o2));
        Assert.assertEquals(1, IdentityProviderComparator.INSTANCE.compare(o2, o1));

        // guiOrder is defined for one only to it is always first
        o1 = new IdentityProvider("alias1", "id1", "ur1", "0");
        Assert.assertEquals(-1, IdentityProviderComparator.INSTANCE.compare(o1, o2));
        Assert.assertEquals(1, IdentityProviderComparator.INSTANCE.compare(o2, o1));

        // guiOrder is defined for both but is same - first is always lower
        o1 = new IdentityProvider("alias1", "id1", "ur1", "0");
        o2 = new IdentityProvider("alias2", "id2", "ur2", "0");
        Assert.assertEquals(1, IdentityProviderComparator.INSTANCE.compare(o1, o2));
        Assert.assertEquals(1, IdentityProviderComparator.INSTANCE.compare(o2, o1));

        // guiOrder is reflected
        o1 = new IdentityProvider("alias1", "id1", "ur1", "0");
        o2 = new IdentityProvider("alias2", "id2", "ur2", "1");
        Assert.assertEquals(-1, IdentityProviderComparator.INSTANCE.compare(o1, o2));
        Assert.assertEquals(1, IdentityProviderComparator.INSTANCE.compare(o2, o1));

    }

}
