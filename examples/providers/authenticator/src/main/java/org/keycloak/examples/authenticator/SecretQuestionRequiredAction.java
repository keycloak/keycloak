package org.keycloak.examples.authenticator;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.UserCredentialValueModel;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SecretQuestionRequiredAction implements RequiredActionProvider {
    public static final String PROVIDER_ID = "secret_question_config";

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Response challenge = context.form().createForm("secret-question-config.ftl");
        context.challenge(challenge);

    }

    @Override
    public void processAction(RequiredActionContext context) {
        String answer = (context.getHttpRequest().getDecodedFormParameters().getFirst("secret_answer"));
        UserCredentialValueModel model = new UserCredentialValueModel();
        model.setValue(answer);
        model.setType(SecretQuestionAuthenticator.CREDENTIAL_TYPE);
        context.getUser().updateCredentialDirectly(model);
        context.success();
    }

    @Override
    public void close() {

    }
}
