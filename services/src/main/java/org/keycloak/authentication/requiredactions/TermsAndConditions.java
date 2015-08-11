package org.keycloak.authentication.requiredactions;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.AbstractFormRequiredAction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TermsAndConditions extends AbstractFormRequiredAction implements RequiredActionFactory {

    public static final String PROVIDER_ID = "terms_and_conditions";

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }


    @Override
    public String getProviderId() {
        return getId();
    }



    @Override
    public void evaluateTriggers(RequiredActionContext context) {

    }


    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Response challenge =  form(context).createForm("terms.ftl");
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        if (context.getHttpRequest().getDecodedFormParameters().containsKey("cancel")) {
            context.failure();
            return;
        }
        context.success();

    }

    @Override
    public String getDisplayText() {
        return "Terms and Conditions";
    }

}
