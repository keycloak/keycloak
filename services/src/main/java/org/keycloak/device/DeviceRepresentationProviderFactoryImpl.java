package org.keycloak.device;

import org.keycloak.models.KeycloakSession;
import ua_parser.Parser;

public class DeviceRepresentationProviderFactoryImpl implements DeviceRepresentationProviderFactory {

    private volatile Parser parser;

    public static final String PROVIDER_ID = "deviceRepresentation";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public DeviceRepresentationProvider create(KeycloakSession session) {
        lazyInit(session);
        return new DeviceRepresentationProviderImpl(session, parser);
    }

    private void lazyInit(KeycloakSession session) {
        if(parser == null) {
            synchronized (this) {
                parser = new Parser();
            }
        }
    }
}
