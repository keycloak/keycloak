package org.keycloak.broker.kerberos;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosIdentityProviderFactory extends AbstractIdentityProviderFactory<KerberosIdentityProvider> {

    public static final String PROVIDER_ID = "kerberos";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getName() {
        return "Kerberos";
    }

    @Override
    public KerberosIdentityProvider create(IdentityProviderModel model) {
        return new KerberosIdentityProvider(new KerberosIdentityProviderConfig(model));
    }
}
