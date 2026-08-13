package org.keycloak.tests.admin.userstorage;

import java.util.List;

import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class UserStorageKerberosRestTest extends AbstractUserStorageRestTest {

    @Test
    public void testKerberosAuthenticatorEnabledAutomatically() {
        // Assert kerberos authenticator DISABLED
        AuthenticationExecutionInfoRepresentation kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.DISABLED.toString());

        // create LDAP provider with kerberos
        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION, "true");

        String id = createComponent(ldapRep);

        // Assert kerberos authenticator ALTERNATIVE
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString());

        // Switch kerberos authenticator to DISABLED
        kerberosExecution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED.toString());
        managedRealm.admin().flows().updateExecutions("browser", kerberosExecution);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("browser"), kerberosExecution, ResourceType.AUTH_EXECUTION);

        // update LDAP provider with kerberos (without changing kerberos switch)
        ldapRep = managedRealm.admin().components().component(id).toRepresentation();
        managedRealm.admin().components().component(id).update(ldapRep);
        adminEvents.clear();

        // Assert kerberos authenticator is still DISABLED
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.DISABLED.toString());

        // update LDAP provider with kerberos (with changing kerberos switch to disabled)
        ldapRep = managedRealm.admin().components().component(id).toRepresentation();
        ldapRep.getConfig().putSingle(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION, "false");
        managedRealm.admin().components().component(id).update(ldapRep);
        adminEvents.clear();

        // Assert kerberos authenticator is still DISABLED
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.DISABLED.toString());

        // update LDAP provider with kerberos (with changing kerberos switch to enabled)
        ldapRep = managedRealm.admin().components().component(id).toRepresentation();
        ldapRep.getConfig().putSingle(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION, "true");
        managedRealm.admin().components().component(id).update(ldapRep);
        adminEvents.clear();

        // Assert kerberos authenticator is still ALTERNATIVE
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString());

        // Cleanup
        kerberosExecution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED.toString());
        managedRealm.admin().flows().updateExecutions("browser", kerberosExecution);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("browser"), kerberosExecution, ResourceType.AUTH_EXECUTION);
        removeComponent(id);
    }

    @Test
    public void testKerberosAuthenticatorChangedOnlyIfDisabled() {
        // Change kerberos to REQUIRED
        AuthenticationExecutionInfoRepresentation kerberosExecution = findKerberosExecution();
        kerberosExecution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString());
        managedRealm.admin().flows().updateExecutions("browser", kerberosExecution);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("browser"), kerberosExecution, ResourceType.AUTH_EXECUTION);

        // create LDAP provider with kerberos
        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION, "true");

        String id = createComponent(ldapRep);


        // Assert kerberos authenticator still REQUIRED
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.REQUIRED.toString());

        // update LDAP provider with kerberos
        ldapRep = managedRealm.admin().components().component(id).toRepresentation();
        managedRealm.admin().components().component(id).update(ldapRep);
        adminEvents.clear();

        // Assert kerberos authenticator still REQUIRED
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.REQUIRED.toString());

        // Cleanup
        kerberosExecution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED.toString());
        managedRealm.admin().flows().updateExecutions("browser", kerberosExecution);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("browser"), kerberosExecution, ResourceType.AUTH_EXECUTION);
        removeComponent(id);

    }


    // KEYCLOAK-4438
    @Test
    public void testKerberosAuthenticatorDisabledWhenProviderRemoved() {
        // Assert kerberos authenticator DISABLED
        AuthenticationExecutionInfoRepresentation kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.DISABLED.toString());

        // create LDAP provider with kerberos
        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION, "true");


        String id = createComponent(ldapRep);

        // Assert kerberos authenticator ALTERNATIVE
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString());

        // Remove LDAP provider
        managedRealm.admin().components().component(id).remove();

        // Assert kerberos authenticator DISABLED
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.DISABLED.toString());

        // Add kerberos provider
        ComponentRepresentation kerberosRep = new ComponentRepresentation();
        kerberosRep.setName("kerberos");
        kerberosRep.setProviderId("kerberos");
        kerberosRep.setProviderType(UserStorageProvider.class.getName());
        kerberosRep.setConfig(new MultivaluedHashMap<>());
        kerberosRep.getConfig().putSingle("priority", Integer.toString(2));

        id = createComponent(kerberosRep);


        // Assert kerberos authenticator ALTERNATIVE
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString());

        // Switch kerberos authenticator to REQUIRED
        kerberosExecution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString());
        managedRealm.admin().flows().updateExecutions("browser", kerberosExecution);

        // Remove Kerberos provider
        managedRealm.admin().components().component(id).remove();

        // Assert kerberos authenticator DISABLED
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.DISABLED.toString());
    }

    private AuthenticationExecutionInfoRepresentation findKerberosExecution() {
        AuthenticationExecutionInfoRepresentation kerberosExecution = null;
        List<AuthenticationExecutionInfoRepresentation> executionReps = managedRealm.admin().flows().getExecutions("browser");
        kerberosExecution = findExecutionByProvider("auth-spnego", executionReps);

        Assertions.assertNotNull(kerberosExecution);
        return kerberosExecution;
    }

    private static AuthenticationExecutionInfoRepresentation findExecutionByProvider(String provider, List<AuthenticationExecutionInfoRepresentation> executions) {
        for (AuthenticationExecutionInfoRepresentation exec : executions) {
            if (provider.equals(exec.getProviderId())) {
                return exec;
            }
        }
        return null;
    }
}
