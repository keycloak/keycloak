import Resource from "./resource.js";
import type RequiredActionProviderRepresentation from "../defs/requiredActionProviderRepresentation.js";
import type { KeycloakAdminClient } from "../client.js";
import type AuthenticationExecutionInfoRepresentation from "../defs/authenticationExecutionInfoRepresentation.js";
import type AuthenticationFlowRepresentation from "../defs/authenticationFlowRepresentation.js";
import type AuthenticatorConfigRepresentation from "../defs/authenticatorConfigRepresentation.js";
import type { AuthenticationProviderRepresentation } from "../defs/authenticatorConfigRepresentation.js";
import type AuthenticatorConfigInfoRepresentation from "../defs/authenticatorConfigInfoRepresentation.js";
import type RequiredActionProviderSimpleRepresentation from "../defs/requiredActionProviderSimpleRepresentation.js";
import type RequiredActionConfigInfoRepresentation from "../defs/requiredActionConfigInfoRepresentation.js";
import type RequiredActionConfigRepresentation from "../defs/requiredActionConfigRepresentation.js";

export class AuthenticationManagement extends Resource<{ realm?: string }> {
  /**
   * Authentication Management
   * https://www.keycloak.org/docs-api/8.0/rest-api/index.html#_authentication_management_resource
   */

  //   Register a new required action
  public registerRequiredAction = this.makeRequest<Record<string, any>>({
    method: "POST",
    path: "/register-required-action",
  });

  // Get required actions. Returns a list of required actions.
  public getRequiredActions = this.makeRequest<
    void,
    RequiredActionProviderRepresentation[]
  >({
    method: "GET",
    path: "/required-actions",
  });

  // Get required action for alias
  public getRequiredActionForAlias = this.makeRequest<{
    alias: string;
  }>({
    method: "GET",
    path: "/required-actions/{alias}",
    urlParamKeys: ["alias"],
    catchNotFound: true,
  });

  public getClientAuthenticatorProviders = this.makeRequest<
    void,
    AuthenticationProviderRepresentation[]
  >({
    method: "GET",
    path: "/client-authenticator-providers",
  });

  public getAuthenticatorProviders = this.makeRequest<
    void,
    AuthenticationProviderRepresentation[]
  >({
    method: "GET",
    path: "/authenticator-providers",
  });

  public getFormActionProviders = this.makeRequest<
    void,
    AuthenticationProviderRepresentation[]
  >({
    method: "GET",
    path: "/form-action-providers",
  });

  // Update required action
  public updateRequiredAction = this.makeUpdateRequest<
    { alias: string },
    RequiredActionProviderRepresentation,
    void
  >({
    method: "PUT",
    path: "/required-actions/{alias}",
    urlParamKeys: ["alias"],
  });

  // Delete required action
  public deleteRequiredAction = this.makeRequest<{ alias: string }, void>({
    method: "DELETE",
    path: "/required-actions/{alias}",
    urlParamKeys: ["alias"],
  });

  // Lower required action’s priority
  public lowerRequiredActionPriority = this.makeRequest<{
    alias: string;
  }>({
    method: "POST",
    path: "/required-actions/{alias}/lower-priority",
    urlParamKeys: ["alias"],
  });

  // Raise required action’s priority
  public raiseRequiredActionPriority = this.makeRequest<{
    alias: string;
  }>({
    method: "POST",
    path: "/required-actions/{alias}/raise-priority",
    urlParamKeys: ["alias"],
  });

  // Get unregistered required actions Returns a list of unregistered required actions.
  public getUnregisteredRequiredActions = this.makeRequest<
    void,
    RequiredActionProviderSimpleRepresentation[]
  >({
    method: "GET",
    path: "/unregistered-required-actions",
  });

  public getFlows = this.makeRequest<{}, AuthenticationFlowRepresentation[]>({
    method: "GET",
    path: "/flows",
  });

  public getFlow = this.makeRequest<
    { flowId: string },
    AuthenticationFlowRepresentation
  >({
    method: "GET",
    path: "/flows/{flowId}",
    urlParamKeys: ["flowId"],
  });

  public getFormProviders = this.makeRequest<
    void,
    AuthenticationProviderRepresentation[]
  >({
    method: "GET",
    path: "/form-providers",
  });

  public createFlow = this.makeRequest<
    AuthenticationFlowRepresentation,
    AuthenticationFlowRepresentation
  >({
    method: "POST",
    path: "/flows",
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public copyFlow = this.makeRequest<{ flow: string; newName: string }>({
    method: "POST",
    path: "/flows/{flow}/copy",
    urlParamKeys: ["flow"],
  });

  public deleteFlow = this.makeRequest<{ flowId: string }>({
    method: "DELETE",
    path: "/flows/{flowId}",
    urlParamKeys: ["flowId"],
  });

  public updateFlow = this.makeUpdateRequest<
    { flowId: string },
    AuthenticationFlowRepresentation
  >({
    method: "PUT",
    path: "/flows/{flowId}",
    urlParamKeys: ["flowId"],
  });

  public getExecutions = this.makeRequest<
    { flow: string },
    AuthenticationExecutionInfoRepresentation[]
  >({
    method: "GET",
    path: "/flows/{flow}/executions",
    urlParamKeys: ["flow"],
  });

  public addExecution = this.makeUpdateRequest<
    { flow: string },
    AuthenticationExecutionInfoRepresentation
  >({
    method: "POST",
    path: "/flows/{flow}/executions",
    urlParamKeys: ["flow"],
  });

  public addExecutionToFlow = this.makeRequest<
    { flow: string; provider: string },
    AuthenticationExecutionInfoRepresentation
  >({
    method: "POST",
    path: "/flows/{flow}/executions/execution",
    urlParamKeys: ["flow"],
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public addFlowToFlow = this.makeRequest<
    {
      flow: string;
      alias: string;
      type: string;
      provider: string;
      description: string;
    },
    AuthenticationFlowRepresentation
  >({
    method: "POST",
    path: "/flows/{flow}/executions/flow",
    urlParamKeys: ["flow"],
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public updateExecution = this.makeUpdateRequest<
    { flow: string },
    AuthenticationExecutionInfoRepresentation
  >({
    method: "PUT",
    path: "/flows/{flow}/executions",
    urlParamKeys: ["flow"],
  });

  public delExecution = this.makeRequest<{ id: string }>({
    method: "DELETE",
    path: "/executions/{id}",
    urlParamKeys: ["id"],
  });

  public lowerPriorityExecution = this.makeRequest<{ id: string }>({
    method: "POST",
    path: "/executions/{id}/lower-priority",
    urlParamKeys: ["id"],
  });

  public raisePriorityExecution = this.makeRequest<{ id: string }>({
    method: "POST",
    path: "/executions/{id}/raise-priority",
    urlParamKeys: ["id"],
  });

  // Get required actions provider's configuration description
  public getRequiredActionConfigDescription = this.makeRequest<
    { alias: string },
    RequiredActionConfigInfoRepresentation
  >({
    method: "GET",
    path: "/required-actions/{alias}/config-description",
    urlParamKeys: ["alias"],
  });

  // Get the configuration of the RequiredAction provider in the current Realm.
  public getRequiredActionConfig = this.makeRequest<
    { alias: string },
    RequiredActionConfigRepresentation
  >({
    method: "GET",
    path: "/required-actions/{alias}/config",
    urlParamKeys: ["alias"],
  });

  // Remove the configuration from the RequiredAction provider in the current Realm.
  public removeRequiredActionConfig = this.makeRequest<{ alias: string }>({
    method: "DELETE",
    path: "/required-actions/{alias}/config",
    urlParamKeys: ["alias"],
  });

  // Update the configuration from the RequiredAction provider in the current Realm.
  public updateRequiredActionConfig = this.makeUpdateRequest<
    { alias: string },
    RequiredActionConfigRepresentation,
    void
  >({
    method: "PUT",
    path: "/required-actions/{alias}/config",
    urlParamKeys: ["alias"],
  });

  public getConfigDescription = this.makeRequest<
    { providerId: string },
    AuthenticatorConfigInfoRepresentation
  >({
    method: "GET",
    path: "config-description/{providerId}",
    urlParamKeys: ["providerId"],
  });

  public createConfig = this.makeRequest<
    AuthenticatorConfigRepresentation,
    AuthenticatorConfigRepresentation
  >({
    method: "POST",
    path: "/executions/{id}/config",
    urlParamKeys: ["id"],
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public updateConfig = this.makeRequest<
    AuthenticatorConfigRepresentation,
    void
  >({
    method: "PUT",
    path: "/config/{id}",
    urlParamKeys: ["id"],
  });

  public getConfig = this.makeRequest<
    { id: string },
    AuthenticatorConfigRepresentation
  >({
    method: "GET",
    path: "/config/{id}",
    urlParamKeys: ["id"],
  });

  public delConfig = this.makeRequest<{ id: string }>({
    method: "DELETE",
    path: "/config/{id}",
    urlParamKeys: ["id"],
  });

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/authentication",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }
}
