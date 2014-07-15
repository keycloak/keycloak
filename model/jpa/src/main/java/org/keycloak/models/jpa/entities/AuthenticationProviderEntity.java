package org.keycloak.models.jpa.entities;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Entity
@Table(name="AuthProviderEntity")
public class AuthenticationProviderEntity {

    @Id
    @GenericGenerator(name="keycloak_generator", strategy="org.keycloak.models.jpa.utils.JpaIdGenerator")
    @GeneratedValue(generator = "keycloak_generator")
    protected String id;

    private String providerName;
    private boolean passwordUpdateSupported;
    private int priority;

    @ElementCollection
    @MapKeyColumn(name="name")
    @Column(name="value")
    @CollectionTable(name="AuthProviderEntity_cfg", joinColumns = {
            @JoinColumn(name = "AuthProviderEntity_id")
    })
    private Map<String, String> config;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}
