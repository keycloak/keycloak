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

package org.keycloak.models.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Table(name="REALM_REQUIRED_CREDENTIAL")
@Entity
@IdClass(RequiredCredentialEntity.Key.class)
public class RequiredCredentialEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @Id
    @Column(name = "TYPE")
    protected String type;
    @Column(name = "INPUT")
    protected boolean input;
    @Column(name = "SECRET")
    protected boolean secret;
    @Column(name = "FORM_LABEL")
    protected String formLabel;

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isInput() {
        return input;
    }

    public void setInput(boolean input) {
        this.input = input;
    }

    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    public String getFormLabel() {
        return formLabel;
    }

    public void setFormLabel(String formLabel) {
        this.formLabel = formLabel;
    }

    public static class Key implements Serializable {

        protected RealmEntity realm;

        protected String type;

        public Key() {
        }

        public Key(RealmEntity realm, String type) {
            this.realm = realm;
            this.type = type;
        }

        public RealmEntity getRealm() {
            return realm;
        }

        public String getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (realm != null ? !realm.getId().equals(key.realm != null ? key.realm.getId() : null) : key.realm != null) return false;
            if (type != null ? !type.equals(key.type) : key.type != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = realm != null ? realm.getId().hashCode() : 0;
            result = 31 * result + (type != null ? type.hashCode() : 0);
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof RequiredCredentialEntity)) return false;

        RequiredCredentialEntity key = (RequiredCredentialEntity) o;

        if (realm != null ? !realm.getId().equals(key.realm != null ? key.realm.getId() : null) : key.realm != null) return false;
        if (type != null ? !type.equals(key.type) : key.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = realm != null ? realm.getId().hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }


}
