/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.keycloak.login.freemarker.model;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.login.freemarker.model.IdentityProviderBean.IdentityProvider;
import org.keycloak.login.freemarker.model.IdentityProviderBean.IdentityProviderComparator;

/**
 * Unit test for {@link IdentityProviderBean}
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
