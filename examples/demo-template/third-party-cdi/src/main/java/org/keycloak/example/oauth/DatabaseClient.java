package org.keycloak.example.oauth;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jboss.logging.Logger;
import org.keycloak.servlet.ServletOAuthClient;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.UriUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @version $Revision: 1 $
 */
@ApplicationScoped
@Named("databaseClient")
public class DatabaseClient {

    @Inject
    @ServletRequestQualifier
    private HttpServletRequest request;

    @Inject
    private HttpServletResponse response;

    @Inject
    private FacesContext facesContext;

    @Inject
    private ServletOAuthClient oauthClient;

    @Inject
    private UserData userData;

    private static final Logger logger = Logger.getLogger(DatabaseClient.class);

    public void retrieveAccessToken() {
        try {
            oauthClient.redirectRelative("client.jsf", request, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class TypedList extends ArrayList<String> {}

    public void sendCustomersRequest() {
        List<String> customers = sendRequestToDBApplication(getBaseUrl() + "/database/customers");
        userData.setCustomers(customers);
    }

    public void sendProductsRequest() {
        List<String> products = sendRequestToDBApplication(getBaseUrl() + "/database/products");
        userData.setProducts(products);
    }

    protected List<String> sendRequestToDBApplication(String dbUri) {
        HttpClient client = oauthClient.getClient();
        HttpGet get = new HttpGet(dbUri);
        try {

            if (userData.isHasAccessToken()) {
                get.addHeader("Authorization", "Bearer " + userData.getAccessToken());
            }

            HttpResponse response = client.execute(get);
            switch (response.getStatusLine().getStatusCode()) {
                case 200: HttpEntity entity = response.getEntity();
                    InputStream is = entity.getContent();
                    try {
                        return JsonSerialization.readValue(is, TypedList.class);
                    } finally {
                        is.close();
                    }
                case 401: facesContext.addMessage(null, new FacesMessage("Status: 401. Request not authenticated! You need to retrieve access token first."));
                    break;
                case 403: facesContext.addMessage(null, new FacesMessage("Status: 403. Access token has insufficient privileges"));
                    break;
                default: facesContext.addMessage(null, new FacesMessage("Status: " + response.getStatusLine() + ". Not able to retrieve data. See log for details"));
                    logger.warn("Error occured. Status: " + response.getStatusLine());
            }

            return null;
        } catch (IOException e) {
            e.printStackTrace();
            facesContext.addMessage(null, new FacesMessage("Unknown error. See log for details"));
            return null;
        }
    }

    public String getBaseUrl() {
        switch (oauthClient.getRelativeUrlsUsed()) {
            case ALL_REQUESTS:
                // Resolve baseURI from the request
                return UriUtils.getOrigin(request.getRequestURL().toString());
            case BROWSER_ONLY:
                // Resolve baseURI from the codeURL (This is already non-relative and based on our hostname)
                return UriUtils.getOrigin(oauthClient.getTokenUrl());
            case NEVER:
                return "";
            default:
                return "";
        }
    }

}
