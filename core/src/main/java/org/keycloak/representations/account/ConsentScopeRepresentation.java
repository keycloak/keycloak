/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.representations.account;

public class ConsentScopeRepresentation {

    private String id;

    private String name;

    private String displayText;

    public ConsentScopeRepresentation() {
    }

    public ConsentScopeRepresentation(String id, String name, String displayText) {
        this.id = id;
        this.name = name;
        this.displayText = displayText;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    /**
     * @deprecated Use {@link #getDisplayText()} instead. This method will be removed in KC 27.0.
     */
    @Deprecated
    public String getDisplayTest() {
        return displayText;
    }

    /**
     * @deprecated Use {@link #setDisplayText(String)} instead. This method will be removed in KC 27.0.
     */
    @Deprecated
    public void setDisplayTest(String displayTest) {
        this.displayText = displayTest;
    }
}
