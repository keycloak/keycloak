package org.keycloak.models.jpa.entities;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="findOAuthClientByUser", query="select o from OAuthClientEntity o where o.agent.loginName=:name and o.realm = :realm"),
        @NamedQuery(name="findOAuthClientByRealm", query="select o from OAuthClientEntity o where o.realm = :realm")

})
@Entity
public class OAuthClientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    private String name;

    @OneToOne(fetch = FetchType.EAGER)
    private UserEntity agent;

    @ManyToOne
    protected RealmEntity realm;

    public String getId() {
        return id;
    }

    public UserEntity getAgent() {
        return agent;
    }

    public void setAgent(UserEntity agent) {
        this.agent = agent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }
}
