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

package org.keycloak.testsuite.federation.ldap.noimport;

import org.keycloak.testsuite.federation.ldap.LDAPGroupMapperTest;

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPGroupMapperNoImportTest extends LDAPGroupMapperTest {


    @Override
    protected boolean isImportEnabled() {
        return false;
    }


    @Test
    @Override
    public void test01_ldapOnlyGroupMappings() {
        test01_ldapOnlyGroupMappings(false);
    }

    @Test
    @Override
    public void test02_readOnlyGroupMappings() {
        test02_readOnlyGroupMappings(false);
    }

    @Test
    @Override
    @Ignore
    public void test03_importGroupMappings() {
    }

}
