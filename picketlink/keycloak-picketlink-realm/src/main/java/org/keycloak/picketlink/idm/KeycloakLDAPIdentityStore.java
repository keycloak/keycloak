package org.keycloak.picketlink.idm;

import java.lang.reflect.Method;

import javax.naming.directory.BasicAttributes;

import org.keycloak.models.utils.reflection.Reflections;
import org.picketlink.idm.config.LDAPMappingConfiguration;
import org.picketlink.idm.ldap.internal.LDAPIdentityStore;
import org.picketlink.idm.ldap.internal.LDAPOperationManager;
import org.picketlink.idm.model.AttributedType;
import org.picketlink.idm.model.basic.User;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.spi.IdentityContext;

import static org.picketlink.common.constants.LDAPConstants.CN;
import static org.picketlink.common.constants.LDAPConstants.COMMA;
import static org.picketlink.common.constants.LDAPConstants.EQUAL;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakLDAPIdentityStore extends LDAPIdentityStore {

    public static Method GET_BINDING_DN_METHOD;
    public static Method GET_OPERATION_MANAGER_METHOD;
    public static Method CREATE_SEARCH_FILTER_METHOD;
    public static Method EXTRACT_ATTRIBUTES_METHOD;
    public static Method GET_ENTRY_IDENTIFIER_METHOD;

    public static final String SAM_ACCOUNT_NAME = "sAMAccountName";

    static {
        GET_BINDING_DN_METHOD = getMethodOnLDAPStore("getBindingDN", AttributedType.class);
        GET_OPERATION_MANAGER_METHOD = getMethodOnLDAPStore("getOperationManager");
        CREATE_SEARCH_FILTER_METHOD = getMethodOnLDAPStore("createIdentityTypeSearchFilter", IdentityQuery.class, LDAPMappingConfiguration.class);
        EXTRACT_ATTRIBUTES_METHOD = getMethodOnLDAPStore("extractAttributes", AttributedType.class, boolean.class);
        GET_ENTRY_IDENTIFIER_METHOD = getMethodOnLDAPStore("getEntryIdentifier", AttributedType.class);
    }

    @Override
    public void addAttributedType(IdentityContext context, AttributedType attributedType) {
        LDAPMappingConfiguration userMappingConfig = getConfig().getMappingConfig(attributedType.getClass());
        String ldapUsernameAttrName = userMappingConfig.getMappedProperties().get(userMappingConfig.getIdProperty().getName());

        if (getConfig().isActiveDirectory() && SAM_ACCOUNT_NAME.equals(ldapUsernameAttrName)) {
            // TODO: pain, but everything in picketlink is private... Improve if possible...
            LDAPOperationManager operationManager = Reflections.invokeMethod(false, GET_OPERATION_MANAGER_METHOD, LDAPOperationManager.class, this);

            String cn = getCommonName(attributedType);
            String bindingDN = CN + EQUAL + cn + COMMA + userMappingConfig.getBaseDN();

            BasicAttributes ldapAttributes = Reflections.invokeMethod(false, EXTRACT_ATTRIBUTES_METHOD, BasicAttributes.class, this, attributedType, true);
            ldapAttributes.put(CN, cn);

            operationManager.createSubContext(bindingDN, ldapAttributes);

            String ldapId = Reflections.invokeMethod(false, GET_ENTRY_IDENTIFIER_METHOD, String.class, this, attributedType);
            attributedType.setId(ldapId);
        } else {
            super.addAttributedType(context, attributedType);
        }
    }

    // Hack as methods are protected on LDAPIdentityStore :/
    public static Method getMethodOnLDAPStore(String methodName, Class... classes) {
        try {
            Method m = LDAPIdentityStore.class.getDeclaredMethod(methodName, classes);
            m.setAccessible(true);
            return m;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String getCommonName(AttributedType aType) {
        User user = (User)aType;
        String fullName;
        if (user.getFirstName() != null && user.getLastName() != null) {
            fullName = user.getFirstName() + " " + user.getLastName();
        } else if (user.getFirstName() != null && user.getFirstName().trim().length() > 0) {
            fullName = user.getFirstName();
        } else {
            fullName = user.getLastName();
        }

        // Fallback to loginName
        if (fullName == null || fullName.trim().length() == 0) {
            fullName = user.getLoginName();
        }

        return fullName;
    }
}
