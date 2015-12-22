package org.keycloak.federation.ldap.mappers.membership;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.UserFederationMapperModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class CommonLDAPGroupMapperConfig {

    // Name of LDAP attribute on role, which is used for membership mappings. Usually it will be "member"
    public static final String MEMBERSHIP_LDAP_ATTRIBUTE = "membership.ldap.attribute";

    // See docs for MembershipType enum
    public static final String MEMBERSHIP_ATTRIBUTE_TYPE = "membership.attribute.type";

    // See docs for Mode enum
    public static final String MODE = "mode";

    // See docs for UserRolesRetriever enum
    public static final String USER_ROLES_RETRIEVE_STRATEGY = "user.roles.retrieve.strategy";


    protected final UserFederationMapperModel mapperModel;

    public CommonLDAPGroupMapperConfig(UserFederationMapperModel mapperModel) {
        this.mapperModel = mapperModel;
    }

    public String getMembershipLdapAttribute() {
        String membershipAttrName = mapperModel.getConfig().get(MEMBERSHIP_LDAP_ATTRIBUTE);
        return membershipAttrName!=null ? membershipAttrName : LDAPConstants.MEMBER;
    }

    public MembershipType getMembershipTypeLdapAttribute() {
        String membershipType = mapperModel.getConfig().get(MEMBERSHIP_ATTRIBUTE_TYPE);
        return (membershipType!=null && !membershipType.isEmpty()) ? Enum.valueOf(MembershipType.class, membershipType) : MembershipType.DN;
    }

    public LDAPGroupMapperMode getMode() {
        String modeString = mapperModel.getConfig().get(MODE);
        if (modeString == null || modeString.isEmpty()) {
            throw new ModelException("Mode is missing! Check your configuration");
        }

        return Enum.valueOf(LDAPGroupMapperMode.class, modeString.toUpperCase());
    }

    protected Set<String> getConfigValues(String str) {
        String[] objClasses = str.split(",");
        Set<String> trimmed = new HashSet<>();
        for (String objectClass : objClasses) {
            objectClass = objectClass.trim();
            if (objectClass.length() > 0) {
                trimmed.add(objectClass);
            }
        }
        return trimmed;
    }

    public abstract String getLDAPGroupsDn();

    public abstract String getLDAPGroupNameLdapAttribute();


}
