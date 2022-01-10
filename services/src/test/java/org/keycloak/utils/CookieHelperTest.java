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

package org.keycloak.utils;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.services.util.CookieHelper;

public class CookieHelperTest {
    
    @Test
    public void testParseCookies() {
        Set<String> values = CookieHelper.parseCookie(
                "terms_user=; KC_RESTART=eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJhZDUyMjdhMy1iY2ZkLTRjZjAtYTdiNi0zOTk4MzVhMDg1NjYifQ.eyJjaWQiOiJodHRwczovL3Nzby5qYm9zcy5vcmciLCJwdHkiOiJzYW1sIiwicnVyaSI6Imh0dHBzOi8vc3NvLmpib3NzLm9yZy9sb2dpbj9wcm92aWRlcj1SZWRIYXRFeHRlcm5hbFByb3ZpZGVyIiwiYWN0IjoiQVVUSEVOVElDQVRFIiwibm90ZXMiOnsiU0FNTF9SRVFVRVNUX0lEIjoibXBmbXBhYWxkampqa2ZmcG5oYmJoYWdmZmJwam1rbGFqbWVlb2lsaiIsInNhbWxfYmluZGluZyI6InBvc3QifX0.d0QJSOQ6pJGzqcjqDTRwkRpU6fwYeICedL6R9Gqs8CQ; AUTH_SESSION_ID=451ec4be-a0c8-430e-b489-6580f195ccf0; AUTH_SESSION_ID=55000981-8b5e-4c8d-853f-ee4c582c1d0d;AUTH_SESSION_ID=451ec4be-a0c8-430e-b489-6580f195ccf0; AUTH_SESSION_ID=55000981-8b5e-4c8d-853f-ee4c582c1d0d;AUTH_SESSION_ID=451ec4be-a0c8-430e-b489-6580f195ccf0; AUTH_SESSION_ID=55000981-8b5e-4c8d-853f-ee4c582c1d0d4;", "AUTH_SESSION_ID");
        
        Assert.assertEquals(3, values.size());
    }
}
