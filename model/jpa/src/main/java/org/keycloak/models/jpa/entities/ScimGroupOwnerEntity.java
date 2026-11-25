/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.jpa.entities;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The SCIM client that created a group, and so owns it.
 *
 * <p>The owner is stored by the client's {@code clientId}, not by its internal id, and has no foreign key to the
 * client. Deleting the client keeps the row, and a client created again with the same {@code clientId} owns the group
 * again. The database removes the row together with the group. A group without a row was created some other way, for
 * example by an administrator, or before the realm turned ownership on.
 */
@Entity
@Table(name = "SCIM_GROUP_OWNER")
public class ScimGroupOwnerEntity {

    @Id
    @Column(name = "GROUP_ID", length = 36)
    protected String groupId;

    @Column(name = "CLIENT_ID", nullable = false)
    protected String clientId;

    public ScimGroupOwnerEntity() {
    }

    public ScimGroupOwnerEntity(String groupId, String clientId) {
        this.groupId = groupId;
        this.clientId = clientId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScimGroupOwnerEntity that)) return false;
        return Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(groupId);
    }
}
