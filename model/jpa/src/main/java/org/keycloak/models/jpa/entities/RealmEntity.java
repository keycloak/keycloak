package org.keycloak.models.jpa.entities;


import javax.persistence.*;
import java.util.Collection;
import java.util.HashMap;

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
    @Lob
    protected HashMap<String, String> smtpConfig;
    @Lob
    protected HashMap<String, String> socialConfig;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    Collection<RequiredCredentailEntity> requiredCredentials;
    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    Collection<ResourceEntity> resources;
    @OneToMany(fetch = FetchType.LAZY, cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    Collection<RoleEntity> roles;




}
