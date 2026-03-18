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

import java.net.URI;
import java.util.List;

import org.keycloak.models.RealmModel;
import org.keycloak.services.Urls;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 */
public class RequiredActionUrlFormatterMethod implements TemplateMethodModelEx {
    private final String realm;
    private final URI baseUri;

    public RequiredActionUrlFormatterMethod(RealmModel realm, URI baseUri) {
        this.realm = realm.getName();
        this.baseUri = baseUri;
    }

    @Override
    public Object exec(List list) throws TemplateModelException {
        String action = list.get(0).toString();
        String relativePath = list.get(1).toString();
        String url = Urls.requiredActionBase(baseUri).path(relativePath).build(realm, action).toString();
        return url;
    }
}
