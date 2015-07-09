package org.keycloak.models.jpa.entities;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Table(name="AUTHENTICATION_FLOW")
@Entity
@NamedQueries({
        @NamedQuery(name="getAuthenticationFlowsByRealm", query="select flow from AuthenticationFlowEntity flow where flow.realm = :realm"),
        @NamedQuery(name="deleteAuthenticationFlowByRealm", query="delete from AuthenticationFlowEntity flow where flow.realm = :realm")
})
public class AuthenticationFlowEntity {
    @Id
    @Column(name="ID", length = 36)
    protected String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @Column(name="ALIAS")
    protected String alias;

    @Column(name="PROVIDER_ID")
    protected String providerId;

    @Column(name="DESCRIPTION")
    protected String description;

    @Column(name="TOP_LEVEL")
    protected boolean topLevel;

    @Column(name="BUILT_IN")
    protected boolean builtIn;


    @OneToMany(fetch = FetchType.LAZY, cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "parentFlow")
    Collection<AuthenticationExecutionEntity> executions = new ArrayList<AuthenticationExecutionEntity>();
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Collection<AuthenticationExecutionEntity> getExecutions() {
        return executions;
    }

    public void setExecutions(Collection<AuthenticationExecutionEntity> executions) {
        this.executions = executions;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean isTopLevel() {
        return topLevel;
    }

    public void setTopLevel(boolean topLevel) {
        this.topLevel = topLevel;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }
}
