package org.keycloak.authentication.otp;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

import com.google.auto.service.AutoService;

@AutoService(Spi.class)
public class OTPApplicationSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "otp-application";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return OTPApplicationProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return OTPApplicationProviderFactory.class;
    }

}
