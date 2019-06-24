package org.keycloak.authentication.authenticators.browser;

import org.jboss.resteasy.spi.HttpRequest;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import javax.ws.rs.core.HttpHeaders;
import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.DEFAULT_OTP_OUTCOME;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE_OTP_ROLE;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class ConditionalOtpFormAuthenticatorTest {

    private Map<String, String> config;

    private boolean otpFormShown;

    private UserModel user;

    private Set<GroupModel> userGroups;

    private Set<RoleModel> userRoles;

    @Mock
    private RealmModel realm;

    @Mock
    private RoleModel role;

    @Mock
    private GroupModel group;

    @Mock
    private LoginFormsProvider form;

    @Mock
    private HttpHeaders httpHeaders;

    @Mock
    private HttpRequest httpRequest;

    @Mock
    private AuthenticatorConfigModel authenticatorConfigModel;

    @Mock
    private AuthenticationFlowContext authenticationFlowContext;

    private ConditionalOtpFormAuthenticator authenticator;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.authenticator = new ConditionalOtpFormAuthenticator();
        this.config = new HashMap<>();
        this.config.put(DEFAULT_OTP_OUTCOME, "skip");
        this.otpFormShown = false;
        this.user = new AbstractUserAdapter(null, this.realm, null) {

            @Override
            public String getUsername() {
                return "";
            }

            protected Set<GroupModel> getGroupsInternal() {
                return userGroups;
            }

            protected Set<RoleModel> getRoleMappingsInternal() {
                return userRoles;
            }
        };
        this.userRoles = new HashSet<>();
        this.userGroups = new HashSet<>();
        this.user.getRoleMappings().clear();
        when(authenticationFlowContext.form()).thenReturn(form);
        when(authenticationFlowContext.getAuthenticatorConfig()).thenReturn(authenticatorConfigModel);
        when(authenticatorConfigModel.getConfig()).thenReturn(config);
        when(authenticationFlowContext.getRealm()).thenReturn(realm);
        when(authenticationFlowContext.getUser()).thenReturn(user);
        when(authenticationFlowContext.getHttpRequest()).thenReturn(httpRequest);
        when(httpRequest.getHttpHeaders()).thenReturn(httpHeaders);
        doAnswer((Answer) invocation -> {
            this.otpFormShown = true;
            return null;
        }).when(authenticationFlowContext).challenge(any());
    }

    @Test
    public void shouldNotShowOTPWithDefaultingToSkip() {

        this.authenticator.authenticate(this.authenticationFlowContext);

        assertFalse("OTP Form not shown", this.otpFormShown);
    }

    @Test
    public void shouldNotShowOtpWithoutAnyGroup() {
        this.config.put(FORCE_OTP_ROLE, "admin");
        when(realm.getRole("admin")).thenReturn(role);

        this.authenticator.authenticate(this.authenticationFlowContext);

        assertFalse("OTP Form not shown", this.otpFormShown);
    }

    @Test
    public void shouldShowOtpWithExplicitAssignedAndExistignRealmRole() {
        this.config.put(FORCE_OTP_ROLE, "admin");
        when(realm.getRole("admin")).thenReturn(role);
        this.userRoles.add(role);

        this.authenticator.authenticate(this.authenticationFlowContext);

        assertTrue("OTP Form shown", this.otpFormShown);
    }

    @Test
    public void shouldShowOtpWithImplicitRealmRoleViaGroup() {
        this.config.put(FORCE_OTP_ROLE, "admin");
        when(realm.getRole("admin")).thenReturn(role);
        when(group.hasRole(role)).thenReturn(true);
        when(group.getRoleMappings()).thenReturn(new HashSet<>(Arrays.asList(role)));
        this.userGroups.add(group);

        this.authenticator.authenticate(this.authenticationFlowContext);

        assertTrue("OTP Form shown", this.otpFormShown);
    }

    @Test
    public void shouldNotShowOtpWithoutImplicitRealmRoleViaGroup() {
        this.config.put(FORCE_OTP_ROLE, "admin");
        when(realm.getRole("admin")).thenReturn(role);
        when(group.hasRole(role)).thenReturn(false);
        when(group.getRoleMappings()).thenReturn(new HashSet<>(Arrays.asList(role)));
        this.userGroups.add(group);

        this.authenticator.authenticate(this.authenticationFlowContext);

        assertFalse("OTP Form not shown", this.otpFormShown);
    }
}