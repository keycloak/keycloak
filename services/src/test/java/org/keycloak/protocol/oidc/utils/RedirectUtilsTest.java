/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oidc.utils;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import java.util.HashSet;
import org.junit.Test;

/**
 *
 * @author spurreiter
 */
public class RedirectUtilsTest {

  @Test
  public void testEquals() {
    Set<String> validRedirects = new HashSet<String>();
    validRedirects.add("http://sub.acme.com/login");
    boolean isValid = RedirectUtils.matchesRedirects(validRedirects, "http://sub.acme.com/login");
    assertEquals(isValid, true);
  }

  @Test
  public void testWildCardPaths() {
    Set<String> validRedirects = new HashSet<String>();
    validRedirects.add("http://sub.acme.com/*");
    boolean isValid = RedirectUtils.matchesRedirects(validRedirects, "http://sub.acme.com/login?client_id=test");
    assertEquals(isValid, true);
  }

  @Test
  public void testWildCardPathsFail() {
    Set<String> validRedirects = new HashSet<String>();
    validRedirects.add("http://sub.acme.com/*");
    boolean isValid = RedirectUtils.matchesRedirects(validRedirects, "http://other.acme.com/login?client_id=test");
    assertEquals(isValid, false);
  }

  @Test
  public void testWildCardSubDomain() {
    Set<String> validRedirects = new HashSet<String>();
    validRedirects.add("http://*.acme.com/*");
    boolean isValid = RedirectUtils.matchesRedirects(validRedirects, "http://sub.acme.com/login?client_id=test");
    assertEquals(isValid, true);
  }

  @Test
  public void testWildCardSubDomainStrict() {
    Set<String> validRedirects = new HashSet<String>();
    validRedirects.add("http://*.acme.com/login");
    boolean isValid = RedirectUtils.matchesRedirects(validRedirects, "http://sub.acme.com/login");
    assertEquals(isValid, true);
  }

  @Test
  public void testWildCardSubDomainStrictFail() {
    Set<String> validRedirects = new HashSet<String>();
    validRedirects.add("http://*.acme.com/login");
    boolean isValid = RedirectUtils.matchesRedirects(validRedirects, "http://sub.acme.com/logout");
    assertEquals(isValid, false);
  }

  @Test
  public void testWildCardSubDomains() {
    Set<String> validRedirects = new HashSet<String>();
    validRedirects.add("http://*.acme.com/*");
    validRedirects.add("http://*.test.other.com/login/*");
    boolean isValid = RedirectUtils.matchesRedirects(validRedirects, "http://sub.test.other.com/login?client_id=test");
    assertEquals(isValid, true);
  }

  @Test
  public void testWildCardSubDomainMixed() {
    Set<String> validRedirects = new HashSet<String>();
    validRedirects.add("http://*.acme.com/*");
    validRedirects.add("http://test.other.com/login");
    boolean isValid = RedirectUtils.matchesRedirects(validRedirects, "http://test.other.com/login");
    assertEquals(isValid, true);
  }

  @Test
  public void testWildCardDomainFail() {
    Set<String> validRedirects = new HashSet<String>();
    validRedirects.add("http://*.com/*");
    boolean isValid = RedirectUtils.matchesRedirects(validRedirects, "http://other.com");
    assertEquals(isValid, false);
  }
}
