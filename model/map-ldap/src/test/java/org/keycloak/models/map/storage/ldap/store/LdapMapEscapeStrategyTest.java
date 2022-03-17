/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.ldap.store;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test escaping of characters.
 *
 * Test cases extracted from https://docs.oracle.com/cd/E29127_01/doc.111170/e28969/ds-ldif-search-filters.htm#gdxoy
 */
public class LdapMapEscapeStrategyTest {

    @Test
    public void shouldEscapeUtf8CharactersForDefaultStrategy() {
        Assert.assertEquals("V\\c3\\a9ronique", LdapMapEscapeStrategy.DEFAULT.escape("V\u00E9ronique"));
    }

    @Test
    public void shouldEscapeLdapQueryCharactersCharactersForDefaultStrategy() {
        Assert.assertEquals("\\28\\29\\2a\\5c", LdapMapEscapeStrategy.DEFAULT.escape("()*\\"));
    }

}