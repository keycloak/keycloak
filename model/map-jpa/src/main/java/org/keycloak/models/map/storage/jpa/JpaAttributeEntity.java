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
package org.keycloak.models.map.storage.jpa;

import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import org.hibernate.annotations.Nationalized;

@MappedSuperclass
public abstract class JpaAttributeEntity<E> implements JpaChildEntity<E> {

    @Id
    @Column
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="fk_root")
    private E root;

    @Column
    private String name;

    @Nationalized
    @Column
    private String value;

    public JpaAttributeEntity() {
    }

    public JpaAttributeEntity(E root, String name, String value) {
        this.root = root;
        this.name = name;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
 
    @Override
    public E getParent() {
        return root;
    }

    public void setParent(E root) {
        this.root = root;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JpaAttributeEntity)) return false;
        JpaAttributeEntity<?> that = (JpaAttributeEntity<?>) obj;
        return Objects.equals(getParent(), that.getParent()) &&
               Objects.equals(getName(), that.getName()) &&
               Objects.equals(getValue(), that.getValue());
    }
}
