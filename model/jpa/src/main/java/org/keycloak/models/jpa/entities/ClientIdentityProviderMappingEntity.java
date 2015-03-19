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
 * @author pedroigor
 */
@Table(name="CLIENT_IDENTITY_PROVIDER_MAPPING")
@Entity
@IdClass(ClientIdentityProviderMappingEntity.Key.class)
public class ClientIdentityProviderMappingEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_ID")
    private ClientEntity client;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDENTITY_PROVIDER_ID")
    private IdentityProviderEntity identityProvider;

    @Column(name = "RETRIEVE_TOKEN")
    private boolean retrieveToken;

    public ClientEntity getClient() {
        return this.client;
    }

    public void setClient(ClientEntity client) {
        this.client = client;
    }

    public IdentityProviderEntity getIdentityProvider() {
        return this.identityProvider;
    }

    public void setIdentityProvider(IdentityProviderEntity identityProvider) {
        this.identityProvider = identityProvider;
    }

    public void setRetrieveToken(boolean retrieveToken) {
        this.retrieveToken = retrieveToken;
    }

    public boolean isRetrieveToken() {
        return retrieveToken;
    }

    public static class Key implements Serializable {

        private ClientEntity client;
        private IdentityProviderEntity identityProvider;

        public Key() {
        }

        public Key(ClientEntity client, IdentityProviderEntity identityProvider) {
            this.client = client;
            this.identityProvider = identityProvider;
        }

        public ClientEntity getUser() {
            return client;
        }

        public IdentityProviderEntity getIdentityProvider() {
            return identityProvider;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (identityProvider != null ? !identityProvider.getAlias().equals(key.identityProvider.getAlias()) : key.identityProvider != null)
                return false;
            if (client != null ? !client.getId().equals(key.client != null ? key.client.getId() : null) : key.client != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = client != null ? client.getId().hashCode() : 0;
            result = 31 * result + (identityProvider != null ? identityProvider.hashCode() : 0);
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientIdentityProviderMappingEntity key = (ClientIdentityProviderMappingEntity) o;

        if (identityProvider != null ? !identityProvider.getAlias().equals(key.identityProvider.getAlias()) : key.identityProvider != null)
            return false;
        if (client != null ? !client.getId().equals(key.client != null ? key.client.getId() : null) : key.client != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = client != null ? client.getId().hashCode() : 0;
        result = 31 * result + (identityProvider != null ? identityProvider.hashCode() : 0);
        return result;
    }
}
