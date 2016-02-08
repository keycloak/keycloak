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

package org.keycloak.models.entities;

/**
 * Base for the identifiable entity
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AbstractIdentifiableEntity {

    protected String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (this.id == null) return false;

        if (o == null || getClass() != o.getClass()) return false;

        AbstractIdentifiableEntity that = (AbstractIdentifiableEntity) o;

        if (!getId().equals(that.getId())) return false;

        return true;

    }

    @Override
    public int hashCode() {
        return id!=null ? id.hashCode() : super.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s [ id=%s ]", getClass().getSimpleName(), getId());
    }
}
