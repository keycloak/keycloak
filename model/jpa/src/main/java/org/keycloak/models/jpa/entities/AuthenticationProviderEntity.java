package org.keycloak.models.jpa.entities;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Entity
@Table(name="AUTH_PROVIDER_ENTITY")
@IdClass(AuthenticationProviderEntity.Key.class)
public class AuthenticationProviderEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @Id
    @Column(name="PROVIDER_NAME")
    private String providerName;
    @Column(name="PASSWORD_UPDATE_SUPPORTED")
    private boolean passwordUpdateSupported;
    @Column(name="PRIORITY")
    private int priority;

    @ElementCollection
    @MapKeyColumn(name="name")
    @Column(name="value")
    @CollectionTable(name="AUTH_PROVIDER_CONFIG")
    private Map<String, String> config;

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public boolean isPasswordUpdateSupported() {
        return passwordUpdateSupported;
    }

    public void setPasswordUpdateSupported(boolean passwordUpdateSupported) {
        this.passwordUpdateSupported = passwordUpdateSupported;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public static class Key implements Serializable {

        protected RealmEntity realm;

        protected String providerName;

        public Key() {
        }

        public Key(RealmEntity realm, String providerName) {
            this.realm = realm;
            this.providerName = providerName;
        }

        public RealmEntity getRealm() {
            return realm;
        }

        public String getProviderName() {
            return providerName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (providerName != null ? !providerName.equals(key.providerName) : key.providerName != null) return false;
            if (realm != null ? !realm.getId().equals(key.realm != null ? key.realm.getId() : null) : key.realm != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = realm != null ? realm.getId().hashCode() : 0;
            result = 31 * result + (providerName != null ? providerName.hashCode() : 0);
            return result;
        }
    }

}
