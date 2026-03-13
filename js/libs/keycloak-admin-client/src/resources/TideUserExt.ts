import type { KeycloakAdminClient } from "../client.js";
import Resource from "./resource.js";
import type RequestedChanges from "../defs/RequestedChanges.js";
import type RoleChangeRequest from "../defs/RoleChangeRequest.js";
import type CompositeRoleChangeRequest from "../defs/CompositeRoleChangeRequest.js";
import type GroupChangeRequest from "../defs/GroupChangeRequest.js";
import type DraftChangeSetRequest from "../defs/DraftChangeSetRequest.js";

// TIDECLOAK IMPLEMENTATION
interface ChangeSetRequest {
  changeSetId: string;
  changeSetType: string;
  actionType: string;
}

interface ChangeSetRequestList {
  changeSets: ChangeSetRequest[];
}

export interface RoleDraftStatus {
  draftStatus: string,
  deleteStatus: string
}

export interface changeSetApprovalRequest {
  message: string,
  uri: string,
  changeSetRequests: string,
  requiresApprovalPopup: string,
  expiry: string
}

export class TideUsersExt extends Resource<{ realm?: string }> {
    public generateDefaultUserContext = this.makeRequest<
    {
        clients?: (string)[];
    },
    string
    >({
        method: "POST",
        path: "/tide-admin/generate-default-user-context",
        payloadKey: "clients",
    });

  public getUserDraftStatus = this.makeRequest<
    { id: string },
    string
  >({
    method: "GET",
    path: "/tide-admin/users/{id}/draft/status",
    urlParamKeys: ["id"],
  });

  public getUserRoleDraftStatus = this.makeRequest<
    { userId: string, roleId: string },
    RoleDraftStatus
  >({
    method: "GET",
    path: "/tide-admin/users/{userId}/roles/{roleId}/draft/status",
    urlParamKeys: ["userId", "roleId"],
  });

  public getRoleDraftStatus = this.makeRequest<
    { parentId: string, childId: string },
    RoleDraftStatus
  >({
    method: "GET",
    path: "/tide-admin/composite/{parentId}/child/{childId}/draft/status",
    urlParamKeys: ["parentId", "childId"],
  });

  public getRequestedChangesForUsers = this.makeRequest<void, RoleChangeRequest[]>({
    method: "GET",
    path: "/tide-admin/change-set/users/requests",
  });

  public getRequestedChangesForRoles = this.makeRequest<void, CompositeRoleChangeRequest[] | RoleChangeRequest[]>({
    method: "GET",
    path: "/tide-admin/change-set/roles/requests",
  });

  public getRequestedChangesForClients = this.makeRequest<void, RequestedChanges[]>({
    method: "GET",
    path: "/tide-admin/change-set/clients/requests",
  });

  public getRequestedChangesForGroups = this.makeRequest<void, GroupChangeRequest[]>({
    method: "GET",
    path: "/tide-admin/change-set/groups/requests",
  });

  public getRequestedChangesForRagnarokSettings = this.makeRequest<void, RequestedChanges[]>({
    method: "GET",
    path: "/ragnarok/change-set/offboarding/requests",
  });
    public getRequestedChangesForRealmLicensing = this.makeRequest<void, RequestedChanges[]>({
    method: "GET",
    path: "/tideAdminResources/change-set/licensing/requests",
  });


public getChangeSetRequests = this.makeRequest<
  ChangeSetRequestList,
  { id?: string; type?: string }
>({
  method: "GET",
  path: "/tide-admin/change-set/requests",
  queryParamKeys: ["id", "type"],
});

  public approveDraftChangeSet = this.makeRequest<
  ChangeSetRequestList,
  any
>({
  method: "POST",
  path: "/tide-admin/change-set/sign/batch",
});


  public cancelDraftChangeSet = this.makeRequest<
    ChangeSetRequestList,
    void
  >({
    method: "POST",
    path: "/tide-admin/change-set/cancel/batch",
  });

  public commitDraftChangeSet = this.makeRequest<
    ChangeSetRequestList,
    void
  >({
    method: "POST",
    path: "/tide-admin/change-set/commit/batch",
  });

  // ── Policy Template endpoints ───────────────────────────────────

  public createPolicyTemplate = this.makeRequest<
    {
      name: string;
      description?: string;
      contractCode: string;
      modelId?: string;
      approvalType?: string;
      executionType?: string;
      params?: Record<string, string>;
      parameters?: Array<{
        name: string;
        type: string;
        helpText: string;
        required: boolean;
        defaultValue?: string;
        options?: string[];
      }>;
    },
    { success: boolean; name: string; description: string; modelId: string }
  >({
    method: "POST",
    path: "/tide-admin/policy-templates",
  });

  public listPolicyTemplates = this.makeRequest<
    void,
    Array<{
      id: string;
      name: string;
      description?: string;
      contractCode: string;
      modelId?: string;
      approvalType?: string;
      executionType?: string;
      parameters?: Array<{
        name: string;
        type: string;
        helpText: string;
        required: boolean;
        defaultValue?: string;
        options?: string[];
      }>;
    }>
  >({
    method: "GET",
    path: "/tide-admin/policy-templates",
  });

  public updatePolicyTemplate = this.makeRequest<
    {
      id: string;
      name: string;
      description?: string;
      contractCode: string;
      modelId?: string;
      parameters?: Array<{
        name: string;
        type: string;
        helpText: string;
        required: boolean;
        defaultValue?: string;
        options?: string[];
      }>;
    },
    { success: boolean; id: string; name: string }
  >({
    method: "PUT",
    path: "/tide-admin/policy-templates/{id}",
    urlParamKeys: ["id"],
  });

  public deletePolicyTemplate = this.makeRequest<
    { id: string },
    { success: boolean }
  >({
    method: "DELETE",
    path: "/tide-admin/policy-templates/{id}",
    urlParamKeys: ["id"],
  });

  // ── Realm Policy endpoints (Midgard-signed) ───────────────────

  public getRealmPolicy = this.makeRequest<
    void,
    {
      status: "none" | "pending" | "active";
      id?: string;
      templateId?: string;
      templateName?: string;
      changesetRequestId?: string;
      requestModel?: string;
      policyData?: string;
      timestamp?: number;
    }
  >({
    method: "GET",
    path: "/tide-admin/realm-policy",
  });

  public createPendingRealmPolicy = this.makeRequest<
    {
      templateId: string;
      contractCode: string;
      paramValues?: Record<string, string>;
    },
    {
      success: boolean;
      id: string;
      changesetRequestId: string;
      requestModel: string;
      templateName: string;
      contractId: string;
    }
  >({
    method: "POST",
    path: "/tide-admin/realm-policy/pending",
  });

  public commitRealmPolicy = this.makeRequest<
    void,
    { success: boolean; id: string }
  >({
    method: "POST",
    path: "/tide-admin/realm-policy/commit",
  });

  public deleteRealmPolicy = this.makeRequest<void, { success: boolean }>({
    method: "DELETE",
    path: "/tide-admin/realm-policy",
  });

  public requestDeleteRealmPolicy = this.makeRequest<
    void,
    {
      success: boolean;
      id: string;
      changesetRequestId: string;
      requestModel: string;
    }
  >({
    method: "POST",
    path: "/tide-admin/realm-policy/request-delete",
  });

  public commitDeleteRealmPolicy = this.makeRequest<
    void,
    { success: boolean; id: string }
  >({
    method: "POST",
    path: "/tide-admin/realm-policy/commit-delete",
  });

  // ── Forseti Contract endpoints ───────────────────────────────────

  public upsertForsetiContract = this.makeRequest<
    {
      contractCode: string;
      name?: string;
    },
    { success: boolean; id: string; contractHash: string }
  >({
    method: "PUT",
    path: "/tide-admin/forseti-contracts",
  });

  public listForsetiContracts = this.makeRequest<
    void,
    Array<{
      id: string;
      contractHash: string;
      contractCode: string;
      name?: string;
      timestamp: number;
    }>
  >({
    method: "GET",
    path: "/tide-admin/forseti-contracts",
  });

  // ── SSH Policy endpoints ─────────────────────────────────────────

  public upsertSshPolicy = this.makeRequest<
    {
      roleId: string;
      contractCode?: string;
      approvalType?: string;
      executionType?: string;
      threshold?: number;
      policyData?: string;
    },
    { success: boolean; id: string; roleId: string }
  >({
    method: "PUT",
    path: "/tide-admin/ssh-policies",
  });

  public listSshPolicies = this.makeRequest<
    void,
    Array<{
      id: string;
      roleId: string;
      contractId?: string;
      contractHash?: string;
      contractName?: string;
      approvalType: string;
      executionType: string;
      threshold: number;
      policyData?: string;
      timestamp: number;
    }>
  >({
    method: "GET",
    path: "/tide-admin/ssh-policies",
  });

  public deleteSshPolicy = this.makeRequest<
    { roleId: string },
    { success: boolean }
  >({
    method: "DELETE",
    path: "/tide-admin/ssh-policies",
    queryParamKeys: ["roleId"],
  });

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }
}
