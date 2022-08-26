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
package org.keycloak.models.map.storage.jpa.userSession.entity;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.keycloak.models.map.storage.jpa.JpaAttributeEntity;

@Entity
@Table(name = "kc_user_session_note")
public class JpaUserSessionNoteEntity extends JpaAttributeEntity<JpaUserSessionEntity> {

    public JpaUserSessionNoteEntity() {
    }

    public JpaUserSessionNoteEntity(JpaUserSessionEntity root, String name, String value) {
        super(root, name, value);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JpaUserSessionNoteEntity)) return false;
        JpaUserSessionNoteEntity that = (JpaUserSessionNoteEntity) obj;
        return Objects.equals(getParent(), that.getParent()) &&
               Objects.equals(getName(), that.getName());
    }
}
