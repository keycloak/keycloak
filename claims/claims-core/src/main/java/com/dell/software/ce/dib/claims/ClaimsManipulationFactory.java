package com.dell.software.ce.dib.claims;

import org.keycloak.provider.ProviderFactory;

public interface ClaimsManipulationFactory<T extends ClaimsManipulation> extends ProviderFactory<T> {

    /**
     * <p>A friendly name for this factory.</p>
     *
     * @return
     */
    String getName();

    T create();
}