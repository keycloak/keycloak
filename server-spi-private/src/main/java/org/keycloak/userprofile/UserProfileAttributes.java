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

package org.keycloak.userprofile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class UserProfileAttributes extends HashMap<String, List<String>> {

    public UserProfileAttributes(Map<String, List<String>> attribtues){
        this.putAll(attribtues);
    }

    public void setAttribute(String key, List<String> value){
        this.put(key,value);
    }

    public void setSingleAttribute(String key, String value) {
        this.setAttribute(key, Collections.singletonList(value));
    }

    public String getFirstAttribute(String key) {
        return this.get(key) == null ? null : this.get(key).isEmpty()? null : this.get(key).get(0);
    }

    public List<String> getAttribute(String key) {
        return this.get(key);
    }

    public void removeAttribute(String attr) {
        this.remove(attr);
    }
}
