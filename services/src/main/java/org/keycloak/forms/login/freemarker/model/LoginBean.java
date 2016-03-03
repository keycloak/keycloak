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
package org.keycloak.forms.login.freemarker.model;

import javax.ws.rs.core.MultivaluedMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginBean {

    private String username;

    private String password;

    private String passwordToken;

    private String rememberMe;

    public LoginBean(MultivaluedMap<String, String> formData){
        if (formData != null) {
            username = formData.getFirst("username");
            password = formData.getFirst("password");
            passwordToken = formData.getFirst("password-token");
            rememberMe = formData.getFirst("rememberMe");
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getPasswordToken() {
        return passwordToken;
    }

    public String getRememberMe() {
        return rememberMe;
    }
}
