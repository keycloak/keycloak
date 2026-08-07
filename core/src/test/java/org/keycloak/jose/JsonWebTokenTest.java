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

package org.keycloak.jose;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.common.util.Time;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.representations.AccessToken.REALM_ACCESS;
import static org.keycloak.representations.AccessToken.RESOURCE_ACCESS;
import static org.keycloak.representations.AccessToken.ROLES;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by st on 20.08.15.
 */
public class JsonWebTokenTest {

    @Test
    public void testAudSingle() throws IOException {
        String single = "{ \"aud\": \"test\" }";
        JsonWebToken s = JsonSerialization.readValue(single, JsonWebToken.class);
        assertArrayEquals(new String[]{"test"}, s.getAudience());
    }

    @Test
    public void testAudArray() throws IOException {
        String single = "{ \"aud\": [\"test\"] }";
        JsonWebToken s = JsonSerialization.readValue(single, JsonWebToken.class);
        assertArrayEquals(new String[]{"test"}, s.getAudience());
    }

    @Test
    public void testAddAudience() {
        // Token with no audience
        JsonWebToken s = new JsonWebToken();
        s.addAudience("audience-1");
        assertArrayEquals(new String[] { "audience-1"}, s.getAudience());

        // Add to existing
        s.addAudience("audience-2");
        assertArrayEquals(new String[]{"audience-1", "audience-2"}, s.getAudience());

        s.addAudience("audience-3");
        assertArrayEquals(new String[]{"audience-1", "audience-2", "audience-3"}, s.getAudience());

        // Add existing. Shouldn't be added as it's already there
        s.addAudience("audience-2");
        assertArrayEquals(new String[]{"audience-1", "audience-2", "audience-3"}, s.getAudience());
    }

    @Test
    public void test() throws IOException {
        JsonWebToken jsonWebToken = new JsonWebToken();
        jsonWebToken.audience("test");
        assertTrue(JsonSerialization.writeValueAsPrettyString(jsonWebToken).contains("\"aud\" : \"test\""));
    }

    @Test
    public void testArray() throws IOException {
        JsonWebToken jsonWebToken = new JsonWebToken();
        jsonWebToken.audience("test", "test2");
        assertTrue(JsonSerialization.writeValueAsPrettyString(jsonWebToken).contains("\"aud\" : [ \"test\", \"test2\" ]"));
    }

    @Test
    public void isActiveReturnFalseWhenBeforeTimeInFuture() {
        long currentTime = Time.currentTime();
        long futureTime = currentTime + 12; // default allowed clock skew is 10 seconds
        JsonWebToken jsonWebToken = new JsonWebToken();
        jsonWebToken.nbf(futureTime);
        assertFalse(jsonWebToken.isActive());
    }

    @Test
    public void isActiveReturnTrueWhenBeforeTimeInPast() {
        long currentTime = Time.currentTime();
        long pastTime = currentTime - 10;
        JsonWebToken jsonWebToken = new JsonWebToken();
        jsonWebToken.nbf(pastTime);
        assertTrue(jsonWebToken.isActive());
    }

    @Test
    public void isActiveShouldReturnTrueWhenBeforeTimeInFutureWithinTimeSkew() {
        long notBeforeTime = Time.currentTime() + 5;
        int allowedClockSkew = 10;
        JsonWebToken jsonWebToken = new JsonWebToken();
        jsonWebToken.nbf(notBeforeTime);
        assertTrue(jsonWebToken.isActive(allowedClockSkew));
    }

    @Test
    public void isActiveShouldReturnFalseWhenWhenBeforeTimeInFutureOutsideTimeSkew() {
        long notBeforeTime = Time.currentTime() + 10;
        int allowedClockSkew = 5;
        JsonWebToken jsonWebToken = new JsonWebToken();
        jsonWebToken.nbf(notBeforeTime);
        assertFalse(jsonWebToken.isActive(allowedClockSkew));
    }

    @Test
    public void testRealmAccessMerge() {
        Set<String> role1Set = new HashSet<>(Collections.singleton("role1"));
        Map<String, Object> map1 = new HashMap<>();
        map1.put(ROLES, role1Set);

        Set<String> role2Set = new HashSet<>(Collections.singleton("role2"));
        Set<String> rolesSet = new HashSet<>(role1Set);
        rolesSet.addAll(role2Set);

        // No "otherClaims"
        AccessToken at = new AccessToken();
        at.setRealmAccess(new AccessToken.Access());
        at.getRealmAccess().roles(role1Set);
        TokenUtil.convertTokenRolesFromOtherClaims(at);
        Assert.assertEquals(at.getRealmAccess().getRoles(), role1Set);
        Assert.assertNull(at.getOtherClaims().get(REALM_ACCESS));

        // Just "otherClaims"
        at = new AccessToken();
        at.getOtherClaims().put(REALM_ACCESS, map1);
        TokenUtil.convertTokenRolesFromOtherClaims(at);
        Assert.assertEquals(at.getRealmAccess().getRoles(), role1Set);
        Assert.assertNull(at.getOtherClaims().get(REALM_ACCESS));

        // Both
        at = new AccessToken();
        at.setRealmAccess(new AccessToken.Access());
        at.getRealmAccess().roles(role2Set);
        at.getOtherClaims().put(REALM_ACCESS, map1);
        TokenUtil.convertTokenRolesFromOtherClaims(at);
        Assert.assertEquals(at.getRealmAccess().getRoles(), rolesSet);
        Assert.assertNull(at.getOtherClaims().get(REALM_ACCESS));

        // Invalid stuff in "otherClaims"
        at = new AccessToken();
        at.setRealmAccess(new AccessToken.Access());
        at.getRealmAccess().roles(role2Set);
        at.getOtherClaims().put(REALM_ACCESS, role1Set);
        TokenUtil.convertTokenRolesFromOtherClaims(at);
        Assert.assertEquals(at.getRealmAccess().getRoles(), role2Set);
        Assert.assertNotNull(at.getOtherClaims().get(REALM_ACCESS));
    }

    @Test
    public void testResourceAccessMerge() throws IOException {
        Set<String> role1Set = new HashSet<>(Collections.singleton("role1"));
        Map<String, Object> map1 = new HashMap<>();
        map1.put(ROLES, role1Set);

        Set<String> role2Set = new HashSet<>(Collections.singleton("role2"));
        Map<String, Object> map2 = new HashMap<>();
        map2.put(ROLES, role2Set);

        Set<String> rolesSet = new HashSet<>(role1Set);
        rolesSet.addAll(role2Set);

        AccessToken.Access access1 = new AccessToken.Access();
        access1.roles(role1Set);
        Map<String, AccessToken.Access> access1Map = new HashMap<>();
        access1Map.put("client1", access1);

        // No "otherClaims"
        AccessToken at = new AccessToken();
        at.setResourceAccess(access1Map);
        TokenUtil.convertTokenRolesFromOtherClaims(at);
        Assert.assertEquals(1, at.getResourceAccess().size());
        Assert.assertEquals(role1Set, at.getResourceAccess().get("client1").getRoles());

        // Just "otherClaims"
        at = new AccessToken();
        Map<?,?> map = JsonSerialization.readValue(JsonSerialization.writeValueAsString(access1Map), Map.class);
        at.getOtherClaims().put(RESOURCE_ACCESS, map);
        TokenUtil.convertTokenRolesFromOtherClaims(at);
        Assert.assertEquals(1, at.getResourceAccess().size());
        Assert.assertEquals(role1Set, at.getResourceAccess().get("client1").getRoles());

        // Both - different clients
        at = new AccessToken();
        map = JsonSerialization.readValue(JsonSerialization.writeValueAsString(access1Map), Map.class);
        at.getOtherClaims().put(RESOURCE_ACCESS, map);
        Map<String, AccessToken.Access> accessMap = new HashMap<>();
        accessMap.put("client2", access1);
        at.setResourceAccess(accessMap);
        TokenUtil.convertTokenRolesFromOtherClaims(at);
        Assert.assertEquals(2, at.getResourceAccess().size());
        Assert.assertEquals(role1Set, at.getResourceAccess().get("client1").getRoles());
        Assert.assertEquals(role1Set, at.getResourceAccess().get("client2").getRoles());

        // Both - same client
        at = new AccessToken();
        map = JsonSerialization.readValue(JsonSerialization.writeValueAsString(access1Map), Map.class);
        at.getOtherClaims().put(RESOURCE_ACCESS, map);
        accessMap = new HashMap<>();
        AccessToken.Access access2 = new AccessToken.Access();
        access2.roles(role2Set);
        accessMap.put("client1", access2);
        at.setResourceAccess(accessMap);
        TokenUtil.convertTokenRolesFromOtherClaims(at);
        Assert.assertEquals(1, at.getResourceAccess().size());
        Assert.assertEquals(rolesSet, at.getResourceAccess().get("client1").getRoles());

        // Invalid
        at = new AccessToken();
        at.setResourceAccess(access1Map);
        at.getOtherClaims().put(RESOURCE_ACCESS, map1);
        TokenUtil.convertTokenRolesFromOtherClaims(at);
        Assert.assertEquals(1, at.getResourceAccess().size());
        Assert.assertEquals(role1Set, at.getResourceAccess().get("client1").getRoles());
    }

}
