package org.keycloak.protocol.saml.profile.ecp;

import org.keycloak.Config;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlProtocolFactory;
import org.keycloak.protocol.saml.profile.ecp.util.Soap;
import org.keycloak.protocol.saml.profile.ecp.util.Soap.SoapMessageBuilder;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.services.managers.AuthenticationManager;
import org.w3c.dom.Document;

import javax.ws.rs.core.Response;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeaderElement;
import java.io.IOException;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class SamlEcpProfileProtocolFactory extends SamlProtocolFactory {

    static final String ID = "saml-ecp-profile";

    private static final String NS_PREFIX_PROFILE_ECP = "ecp";
    private static final String NS_PREFIX_SAML_PROTOCOL = "samlp";
    private static final String NS_PREFIX_SAML_ASSERTION = "saml";

    @Override
    public Object createProtocolEndpoint(RealmModel realm, EventBuilder event, AuthenticationManager authManager) {
        return new SamlEcpProfileService(realm, event, authManager);
    }

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new SamlProtocol() {
            // method created to send a SOAP Binding response instead of a HTTP POST response
            @Override
            protected Response buildAuthenticatedResponse(ClientSessionModel clientSession, String redirectUri, Document samlDocument, JaxrsSAML2BindingBuilder bindingBuilder) throws ConfigurationException, ProcessingException, IOException {
                Document document = bindingBuilder.postBinding(samlDocument).getDocument();

                try {
                    SoapMessageBuilder messageBuilder = Soap.createMessage()
                            .addNamespace(NS_PREFIX_SAML_ASSERTION, JBossSAMLURIConstants.ASSERTION_NSURI.get())
                            .addNamespace(NS_PREFIX_SAML_PROTOCOL, JBossSAMLURIConstants.PROTOCOL_NSURI.get())
                            .addNamespace(NS_PREFIX_PROFILE_ECP, JBossSAMLURIConstants.ECP_PROFILE.get());

                    createEcpResponseHeader(redirectUri, messageBuilder);
                    createRequestAuthenticatedHeader(clientSession, messageBuilder);

                    messageBuilder.addToBody(document);

                    return messageBuilder.build();
                } catch (Exception e) {
                    throw new RuntimeException("Error while creating SAML response.", e);
                }
            }

            private void createRequestAuthenticatedHeader(ClientSessionModel clientSession, SoapMessageBuilder messageBuilder) {
                ClientModel client = clientSession.getClient();

                if ("true".equals(client.getAttribute(SamlProtocol.SAML_CLIENT_SIGNATURE_ATTRIBUTE))) {
                    SOAPHeaderElement ecpRequestAuthenticated = messageBuilder.addHeader(JBossSAMLConstants.REQUEST_AUTHENTICATED.get(), NS_PREFIX_PROFILE_ECP);

                    ecpRequestAuthenticated.setMustUnderstand(true);
                    ecpRequestAuthenticated.setActor("http://schemas.xmlsoap.org/soap/actor/next");
                }
            }

            private void createEcpResponseHeader(String redirectUri, SoapMessageBuilder messageBuilder) throws SOAPException {
                SOAPHeaderElement ecpResponseHeader = messageBuilder.addHeader(JBossSAMLConstants.RESPONSE.get(), NS_PREFIX_PROFILE_ECP);

                ecpResponseHeader.setMustUnderstand(true);
                ecpResponseHeader.setActor("http://schemas.xmlsoap.org/soap/actor/next");
                ecpResponseHeader.addAttribute(messageBuilder.createName(JBossSAMLConstants.ASSERTION_CONSUMER_SERVICE_URL.get()), redirectUri);
            }

            @Override
            protected Response buildErrorResponse(ClientSessionModel clientSession, JaxrsSAML2BindingBuilder binding, Document document) throws ConfigurationException, ProcessingException, IOException {
                return Soap.createMessage().addToBody(document).build();
            }

            @Override
            protected Response buildLogoutResponse(UserSessionModel userSession, String logoutBindingUri, SAML2LogoutResponseBuilder builder, JaxrsSAML2BindingBuilder binding) throws ConfigurationException, ProcessingException, IOException {
                return Soap.createFault().reason("Logout not supported.").build();
            }
        }.setSession(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public String getId() {
        return ID;
    }
}
