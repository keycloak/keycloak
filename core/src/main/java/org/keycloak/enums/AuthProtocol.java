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

package org.keycloak.enums;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum AuthProtocol {

    OIDC("oidc"),
    SAML("saml"),
    OTHER("other");

    private String specName;

    AuthProtocol(String specName) {
        this.specName = specName;
    }

    public String getSpecName() {
        return specName;
    }

    public static List<AuthProtocol> getAll(){
        return Arrays.asList(AuthProtocol.OIDC, AuthProtocol.SAML, AuthProtocol.OTHER);
    }

    public static List<String> getAllNames(){
        List<String> allNames = new ArrayList<>();
        for(AuthProtocol ap : getAll())
            allNames.add(ap.name().toUpperCase());
        return allNames;
    }
}
