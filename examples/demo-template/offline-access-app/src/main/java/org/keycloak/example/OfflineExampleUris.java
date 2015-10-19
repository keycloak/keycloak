package org.keycloak.example;

import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.common.util.KeycloakUriBuilder;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineExampleUris {


    public static final String LOGIN_CLASSIC = "/offline-access-portal/app/login";


    public static final String LOGIN_WITH_OFFLINE_TOKEN = "/offline-access-portal/app/login?scope=offline_access";


    public static final String LOAD_CUSTOMERS = "/offline-access-portal/app/loadCustomers";


    public static final String ACCOUNT_MGMT = KeycloakUriBuilder.fromUri("/auth").path(ServiceUrlConstants.ACCOUNT_SERVICE_PATH + "/applications")
            .queryParam("referrer", "offline-access-portal").build("demo").toString();


    public static final String LOGOUT = KeycloakUriBuilder.fromUri("/auth").path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
            .queryParam("redirect_uri", "/offline-access-portal").build("demo").toString();
}
