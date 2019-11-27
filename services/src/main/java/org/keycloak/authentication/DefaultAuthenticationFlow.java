/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authentication;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.util.AuthenticationFlowHistoryHelper;
import org.keycloak.services.util.AuthenticationFlowURLHelper;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultAuthenticationFlow implements AuthenticationFlow {
    private static final Logger logger = Logger.getLogger(DefaultAuthenticationFlow.class);
    private final List<AuthenticationExecutionModel> executions;
    private final AuthenticationProcessor processor;
    private final AuthenticationFlowModel flow;
    private boolean successful;
    private List<AuthenticationFlowException> afeList = new ArrayList<>();

    public DefaultAuthenticationFlow(AuthenticationProcessor processor, AuthenticationFlowModel flow) {
        this.processor = processor;
        this.flow = flow;
        this.executions = processor.getRealm().getAuthenticationExecutions(flow.getId());
    }

    protected boolean isProcessed(AuthenticationExecutionModel model) {
        if (model.isDisabled()) return true;
        AuthenticationSessionModel.ExecutionStatus status = processor.getAuthenticationSession().getExecutionStatus().get(model.getId());
        if (status == null) return false;
        return status == AuthenticationSessionModel.ExecutionStatus.SUCCESS || status == AuthenticationSessionModel.ExecutionStatus.SKIPPED
                || status == AuthenticationSessionModel.ExecutionStatus.ATTEMPTED
                || status == AuthenticationSessionModel.ExecutionStatus.SETUP_REQUIRED;
    }

    protected Authenticator createAuthenticator(AuthenticatorFactory factory) {
        String display = processor.getAuthenticationSession().getAuthNote(OAuth2Constants.DISPLAY);
        if (display == null) return factory.create(processor.getSession());

        if (factory instanceof DisplayTypeAuthenticatorFactory) {
            Authenticator authenticator = ((DisplayTypeAuthenticatorFactory) factory).createDisplay(processor.getSession(), display);
            if (authenticator != null) return authenticator;
        }
        // todo create a provider for handling lack of display support
        if (OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(display)) {
            processor.getAuthenticationSession().removeAuthNote(OAuth2Constants.DISPLAY);
            throw new AuthenticationFlowException(AuthenticationFlowError.DISPLAY_NOT_SUPPORTED,
                    ConsoleDisplayMode.browserContinue(processor.getSession(), processor.getRefreshUrl(true).toString()));
        } else {
            return factory.create(processor.getSession());
        }
    }

    @Override
    public Response processAction(String actionExecution) {
        logger.debugv("processAction: {0}", actionExecution);

        if (actionExecution == null || actionExecution.isEmpty()) {
            throw new AuthenticationFlowException("action is not in current execution", AuthenticationFlowError.INTERNAL_ERROR);
        }
        AuthenticationExecutionModel model = processor.getRealm().getAuthenticationExecutionById(actionExecution);
        if (model == null) {
            throw new AuthenticationFlowException("action is not in current execution", AuthenticationFlowError.INTERNAL_ERROR);
        }

        MultivaluedMap<String, String> inputData = processor.getRequest().getDecodedFormParameters();
        String authExecId = inputData.getFirst(Constants.AUTHENTICATION_EXECUTION);
        String selectedCredentialId = inputData.getFirst(Constants.CREDENTIAL_ID);

        //check if the user has selected the "back" option
        if (inputData.containsKey("back")) {
            AuthenticationSessionModel authSession = processor.getAuthenticationSession();

            AuthenticationFlowHistoryHelper history = new AuthenticationFlowHistoryHelper(processor);
            if (history.hasAnyExecution()) {

                String executionId = history.pullExecution();
                AuthenticationExecutionModel lastActionExecution = processor.getRealm().getAuthenticationExecutionById(executionId);

                logger.debugf("Moving back to authentication execution '%s'", lastActionExecution.getAuthenticator());

                recursiveClearExecutionStatusOfAllExecutionsAfterOurExecutionInclusive(lastActionExecution);

                Response response = processSingleFlowExecutionModel(lastActionExecution, null, false);
                if (response == null) {
                    processor.getAuthenticationSession().removeAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
                    return processFlow();
                } else return response;
            } else {
                // This normally shouldn't happen as "back" button shouldn't be available on the form. If it is still triggered, we show "pageExpired" page
                new AuthenticationFlowURLHelper(processor.getSession(), processor.getRealm(), processor.getUriInfo())
                        .showPageExpired(authSession);
            }
        }

        // check if the user has switched to a new authentication execution, and if so switch to it.
        if (authExecId != null && !authExecId.isEmpty()) {

            List<AuthenticationSelectionOption> selectionOptions = createAuthenticationSelectionList(model);

            // Check if switch to the requested authentication execution is allowed
            selectionOptions.stream()
                    .filter(authSelectionOption -> authExecId.equals(authSelectionOption.getAuthExecId()))
                    .findFirst()
                    .orElseThrow(() -> new AuthenticationFlowException("Requested authentication execution is not allowed", AuthenticationFlowError.INTERNAL_ERROR)
            );

            model = processor.getRealm().getAuthenticationExecutionById(authExecId);

            // In case that new execution is a flow, we will add the 1st item from the selection (preferred credential) to the history, so when later click "back", we will return to it.
            if (model.isAuthenticatorFlow()) {
                new AuthenticationFlowHistoryHelper(processor).pushExecution(selectionOptions.get(0).getAuthExecId());
            }

            Response response = processSingleFlowExecutionModel(model, selectedCredentialId, false);
            if (response == null) {
                processor.getAuthenticationSession().removeAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
                checkAndValidateParentFlow(model);
                return processFlow();
            } else return response;
        }
        //handle case where execution is a flow
        if (model.isAuthenticatorFlow()) {
            logger.debug("execution is flow");
            AuthenticationFlow authenticationFlow = processor.createFlowExecution(model.getFlowId(), model);
            Response flowChallenge = authenticationFlow.processAction(actionExecution);
            if (flowChallenge == null) {
                checkAndValidateParentFlow(model);
                return processFlow();
            } else {
                processor.getAuthenticationSession().setExecutionStatus(model.getId(), AuthenticationSessionModel.ExecutionStatus.CHALLENGED);
                return flowChallenge;
            }
        }
        //handle normal execution case
        AuthenticatorFactory factory = getAuthenticatorFactory(model);
        Authenticator authenticator = createAuthenticator(factory);
        AuthenticationProcessor.Result result = processor.createAuthenticatorContext(model, authenticator, executions);
        result.setAuthenticationSelections(createAuthenticationSelectionList(model));

        result.setSelectedCredentialId(selectedCredentialId);

        logger.debugv("action: {0}", model.getAuthenticator());
        authenticator.action(result);
        Response response = processResult(result, true);
        if (response == null) {
            processor.getAuthenticationSession().removeAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
            checkAndValidateParentFlow(model);
            return processFlow();
        } else return response;
    }

    /**
     * Clear execution status of targetExecution and also clear execution status of all the executions, which were triggered after this execution.
     * This covers also "flow" executions and executions, which were set automatically
     *
     * @param targetExecution
     */
    private void recursiveClearExecutionStatusOfAllExecutionsAfterOurExecutionInclusive(AuthenticationExecutionModel targetExecution) {
        RealmModel realm = processor.getRealm();
        AuthenticationSessionModel authSession = processor.getAuthenticationSession();

        // Clear execution status of our execution
        authSession.getExecutionStatus().remove(targetExecution.getId());

        // Find all the "sibling" executions after target execution including target execution. For those, we can recursively remove execution status
        recursiveClearExecutionStatusOfAllSiblings(targetExecution);

        // Find the parent flow. If corresponding execution of this parent flow already has "executionStatus" set, we should clear it and also clear
        // the status for all the siblings after that execution
        while (true) {
            AuthenticationFlowModel parentFlow = realm.getAuthenticationFlowById(targetExecution.getParentFlow());
            if (parentFlow.isTopLevel()) {
                return;
            }

            AuthenticationExecutionModel flowExecution = realm.getAuthenticationExecutionByFlowId(parentFlow.getId());
            if (authSession.getExecutionStatus().containsKey(flowExecution.getId())) {
                authSession.getExecutionStatus().remove(flowExecution.getId());
                recursiveClearExecutionStatusOfAllSiblings(flowExecution);
                targetExecution = flowExecution;
            } else {
                return;
            }

        }
    }


    /**
     * Recursively removes the execution status of all "sibling" executions after targetExecution.
     *
     * @param targetExecution
     */
    private void recursiveClearExecutionStatusOfAllSiblings(AuthenticationExecutionModel targetExecution) {
        RealmModel realm = processor.getRealm();
        AuthenticationFlowModel parentFlow = realm.getAuthenticationFlowById(targetExecution.getParentFlow());

        logger.debugf("Recursively clearing executions in flow '%s', which are after execution '%s'", parentFlow.getAlias(), targetExecution.getId());

        List<AuthenticationExecutionModel> siblingExecutions = realm.getAuthenticationExecutions(parentFlow.getId());
        int index = siblingExecutions.indexOf(targetExecution);
        siblingExecutions = siblingExecutions.subList(index + 1, siblingExecutions.size());

        for (AuthenticationExecutionModel authExec : siblingExecutions) {
            recursiveClearExecutionStatus(authExec);
        }
    }


    /**
     * Removes the execution status for an execution. If it is a flow, do the same for all sub-executions.
     *
     * @param execution the execution for which the status must be cleared
     */
    private void recursiveClearExecutionStatus(AuthenticationExecutionModel execution) {
        processor.getAuthenticationSession().getExecutionStatus().remove(execution.getId());
        if (execution.isAuthenticatorFlow()) {
            processor.getRealm().getAuthenticationExecutions(execution.getFlowId()).forEach(this::recursiveClearExecutionStatus);
        }
    }

    /**
     * This method makes sure that the parent flow's corresponding execution is considered successful if its contained
     * executions are successful.
     * The purpose is for when an execution is validated through an action, to make sure its parent flow can be successful
     * when re-evaluation the flow tree.
     *
     * @param model An execution model.
     */
    private void checkAndValidateParentFlow(AuthenticationExecutionModel model) {
        List<AuthenticationExecutionModel> localExecutions = processor.getRealm().getAuthenticationExecutions(model.getParentFlow());
        AuthenticationExecutionModel parentFlowModel = processor.getRealm().getAuthenticationExecutionByFlowId(model.getParentFlow());
        if (parentFlowModel != null &&
                ((model.isRequired() && localExecutions.stream().allMatch(processor::isSuccessful)) ||
                        (model.isAlternative() && localExecutions.stream().anyMatch(processor::isSuccessful)))) {
            processor.getAuthenticationSession().setExecutionStatus(parentFlowModel.getId(), AuthenticationSessionModel.ExecutionStatus.SUCCESS);
        }
    }

    @Override
    public Response processFlow() {
        logger.debug("processFlow");

        //separate flow elements into required and alternative elements
        List<AuthenticationExecutionModel> requiredList = new ArrayList<>();
        List<AuthenticationExecutionModel> alternativeList = new ArrayList<>();

        for (AuthenticationExecutionModel execution : executions) {
            if (isConditionalAuthenticator(execution)) {
                continue;
            } else if (execution.isRequired() || execution.isConditional()) {
                requiredList.add(execution);
            } else if (execution.isAlternative()) {
                alternativeList.add(execution);
            }
        }

        //handle required elements : all required elements need to be executed
        boolean requiredElementsSuccessful = true;
        Iterator<AuthenticationExecutionModel> requiredIListIterator = requiredList.listIterator();
        while (requiredIListIterator.hasNext()) {
            AuthenticationExecutionModel required = requiredIListIterator.next();
            //Conditional flows must be considered disabled (non-existent) if their condition evaluates to false.
            if (required.isConditional() && isConditionalSubflowDisabled(required)) {
                requiredIListIterator.remove();
                continue;
            }
            Response response = processSingleFlowExecutionModel(required, null, true);
            requiredElementsSuccessful &= processor.isSuccessful(required) || isSetupRequired(required);
            if (response != null) {
                return response;
            }
        }

        //Evaluate alternative elements only if there are no required elements. This may also occur if there was only condition elements
        if (requiredList.isEmpty()) {
            //check if an alternative is already successful, in case we are returning in the flow after an action
            if (alternativeList.stream().anyMatch(alternative -> processor.isSuccessful(alternative) || isSetupRequired(alternative))) {
                successful = true;
                return null;
            }

            //handle alternative elements: the first alternative element to be satisfied is enough
            for (AuthenticationExecutionModel alternative : alternativeList) {
                try {
                    Response response = processSingleFlowExecutionModel(alternative, null, true);
                    if (response != null) {
                        return response;
                    }
                    if (processor.isSuccessful(alternative) || isSetupRequired(alternative)) {
                        successful = true;
                        return null;
                    }
                } catch (AuthenticationFlowException afe) {
                    //consuming the error is not good here from an administrative point of view, but the user, since he has alternatives, should be able to go to another alternative and continue
                    afeList.add(afe);
                    processor.getAuthenticationSession().setExecutionStatus(alternative.getId(), AuthenticationSessionModel.ExecutionStatus.ATTEMPTED);
                }
            }
        } else {
            successful = requiredElementsSuccessful;
        }
        return null;
    }

    /**
     * Checks if the conditional subflow passed in parameter is disabled.
     * @param model
     * @return
     */
    private boolean isConditionalSubflowDisabled(AuthenticationExecutionModel model) {
        if (model == null || !model.isAuthenticatorFlow() || !model.isConditional()) {
            return false;
        };
        List<AuthenticationExecutionModel> modelList = processor.getRealm().getAuthenticationExecutions(model.getFlowId());
        List<AuthenticationExecutionModel> conditionalAuthenticatorList = modelList.stream()
                .filter(this::isConditionalAuthenticator)
                .filter(s -> s.isEnabled())
                .collect(Collectors.toList());
        return conditionalAuthenticatorList.isEmpty() || conditionalAuthenticatorList.stream().anyMatch(m-> conditionalNotMatched(m, modelList));
    }

    private boolean isConditionalAuthenticator(AuthenticationExecutionModel model) {
        return !model.isAuthenticatorFlow() && model.getAuthenticator() != null && createAuthenticator(getAuthenticatorFactory(model)) instanceof ConditionalAuthenticator;
    }

    private AuthenticatorFactory getAuthenticatorFactory(AuthenticationExecutionModel model) {
        AuthenticatorFactory factory = (AuthenticatorFactory) processor.getSession().getKeycloakSessionFactory().getProviderFactory(Authenticator.class, model.getAuthenticator());
        if (factory == null) {
            throw new RuntimeException("Unable to find factory for AuthenticatorFactory: " + model.getAuthenticator() + " did you forget to declare it in a META-INF/services file?");
        }
        return factory;
    }

    private boolean conditionalNotMatched(AuthenticationExecutionModel model, List<AuthenticationExecutionModel> executionList) {
        AuthenticatorFactory factory = getAuthenticatorFactory(model);
        ConditionalAuthenticator authenticator = (ConditionalAuthenticator) createAuthenticator(factory);
        AuthenticationProcessor.Result context = processor.createAuthenticatorContext(model, authenticator, executionList);

        return !authenticator.matchCondition(context);
    }

    private boolean isSetupRequired(AuthenticationExecutionModel model) {
        return AuthenticationSessionModel.ExecutionStatus.SETUP_REQUIRED.equals(processor.getAuthenticationSession().getExecutionStatus().get(model.getId()));
    }

    private Response processSingleFlowExecutionModel(AuthenticationExecutionModel model, String selectedCredentialId, boolean calledFromFlow) {
        logger.debugv("check execution: {0} requirement: {1}", model.getAuthenticator(), model.getRequirement());

        if (isProcessed(model)) {
            logger.debug("execution is processed");
            return null;
        }
        //handle case where execution is a flow
        if (model.isAuthenticatorFlow()) {
            logger.debug("execution is flow");
            AuthenticationFlow authenticationFlow = processor.createFlowExecution(model.getFlowId(), model);
            Response flowChallenge = authenticationFlow.processFlow();
            if (flowChallenge == null) {
                if (authenticationFlow.isSuccessful()) {
                    processor.getAuthenticationSession().setExecutionStatus(model.getId(), AuthenticationSessionModel.ExecutionStatus.SUCCESS);
                } else {
                    processor.getAuthenticationSession().setExecutionStatus(model.getId(), AuthenticationSessionModel.ExecutionStatus.FAILED);
                }
                return null;
            } else {
                processor.getAuthenticationSession().setExecutionStatus(model.getId(), AuthenticationSessionModel.ExecutionStatus.CHALLENGED);
                return flowChallenge;
            }
        }

        //handle normal execution case
        AuthenticatorFactory factory = getAuthenticatorFactory(model);
        Authenticator authenticator = createAuthenticator(factory);
        logger.debugv("authenticator: {0}", factory.getId());
        UserModel authUser = processor.getAuthenticationSession().getAuthenticatedUser();

        //If executions are alternative, get the actual execution to show based on user preference
        List<AuthenticationSelectionOption> selectionOptions = createAuthenticationSelectionList(model);
        if (!selectionOptions.isEmpty() && calledFromFlow) {
            List<AuthenticationSelectionOption> finalSelectionOptions = selectionOptions.stream().filter(aso -> !aso.getAuthenticationExecution().isAuthenticatorFlow() && !isProcessed(aso.getAuthenticationExecution())).collect(Collectors.toList());;
            if (finalSelectionOptions.isEmpty()) {
                //move to next
                return null;
            }
            model = finalSelectionOptions.get(0).getAuthenticationExecution();
            factory = (AuthenticatorFactory) processor.getSession().getKeycloakSessionFactory().getProviderFactory(Authenticator.class, model.getAuthenticator());
            if (factory == null) {
                throw new RuntimeException("Unable to find factory for AuthenticatorFactory: " + model.getAuthenticator() + " did you forget to declare it in a META-INF/services file?");
            }
            authenticator = createAuthenticator(factory);
        }
        AuthenticationProcessor.Result context = processor.createAuthenticatorContext(model, authenticator, executions);
        context.setAuthenticationSelections(selectionOptions);
        if (selectedCredentialId != null) {
            context.setSelectedCredentialId(selectedCredentialId);
        } else if (!selectionOptions.isEmpty()) {
            context.setSelectedCredentialId(selectionOptions.get(0).getCredentialId());
        }
        if (authenticator.requiresUser()) {
            if (authUser == null) {
                throw new AuthenticationFlowException("authenticator: " + factory.getId(), AuthenticationFlowError.UNKNOWN_USER);
            }
            if (!authenticator.configuredFor(processor.getSession(), processor.getRealm(), authUser)) {
                if (factory.isUserSetupAllowed() && model.isRequired() && authenticator.areRequiredActionsEnabled(processor.getSession(), processor.getRealm())) {
                    //This means that having even though the user didn't validate the
                    logger.debugv("authenticator SETUP_REQUIRED: {0}", factory.getId());
                    processor.getAuthenticationSession().setExecutionStatus(model.getId(), AuthenticationSessionModel.ExecutionStatus.SETUP_REQUIRED);
                    authenticator.setRequiredActions(processor.getSession(), processor.getRealm(), processor.getAuthenticationSession().getAuthenticatedUser());
                    return null;
                } else {
                    throw new AuthenticationFlowException("authenticator: " + factory.getId(), AuthenticationFlowError.CREDENTIAL_SETUP_REQUIRED);
                }
            }
        }
        logger.debugv("invoke authenticator.authenticate: {0}", factory.getId());
        authenticator.authenticate(context);

        return processResult(context, false);
    }

    /**
     * This method creates the list of authenticators that is presented to the user. For a required execution, this is
     * only the credentials associated to the authenticator, and for an alternative execution, this is all other alternative
     * executions in the flow, including the credentials.
     * <p>
     * In both cases, the credentials take precedence, with the order selected by the user (or his administrator).
     *
     * @param model The current execution model
     * @return an ordered list of the authentication selection options to present the user.
     */
    private List<AuthenticationSelectionOption> createAuthenticationSelectionList(AuthenticationExecutionModel model) {
        List<AuthenticationSelectionOption> authenticationSelectionList = new ArrayList<>();
        if (processor.getAuthenticationSession() != null) {
            Map<String, AuthenticationExecutionModel> typeAuthExecMap = new HashMap<>();
            List<AuthenticationExecutionModel> nonCredentialExecutions = new ArrayList<>();
            if (model.isAlternative()) {
                //get all alternative executions to be able to list their credentials
                List<AuthenticationExecutionModel> alternativeExecutions = processor.getRealm().getAuthenticationExecutions(model.getParentFlow())
                        .stream().filter(AuthenticationExecutionModel::isAlternative).collect(Collectors.toList());
                for (AuthenticationExecutionModel execution : alternativeExecutions) {
                    if (!execution.isAuthenticatorFlow()) {
                        Authenticator localAuthenticator = processor.getSession().getProvider(Authenticator.class, execution.getAuthenticator());
                        if (!(localAuthenticator instanceof CredentialValidator)) {
                            nonCredentialExecutions.add(execution);
                            continue;
                        }
                        CredentialValidator<?> cv = (CredentialValidator<?>) localAuthenticator;
                        typeAuthExecMap.put(cv.getType(processor.getSession()), execution);
                    } else {
                        nonCredentialExecutions.add(execution);
                    }
                }
            } else if (model.isRequired() && !model.isAuthenticatorFlow()) {
                //only get current credentials
                Authenticator authenticator = processor.getSession().getProvider(Authenticator.class, model.getAuthenticator());
                if (authenticator instanceof CredentialValidator) {
                    typeAuthExecMap.put(((CredentialValidator<?>) authenticator).getType(processor.getSession()), model);
                }
            }
            //add credential authenticators in order
            if (processor.getAuthenticationSession().getAuthenticatedUser() != null) {
                List<CredentialModel> credentials = processor.getSession().userCredentialManager()
                        .getStoredCredentials(processor.getRealm(), processor.getAuthenticationSession().getAuthenticatedUser())
                        .stream()
                        .filter(credential -> typeAuthExecMap.containsKey(credential.getType()))
                        .collect(Collectors.toList());

                MultivaluedMap<String, AuthenticationSelectionOption> countAuthSelections = new MultivaluedHashMap<>();

                for (CredentialModel credential : credentials) {
                    AuthenticationSelectionOption authSel = new AuthenticationSelectionOption(typeAuthExecMap.get(credential.getType()), credential);
                    authenticationSelectionList.add(authSel);
                    countAuthSelections.add(credential.getType(), authSel);
                }
                for (Entry<String, List<AuthenticationSelectionOption>> entry : countAuthSelections.entrySet()) {
                    if (entry.getValue().size() == 1) {
                        entry.getValue().get(0).setShowCredentialName(false);
                    }
                }
                //don't show credential type if there's only a single type in the list
                if (countAuthSelections.keySet().size() == 1 && nonCredentialExecutions.isEmpty()) {
                    for (AuthenticationSelectionOption so : authenticationSelectionList) {
                        so.setShowCredentialType(false);
                    }
                }
            }
            //add all other authenticators (including flows)
            for (AuthenticationExecutionModel exec : nonCredentialExecutions) {
                if (exec.isAuthenticatorFlow()) {
                    authenticationSelectionList.add(new AuthenticationSelectionOption(exec,
                            processor.getRealm().getAuthenticationFlowById(exec.getFlowId())));
                } else {
                    authenticationSelectionList.add(new AuthenticationSelectionOption(exec));
                }
            }
        }
        return authenticationSelectionList;
    }


    public Response processResult(AuthenticationProcessor.Result result, boolean isAction) {
        AuthenticationExecutionModel execution = result.getExecution();
        FlowStatus status = result.getStatus();
        switch (status) {
            case SUCCESS:
                logger.debugv("authenticator SUCCESS: {0}", execution.getAuthenticator());
                if (isAction) {
                    new AuthenticationFlowHistoryHelper(processor).pushExecution(execution.getId());
                }

                processor.getAuthenticationSession().setExecutionStatus(execution.getId(), AuthenticationSessionModel.ExecutionStatus.SUCCESS);
                return null;
            case FAILED:
                logger.debugv("authenticator FAILED: {0}", execution.getAuthenticator());
                processor.logFailure();
                processor.getAuthenticationSession().setExecutionStatus(execution.getId(), AuthenticationSessionModel.ExecutionStatus.FAILED);
                if (result.getChallenge() != null) {
                    return sendChallenge(result, execution);
                }
                throw new AuthenticationFlowException(result.getError());
            case FORK:
                logger.debugv("reset browser login from authenticator: {0}", execution.getAuthenticator());
                processor.getAuthenticationSession().setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, execution.getId());
                throw new ForkFlowException(result.getSuccessMessage(), result.getErrorMessage());
            case FORCE_CHALLENGE:
            case CHALLENGE:
                processor.getAuthenticationSession().setExecutionStatus(execution.getId(), AuthenticationSessionModel.ExecutionStatus.CHALLENGED);
                return sendChallenge(result, execution);
            case FAILURE_CHALLENGE:
                logger.debugv("authenticator FAILURE_CHALLENGE: {0}", execution.getAuthenticator());
                processor.logFailure();
                processor.getAuthenticationSession().setExecutionStatus(execution.getId(), AuthenticationSessionModel.ExecutionStatus.CHALLENGED);
                return sendChallenge(result, execution);
            case ATTEMPTED:
                logger.debugv("authenticator ATTEMPTED: {0}", execution.getAuthenticator());
                if (execution.isRequired()) {
                    throw new AuthenticationFlowException(AuthenticationFlowError.INVALID_CREDENTIALS);
                }
                processor.getAuthenticationSession().setExecutionStatus(execution.getId(), AuthenticationSessionModel.ExecutionStatus.ATTEMPTED);
                return null;
            case FLOW_RESET:
                processor.resetFlow();
                return processor.authenticate();
            default:
                logger.debugv("authenticator INTERNAL_ERROR: {0}", execution.getAuthenticator());
                ServicesLogger.LOGGER.unknownResultStatus();
                throw new AuthenticationFlowException(AuthenticationFlowError.INTERNAL_ERROR);
        }
    }

    public Response sendChallenge(AuthenticationProcessor.Result result, AuthenticationExecutionModel execution) {
        processor.getAuthenticationSession().setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, execution.getId());
        return result.getChallenge();
    }

    @Override
    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public List<AuthenticationFlowException> getFlowExceptions(){
        return afeList;
    }
}
