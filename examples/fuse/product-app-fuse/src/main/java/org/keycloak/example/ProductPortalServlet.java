package org.keycloak.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.example.ws.Person;
import org.keycloak.example.ws.UnknownPersonFault;
import org.keycloak.util.KeycloakUriBuilder;

/**
 * Servlet for receiving informations about products from backend JAXWS service. Actually it's about "persons" not "products" :)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ProductPortalServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");

        // Send jaxws request
        PrintWriter out = resp.getWriter();
        out.println("<html><head><title>Product Portal Page</title></head><body>");

        String logoutUri = KeycloakUriBuilder.fromUri("http://localhost:8080/auth").path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
                .queryParam("redirect_uri", "http://localhost:8181/product-portal").build("demo").toString();
        String acctUri = KeycloakUriBuilder.fromUri("http://localhost:8080/auth").path(ServiceUrlConstants.ACCOUNT_SERVICE_PATH)
                .queryParam("referrer", "product-portal").build("demo").toString();

        out.println("<p>Goto: <a href=\"/customer-portal\">customers</a> | <a href=\"" + logoutUri + "\">logout</a> | <a href=\"" + acctUri + "\">manage acct</a></p>");
        out.println("Servlet User Principal <b>" + req.getUserPrincipal() + "</b> made this request.");

        String unsecuredWsClientResponse = sendWsReq(req, false);
        String securedWsClientResponse = sendWsReq(req, true);

        out.println("<p>Person with ID 1 - unsecured request: <b>" + unsecuredWsClientResponse + "</b></p>");
        out.println("<p>Person with ID 1 - secured request: <b>" + securedWsClientResponse + "</b></p>");
        out.println("</body></html>");
        out.flush();
        out.close();
    }

    private String sendWsReq(HttpServletRequest req, boolean secured) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(Person.class);
        factory.setAddress("http://localhost:8282/PersonServiceCF");

        Person simpleClient = (Person)factory.create();
        java.lang.String _getPerson_personIdVal = "1";
        javax.xml.ws.Holder<java.lang.String> _getPerson_personId = new javax.xml.ws.Holder<java.lang.String>(_getPerson_personIdVal);
        javax.xml.ws.Holder<java.lang.String> _getPerson_ssn = new javax.xml.ws.Holder<java.lang.String>();
        javax.xml.ws.Holder<java.lang.String> _getPerson_name = new javax.xml.ws.Holder<java.lang.String>();

        // Attach Authorization header
        if (secured) {
            Client clientProxy = ClientProxy.getClient(simpleClient);

            KeycloakSecurityContext session = (KeycloakSecurityContext) req.getAttribute(KeycloakSecurityContext.class.getName());
            Map<String, List<String>> headers = new HashMap<String, List<String>>();
            headers.put("Authorization", Arrays.asList("Bearer " + session.getTokenString()));

            clientProxy.getRequestContext().put(Message.PROTOCOL_HEADERS, headers);
        }

        try {
            simpleClient.getPerson(_getPerson_personId, _getPerson_ssn, _getPerson_name);
            return String.format("Person received: id=%s, name=%s, ssn=%s", _getPerson_personId.value, _getPerson_name.value, _getPerson_ssn.value);
        } catch (UnknownPersonFault upf) {
            return "UnknownPersonFault has occurred. Details: " + upf.toString();
        } catch (WebServiceException wse) {
            String error = "Can't receive person. Reason: " + wse.getMessage();
            if (wse.getCause() != null) {
                Throwable cause = wse.getCause();
                error = error + " Details: " + cause.getClass().getName() + ": " + cause.getMessage();
            }
            return error;
        }
    }
}
