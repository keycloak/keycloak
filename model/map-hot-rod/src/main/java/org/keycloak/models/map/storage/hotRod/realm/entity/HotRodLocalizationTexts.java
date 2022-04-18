/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.hotRod.realm.entity;

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.storage.hotRod.common.HotRodPair;

import java.util.Objects;
import java.util.Set;

public class HotRodLocalizationTexts {
    @ProtoField(number = 1)
    public String locale;

    @ProtoField(number = 2)
    public Set<HotRodPair<String, String>> values;

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public Set<HotRodPair<String, String>> getValues() {
        return values;
    }

    public void setValues(Set<HotRodPair<String, String>> values) {
        this.values = values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HotRodLocalizationTexts that = (HotRodLocalizationTexts) o;
        return Objects.equals(locale, that.locale) && Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locale, values);
    }
}
