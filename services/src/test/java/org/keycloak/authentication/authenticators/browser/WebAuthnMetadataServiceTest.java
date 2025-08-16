package org.keycloak.authentication.authenticators.browser;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class WebAuthnMetadataServiceTest {

    /**
     *
     */
    @Test
    public void testWebAuthnMetadata() {
       WebAuthnMetadataService service = new WebAuthnMetadataService();
       Assert.assertThat( service.getAuthenticatorProvider(null), nullValue());
       Assert.assertThat( service.getAuthenticatorProvider("00000000-0000-0000-0000-000000000000"), nullValue());
       Assert.assertThat( service.getAuthenticatorProvider("ea9b8d66-4d01-1d21-3ce4-b6b48cb575d4"), is("Google Password Manager"));
       Assert.assertThat( service.getAuthenticatorProvider("d548826e-79b4-db40-a3d8-11116f7e8349"), is("Bitwarden"));
    }
}
