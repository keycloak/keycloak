package org.keycloak.device;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.account.DeviceRepresentation;

import com.github.benmanes.caffeine.cache.LoadingCache;
import org.jboss.logging.Logger;
import ua_parser.Client;

public class DeviceRepresentationProviderImpl implements DeviceRepresentationProvider {
    private static final Logger logger = Logger.getLogger(DeviceActivityManager.class);
    private static final int USER_AGENT_MAX_LENGTH = 512;

    private final LoadingCache<String, Client> cache;
    private final KeycloakSession session;

    DeviceRepresentationProviderImpl(KeycloakSession session, LoadingCache<String, Client> cache) {
        this.session = session;
        this.cache = cache;
    }

    @Override
    public DeviceRepresentation deviceRepresentation() {
        KeycloakContext context = session.getContext();

        if (context.getRequestHeaders() == null) {
            return null;
        }

        String userAgent = context.getRequestHeaders().getHeaderString(HttpHeaders.USER_AGENT);

        if (userAgent == null) {
            return null;
        }

        if (userAgent.length() > USER_AGENT_MAX_LENGTH) {
            logger.warn("Ignoring User-Agent header. Length is above the permitted: " + USER_AGENT_MAX_LENGTH);
            return null;
        }

        DeviceRepresentation current;
        try {
            Client client = cache.get(userAgent);
            // To avoid IDEA warning about NullPointerException
            // It should never be null as the parser never returns a null client.
            assert client != null;
            current = new DeviceRepresentation();

            current.setDevice(client.device.family);

            String browserVersion = client.userAgent.major;

            if (client.userAgent.minor != null) {
                browserVersion += "." + client.userAgent.minor;
            }

            if (client.userAgent.patch != null) {
                browserVersion += "." + client.userAgent.patch;
            }

            if (browserVersion == null) {
                browserVersion = DeviceRepresentation.UNKNOWN;
            }

            current.setBrowser(client.userAgent.family, browserVersion);
            current.setOs(client.os.family);

            String osVersion = client.os.major;

            if (client.os.minor != null) {
                osVersion += "." + client.os.minor;
            }

            if (client.os.patch != null) {
                osVersion += "." + client.os.patch;
            }

            if (client.os.patchMinor != null) {
                osVersion += "." + client.os.patchMinor;
            }

            current.setOsVersion(osVersion);
            current.setIpAddress(context.getConnection().getRemoteHost());
            current.setMobile(userAgent.toLowerCase().contains("mobile"));
            return current;
        } catch (Exception cause) {
            logger.error("Failed to create device info from user agent header", cause);
            return null;
        }
    }
}
