package org.keycloak.services.models.jpa.entities;


import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import java.util.Collection;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmEntity {
    @Id
    protected String id;

    protected String realmName;
    protected boolean enabled;
    protected boolean sslNotRequired;
    protected boolean cookieLoginAllowed;
    protected boolean registrationAllowed;
    protected int tokenLifespan;
    protected int accessCodeLifespan;
    @Column(length = 2048)
    protected String publicKeyPem;
    @Column(length = 2048)
    protected String privateKeyPem;
    protected String[] defaultRoles;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    Collection<RequiredCredentailEntity> requiredCredentials;
    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    Collection<ResourceEntity> resources;
    @OneToMany(fetch = FetchType.LAZY, cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    Collection<RoleEntity> roles;




}
