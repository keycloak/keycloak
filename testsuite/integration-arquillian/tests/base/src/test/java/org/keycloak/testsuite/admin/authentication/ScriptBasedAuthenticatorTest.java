package org.keycloak.testsuite.admin.authentication;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.testsuite.ProfileAssume;

import javax.ws.rs.BadRequestException;
import java.util.HashMap;

/**
 * @author mhajas
 */
public class ScriptBasedAuthenticatorTest extends AbstractAuthenticationTest {

    @Test
    public void checkIfTurnedOffWithProductProfile() throws InterruptedException {
        ProfileAssume.assumePreviewDisabled();

        HashMap<String, String> params = new HashMap<>();
        params.put("newName", "Copy-of-browser");
        authMgmtResource.copy("browser", params);

        params.put("provider", "auth-script-based");
        try {
            authMgmtResource.addExecution("Copy-of-browser", params);
            Assert.fail("Adding script based authenticator should fail with product profile");
        } catch (BadRequestException ex) {
            //Expected
        }
    }
}
