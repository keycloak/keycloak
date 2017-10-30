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

package org.keycloak.theme.beans;

import org.keycloak.models.RealmModel;

import javax.ws.rs.core.UriBuilder;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LocaleBean {

    private Locale current;
    private List<Locale> supported;

    public LocaleBean(RealmModel realm, java.util.Locale current, UriBuilder uriBuilder, Properties messages) {
        String currentTag = current.toLanguageTag();
        this.current = new Locale(
            messages.getProperty("locale_" + currentTag, currentTag),
            uriBuilder.replaceQueryParam("kc_locale", currentTag).build().toString(),
            currentTag
        );

        supported = new LinkedList<>();
        for (String l : realm.getSupportedLocales()) {
            String label = messages.getProperty("locale_" + l, l);
            String url = uriBuilder.replaceQueryParam("kc_locale", l).build().toString();
            supported.add(new Locale(label, url, l));
        }
    }

    public String getCurrent() {
        return current.getLabel();
    }

    public String getCurrentTag() {
        return current.getTag();
    }

    public List<Locale> getSupported() {
        return supported;
    }

    public static class Locale {

        private String label;
        private String url;
        private String tag;

        public Locale(String label, String url, String tag) {
            this.label = label;
            this.url = url;
            this.tag = tag;
        }

        public String getUrl() {
            return url;
        }

        public String getLabel() {
            return label;
        }

        public String getTag() {
            return tag;
        }

    }

}
