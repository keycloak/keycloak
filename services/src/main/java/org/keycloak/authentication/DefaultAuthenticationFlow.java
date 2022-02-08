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
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        this.executions = processor.getRealm().getAuthenticationExecutionsStream(flow.getId()).collect(Collectors.toList());
    }

    protected boolean isProcessed(AuthenticationExecutionModel model) {
        return isProcessed(processor, model);
    }

    protected static boolean isProcessed(AuthenticationProcessor processor, AuthenticationExecutionModel model) {
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
            throw new AuthenticationFlowException("Execution not found", AuthenticationFlowError.INTERNAL_ERROR);
        }

        if (HttpMethod.POST.equals(processor.getRequest().getHttpMethod())) {
            MultivaluedMap<String, String> inputData = processor.getRequest().getDecodedFormParameters();
            String authExecId = inputData.getFirst(Constants.AUTHENTICATION_EXECUTION);

            // User clicked on "try another way" link
            if (inputData.containsKey("tryAnotherWay")) {
                logger.trace("User clicked on link 'Try Another Way'");

                List<AuthenticationSelectionOption> selectionOptions = createAuthenticationSelectionList(model);

                AuthenticationProcessor.Result result = processor.createAuthenticatorContext(model, null, null);
                result.setAuthenticationSelections(selectionOptions);
                return result.form().createSelectAuthenticator();
            }

            // check if the user has switched to a new authentication execution, and if so switch to it.
            if (authExecId != null && !authExecId.isEmpty()) {

                List<AuthenticationSelectionOption> selectionOptions = createAuthenticationSelectionList(model);

                // Check if switch to the requested authentication execution is allowed
                selectionOptions.stream()
                        .filter(authSelectionOption -> authExecId.equals(authSelectionOption.getAuthExecId()))
                        .findFirst()
                        .orElseThrow(() -> new AuthenticationFlowException("Requested authentication execution is not allowed",
                                AuthenticationFlowError.INTERNAL_ERROR)
                        );

                model = processor.getRealm().getAuthenticationExecutionById(authExecId);

                Response response = processSingleFlowExecutionModel(model, false);
                if (response == null) {
                    return continueAuthenticationAfterSuccessfulAction(model);
                } else
                    return response;
            }
        }

        //handle case where execution is a flow - This can happen during user registration for example
        if (model.isAuthenticatorFlow()) {
            logger.debug("execution is flow");
            AuthenticationFlow authenticationFlow = processor.createFlowExecution(model.getFlowId(), model);
            Response flowChallenge = authenticationFlow.processAction(actionExecution);
            if (flowChallenge == null) {
                checkAndValidateParentFlow(model);
                return processFlow();
            } else {
                setExecutionStatus(model, AuthenticationSessionModel.ExecutionStatus.CHALLENGED);
                return flowChallenge;
            }
        }

        //handle normal execution case
        AuthenticatorFactory factory = getAuthenticatorFactory(model);
        Authenticator authenticator = createAuthenticator(factory);
        AuthenticationProcessor.Result result = processor.createAuthenticatorContext(model, authenticator, executions);
        result.setAuthenticationSelections(createAuthenticationSelectionList(model));

        logger.debugv("action: {0}", model.getAuthenticator());
        authenticator.action(result);
        Response response = processResult(result, true);
        if (response == null) {
            return continueAuthenticationAfterSuccessfulAction(model);
        } else return response;
    }


    /**
     * Called after "actionExecutionModel" execution is finished (Either successful or attempted). Find the next appropriate authentication
     * flow where the authentication should continue and continue with authentication process.
     * The method recursively continues with the parent flow
     * until finally the top flow is processed.
     *
     * @param actionExecutionModel
     * @return Response if some more forms should be displayed during authentication. Null otherwise.
     */
    private Response continueAuthenticationAfterSuccessfulAction(AuthenticationExecutionModel actionExecutionModel) {
        processor.getAuthenticationSession().removeAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);

        String firstUnfinishedParentFlowId = checkAndValidateParentFlow(actionExecutionModel);
        AuthenticationExecutionModel parentFlowExecution = processor.getRealm().getAuthenticationExecutionByFlowId(firstUnfinishedParentFlowId);

        if (parentFlowExecution == null) {
            // This means that 1st unfinished ancestor flow is the top flow. We can just process it from the start
            return processFlow();
        } else {
            Response response = processSingleFlowExecutionModel(parentFlowExecution, false);
            if (response == null) {
                // the parent flow is now the last action that has been executed, continue with that until the top flow is reached
                return continueAuthenticationAfterSuccessfulAction(parentFlowExecution);
            } else {
                return response;
            }
        }
    }


    /**
     * This method makes sure that the parent flow's corresponding execution is considered successful if its contained
     * executions are successful.
     * The purpose is for when an execution is validated through an action, to make sure its parent flow can be successful
     * when re-evaluation the flow tree. If the flow is successful, we will recursively check it's parent flow as well
     *
     * @param model An execution model.
     * @return flowId of the 1st ancestor flow, which is not yet successfully finished and may require some further processing
     */
    private String checkAndValidateParentFlow(AuthenticationExecutionModel model) {
        while (true) {
            AuthenticationExecutionModel parentFlowExecutionModel = processor.getRealm().getAuthenticationExecutionByFlowId(model.getParentFlow());

            if (parentFlowExecutionModel != null) {
                List<AuthenticationExecutionModel> requiredExecutions = new LinkedList<>();
                List<AuthenticationExecutionModel> alternativeExecutions = new LinkedList<>();
                fillListsOfExecutions(processor.getRealm().getAuthenticationExecutionsStream(model.getParentFlow()),
                        requiredExecutions, alternativeExecutions);

                // Note: If we evaluate alternative execution, we will also doublecheck that there are not required elements in same subflow
                if (((model.isRequired() || model.isConditional()) && requiredExecutions.stream().allMatch(processor::isSuccessful)) ||
                        (model.isAlternative() && alternativeExecutions.stream().anyMatch(processor::isSuccessful) && requiredExecutions.isEmpty())) {
                    logger.debugf("Flow '%s' successfully finished after children executions success", logExecutionAlias(parentFlowExecutionModel));
                    setExecutionStatus(parentFlowExecutionModel, AuthenticationSessionModel.ExecutionStatus.SUCCESS);

                    // Flow is successfully finished. Recursively check whether it's parent flow is now successful as well
                    model = parentFlowExecutionModel;
                } else {
                    return model.getParentFlow();
                }
            } else {
                return model.getParentFlow();
            }
        }
    }

    @Override
    public Response processFlow() {
        logger.debugf("processFlow: %s", flow.getAlias());

        //separate flow elements into required and alternative elements
        List<AuthenticationExecutionModel> requiredList = new ArrayList<>();
        List<AuthenticationExecutionModel> alternativeList = new ArrayList<>();

        fillListsOfExecutions(executions.stream(), requiredList, alternativeList);

        //handle required elements : all required elements need to be executed
        boolean requiredElementsSuccessful = true;
        Iterator<AuthenticationExecutionModel> requiredIListIterator = requiredList.listIterator();
        while (requiredIListIterator.hasNext()) {
            AuthenticationExecutionModel required = requiredIListIterator.next();
            //Conditional flows must be considered disabled (non-existent) if their condition evaluates to false.
            //If the flow has been processed before it will not be removed to consider its execution status.
            if (required.isConditional() && !isProcessed(required) && isConditionalSubflowDisabled(required, true)) {
                requiredIListIterator.remove();
                continue;
            }
            Response response = processSingleFlowExecutionModel(required, true);
            requiredElementsSuccessful &= processor.isSuccessful(required) || isSetupRequired(required);
            if (response != null) {
                return response;
            }
            // Some required elements were not successful and did not return response.
            // We can break as we know that the whole subflow would be considered unsuccessful as well
            if (!requiredElementsSuccessful) {
                break;
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
                    Response response = processSingleFlowExecutionModel(alternative, true);
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
                    setExecutionStatus(alternative, AuthenticationSessionModel.ExecutionStatus.ATTEMPTED);
                }
            }
        } else {
            successful = requiredElementsSuccessful;
        }
        return null;
    }


    /**
     * Just iterates over executionsToProcess and fill "requiredList" and "alternativeList" according to it
     */
    void fillListsOfExecutions(Stream<AuthenticationExecutionModel> executionsToProcess, List<AuthenticationExecutionModel> requiredList, List<AuthenticationExecutionModel> alternativeList) {
        executionsToProcess
                .filter(((Predicate<AuthenticationExecutionModel>) this::isConditionalAuthenticator).negate())
                .forEachOrdered(execution -> {
                    if (execution.isRequired() || execution.isConditional()) {
                        requiredList.add(execution);
                    } else if (execution.isAlternative()) {
                        alternativeList.add(execution);
                    }
                });

        if (!requiredList.isEmpty() && !alternativeList.isEmpty()) {
            List<String> alternativeIds = alternativeList.stream()
                    .map(AuthenticationExecutionModel::getAuthenticator)
                    .collect(Collectors.toList());

            logger.warnf("REQUIRED and ALTERNATIVE elements at same level! Those alternative executions will be ignored: %s", alternativeIds);
            alternativeList.clear();
        }
    }


    /**
     * Checks if the conditional subflow passed in parameter is disabled.
     * @param model
     * @param storeResult whether to store the result of the conditional evaluations
     * @return
     */
    boolean isConditionalSubflowDisabled(AuthenticationExecutionModel model, boolean storeResult) {
        if (model == null || !model.isAuthenticatorFlow() || !model.isConditional()) {
            return false;
        };
        List<AuthenticationExecutionModel> modelList = processor.getRealm()
                .getAuthenticationExecutionsStream(model.getFlowId()).collect(Collectors.toList());
        List<AuthenticationExecutionModel> conditionalAuthenticatorList = modelList.stream()
                .filter(this::isConditionalAuthenticator)
                .filter(s -> s.isEnabled())
                .collect(Collectors.toList());
        return conditionalAuthenticatorList.isEmpty() || conditionalAuthenticatorList.stream()
                .anyMatch(m -> conditionalNotMatched(m, modelList, storeResult));
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

    private boolean conditionalNotMatched(AuthenticationExecutionModel model, List<AuthenticationExecutionModel> executionList, boolean storeResult) {
        AuthenticatorFactory factory = getAuthenticatorFactory(model);
        ConditionalAuthenticator authenticator = (ConditionalAuthenticator) createAuthenticator(factory);
        AuthenticationProcessor.Result context = processor.createAuthenticatorContext(model, authenticator, executionList);

        boolean matchCondition;

        // Retrieve previous evaluation result if any, else evaluate and store result for future re-evaluation
        if (processor.isEvaluatedTrue(model)) {
            matchCondition = true;
        } else if (processor.isEvaluatedFalse(model)) {
            matchCondition = false;
        } else {
            matchCondition = authenticator.matchCondition(context);
            if (storeResult) {
                setExecutionStatus(model,
                        matchCondition ? AuthenticationSessionModel.ExecutionStatus.EVALUATED_TRUE : AuthenticationSessionModel.ExecutionStatus.EVALUATED_FALSE);
            }
        }

        return !matchCondition;
    }

    private boolean isSetupRequired(AuthenticationExecutionModel model) {
        return AuthenticationSessionModel.ExecutionStatus.SETUP_REQUIRED.equals(processor.getAuthenticationSession().getExecutionStatus().get(model.getId()));
    }


    private Response processSingleFlowExecutionModel(AuthenticationExecutionModel model, boolean calledFromFlow) {
        logger.debugf("check execution: '%s', requirement: '%s'", logExecutionAlias(model), model.getRequirement());

        if (isProcessed(model)) {
            logger.debugf("execution '%s' is processed", logExecutionAlias(model));
            return null;
        }
        //handle case where execution is a flow
        if (model.isAuthenticatorFlow()) {
            AuthenticationFlow authenticationFlow = processor.createFlowExecution(model.getFlowId(), model);
            Response flowChallenge = authenticationFlow.processFlow();
            if (flowChallenge == null) {
                if (authenticationFlow.isSuccessful()) {
                    logger.debugf("Flow '%s' successfully finished", logExecutionAlias(model));
                    setExecutionStatus(model, AuthenticationSessionModel.ExecutionStatus.SUCCESS);
                } else {
                    logger.debugf("Flow '%s' failed", logExecutionAlias(model));
                    setExecutionStatus(model, AuthenticationSessionModel.ExecutionStatus.FAILED);
                }
                return null;
            } else {
                setExecutionStatus(model, AuthenticationSessionModel.ExecutionStatus.CHALLENGED);
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
            List<AuthenticationSelectionOption> finalSelectionOptions = selectionOptions.stream().filter(aso -> !aso.getAuthenticationExecution().isAuthenticatorFlow() && !isProcessed(aso.getAuthenticationExecution())).collect(Collectors.toList());
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

        if (authenticator.requiresUser()) {
            if (authUser == null) {
                throw new AuthenticationFlowException("authenticator: " + factory.getId(), AuthenticationFlowError.UNKNOWN_USER);
            }
            if (!authenticator.configuredFor(processor.getSession(), processor.getRealm(), authUser)) {
                if (factory.isUserSetupAllowed() && model.isRequired() && authenticator.areRequiredActionsEnabled(processor.getSession(), processor.getRealm())) {
                    //This means that having even though the user didn't validate the
                    logger.debugv("authenticator SETUP_REQUIRED: {0}", factory.getId());
                    setExecutionStatus(model, AuthenticationSessionModel.ExecutionStatus.SETUP_REQUIRED);
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

    // Used for debugging purpose only. Log alias of authenticator (for non-flow executions) or alias of authenticationFlow (for flow executions)
    private String logExecutionAlias(AuthenticationExecutionModel executionModel) {
        if (executionModel.isAuthenticatorFlow()) {
            // Resolve authenticationFlow model in case of debug logging. Otherwise don't lookup flowModel just because of logging and return only flowId
            if (logger.isDebugEnabled()) {
                AuthenticationFlowModel flowModel = processor.getRealm().getAuthenticationFlowById(executionModel.getFlowId());
                if (flowModel != null) {
                    return flowModel.getAlias() + " flow";
                }
            }
            return executionModel.getFlowId() + " flow";
        } else {
            return executionModel.getAuthenticator();
        }
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
        return AuthenticationSelectionResolver.createAuthenticationSelectionList(processor, model);
    }


    public Response processResult(AuthenticationProcessor.Result result, boolean isAction) {
        AuthenticationExecutionModel execution = result.getExecution();
        FlowStatus status = result.getStatus();
        switch (status) {
            case SUCCESS:
                logger.debugv("authenticator SUCCESS: {0}", execution.getAuthenticator());
                setExecutionStatus(execution, AuthenticationSessionModel.ExecutionStatus.SUCCESS);
                return null;
            case FAILED:
                logger.debugv("authenticator FAILED: {0}", execution.getAuthenticator());
                processor.logFailure();
                setExecutionStatus(execution, AuthenticationSessionModel.ExecutionStatus.FAILED);
                if (result.getChallenge() != null) {
                    return sendChallenge(result, execution);
                }
                throw new AuthenticationFlowException(result.getError(), result.getEventDetails(), result.getUserErrorMessage());
            case FORK:
                logger.debugv("reset browser login from authenticator: {0}", execution.getAuthenticator());
                processor.getAuthenticationSession().setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, execution.getId());
                throw new ForkFlowException(result.getSuccessMessage(), result.getErrorMessage());
            case FORCE_CHALLENGE:
            case CHALLENGE:
                setExecutionStatus(execution, AuthenticationSessionModel.ExecutionStatus.CHALLENGED);
                return sendChallenge(result, execution);
            case FAILURE_CHALLENGE:
                logger.debugv("authenticator FAILURE_CHALLENGE: {0}", execution.getAuthenticator());
                processor.logFailure();
                setExecutionStatus(execution, AuthenticationSessionModel.ExecutionStatus.CHALLENGED);
                return sendChallenge(result, execution);
            case ATTEMPTED:
                logger.debugv("authenticator ATTEMPTED: {0}", execution.getAuthenticator());
                setExecutionStatus(execution, AuthenticationSessionModel.ExecutionStatus.ATTEMPTED);
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

    private void setExecutionStatus(AuthenticationExecutionModel authExecutionModel, CommonClientSessionModel.ExecutionStatus status) {
        this.processor.getAuthenticationSession().setExecutionStatus(authExecutionModel.getId(), status);

        if (authExecutionModel.isAuthenticatorFlow() && status == CommonClientSessionModel.ExecutionStatus.SUCCESS) {
            // Trigger callbacks after flow was successfully finished
            processor.getRealm().getAuthenticationExecutionsStream(authExecutionModel.getFlowId()).forEach(this::checkAuthCallback);
        }
    }

    private void checkAuthCallback(AuthenticationExecutionModel execution) {
        // We will trigger the callback just if particular authenticator, which corresponds to this callback, was finished with SUCCESS or condition was evaluated to true
        CommonClientSessionModel.ExecutionStatus executionStatus = processor.getAuthenticationSession().getExecutionStatus().get(execution.getId());
        if (executionStatus == CommonClientSessionModel.ExecutionStatus.SUCCESS || executionStatus == CommonClientSessionModel.ExecutionStatus.EVALUATED_TRUE) {
            if (!execution.isAuthenticatorFlow()) {
                AuthenticatorFactory authFactory = getAuthenticatorFactory(execution);
                if (authFactory instanceof AuthenticationFlowCallbackFactory) {
                    AuthenticationFlowCallback authCallback = (AuthenticationFlowCallback) createAuthenticator(authFactory);
                    logger.tracef("Will trigger callback '%s' after successful finish of the flow '%s'", authFactory.getId(), execution.getParentFlow());
                    authCallback.onParentFlowSuccess(processor.createAuthenticatorContext(execution, authCallback, null)); // no need to have executions filled
                }
            }
        }
    }
}
