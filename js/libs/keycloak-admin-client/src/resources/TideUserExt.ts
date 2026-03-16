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

// TIDECLOAK IMPLEMENTATION
interface ChangeSetRequestList {
  changeSets: ChangeSetRequest[];
}

// TIDECLOAK IMPLEMENTATION
export interface RoleDraftStatus {
  draftStatus: string,
  deleteStatus: string
}

// TIDECLOAK IMPLEMENTATION
export interface changeSetApprovalRequest {
  message: string,
  uri: string,
  changeSetRequests: string,
  requiresApprovalPopup: string,
  expiry: string
}

/* TIDECLOAK IMPLEMENTATION */
export class TideUsersExt extends Resource<{ realm?: string }> {
    /* # TIDECLOAK IMPLEMENTATION */
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

  /* # TIDECLOAK IMPLEMENTATION */
  public getUserDraftStatus = this.makeRequest<
    { id: string },
    string
  >({
    method: "GET",
    path: "/tide-admin/users/{id}/draft/status",
    urlParamKeys: ["id"],
  });

  /* # TIDECLOAK IMPLEMENTATION */
  public getUserRoleDraftStatus = this.makeRequest<
    { userId: string, roleId: string },
    RoleDraftStatus
  >({
    method: "GET",
    path: "/tide-admin/users/{userId}/roles/{roleId}/draft/status",
    urlParamKeys: ["userId", "roleId"],
  });

  /* # TIDECLOAK IMPLEMENTATION */
  public getRoleDraftStatus = this.makeRequest<
    { parentId: string, childId: string },
    RoleDraftStatus
  >({
    method: "GET",
    path: "/tide-admin/composite/{parentId}/child/{childId}/draft/status",
    urlParamKeys: ["parentId", "childId"],
  });

  /* # TIDECLOAK IMPLEMENTATION */
  public getRequestedChangesForUsers = this.makeRequest<void, RoleChangeRequest[]>({
    method: "GET",
    path: "/tide-admin/change-set/users/requests",
  });

  /* # TIDECLOAK IMPLEMENTATION */
  public getRequestedChangesForRoles = this.makeRequest<void, CompositeRoleChangeRequest[] | RoleChangeRequest[]>({
    method: "GET",
    path: "/tide-admin/change-set/roles/requests",
  });

  /* # TIDECLOAK IMPLEMENTATION */
  public getRequestedChangesForClients = this.makeRequest<void, RequestedChanges[]>({
    method: "GET",
    path: "/tide-admin/change-set/clients/requests",
  });

  /* # TIDECLOAK IMPLEMENTATION */
  public getRequestedChangesForGroups = this.makeRequest<void, GroupChangeRequest[]>({
    method: "GET",
    path: "/tide-admin/change-set/groups/requests",
  });

  /* # TIDECLOAK IMPLEMENTATION */
  public getChangeSetCounts = this.makeRequest<void, {
    users: number;
    roles: number;
    clients: number;
    groups: number;
    total: number;
  }>({
    method: "GET",
    path: "/tide-admin/change-set/counts",
  });

  /* # TIDECLOAK IMPLEMENTATION */
  public getAllChangeSetRequests = this.makeRequest<void, {
    users: RequestedChanges[];
    roles: RequestedChanges[];
    clients: RequestedChanges[];
    groups: RequestedChanges[];
  }>({
    method: "GET",
    path: "/tide-admin/change-set/all/requests",
  });

  /* # TIDECLOAK IMPLEMENTATION */
  public getRequestedChangesForRagnarokSettings = this.makeRequest<void, RequestedChanges[]>({
    method: "GET",
    path: "/ragnarok/change-set/offboarding/requests",
  });
  /* # TIDECLOAK IMPLEMENTATION */
    public getRequestedChangesForRealmLicensing = this.makeRequest<void, RequestedChanges[]>({
    method: "GET",
    path: "/tideAdminResources/change-set/licensing/requests",
  });


/* # TIDECLOAK IMPLEMENTATION */
public getChangeSetRequests = this.makeRequest<
  ChangeSetRequestList,
  { id?: string; type?: string }
>({
  method: "GET",
  path: "/tide-admin/change-set/requests",
  queryParamKeys: ["id", "type"],
});

  /* # TIDECLOAK IMPLEMENTATION */
  public approveDraftChangeSet = this.makeRequest<
  ChangeSetRequestList,
  any
>({
  method: "POST",
  path: "/tide-admin/change-set/sign/batch",
});


  /* # TIDECLOAK IMPLEMENTATION */
  public cancelDraftChangeSet = this.makeRequest<
    ChangeSetRequestList,
    void
  >({
    method: "POST",
    path: "/tide-admin/change-set/cancel/batch",
  });

  /* # TIDECLOAK IMPLEMENTATION */
  public commitDraftChangeSet = this.makeRequest<
    ChangeSetRequestList,
    void
  >({
    method: "POST",
    path: "/tide-admin/change-set/commit/batch",
  });

  // ── Policy Template endpoints ───────────────────────────────────

  /* # TIDECLOAK IMPLEMENTATION */
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

  /* # TIDECLOAK IMPLEMENTATION */
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

  /* # TIDECLOAK IMPLEMENTATION */
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

  /* # TIDECLOAK IMPLEMENTATION */
  public deletePolicyTemplate = this.makeRequest<
    { id: string },
    { success: boolean }
  >({
    method: "DELETE",
    path: "/tide-admin/policy-templates/{id}",
    urlParamKeys: ["id"],
  });

  // ── Realm Policy endpoints (Midgard-signed) ───────────────────

  /* # TIDECLOAK IMPLEMENTATION */
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

  /* # TIDECLOAK IMPLEMENTATION */
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

  /* # TIDECLOAK IMPLEMENTATION */
  public commitRealmPolicy = this.makeRequest<
    void,
    { success: boolean; id: string }
  >({
    method: "POST",
    path: "/tide-admin/realm-policy/commit",
  });

  /* # TIDECLOAK IMPLEMENTATION */
  public deleteRealmPolicy = this.makeRequest<void, { success: boolean }>({
    method: "DELETE",
    path: "/tide-admin/realm-policy",
  });

  /* # TIDECLOAK IMPLEMENTATION */
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

  /* # TIDECLOAK IMPLEMENTATION */
  public commitDeleteRealmPolicy = this.makeRequest<
    void,
    { success: boolean; id: string }
  >({
    method: "POST",
    path: "/tide-admin/realm-policy/commit-delete",
  });

  // ── Forseti Contract endpoints ───────────────────────────────────

  /* # TIDECLOAK IMPLEMENTATION */
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

  /* # TIDECLOAK IMPLEMENTATION */
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

  /* # TIDECLOAK IMPLEMENTATION */
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

  /* # TIDECLOAK IMPLEMENTATION */
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

  /* # TIDECLOAK IMPLEMENTATION */
  public deleteSshPolicy = this.makeRequest<
    { roleId: string },
    { success: boolean }
  >({
    method: "DELETE",
    path: "/tide-admin/ssh-policies",
    queryParamKeys: ["roleId"],
  });

  /* # TIDECLOAK IMPLEMENTATION */
  public listRolePolicies = this.makeRequest<
    void,
    Array<{
      id: string;
      roleId: string;
      roleName: string;
      clientRole: boolean;
      clientId?: string;
      timestamp: number;
      hasSig: boolean;
      policyDisplay?: string;
    }>
  >({
    method: "GET",
    path: "/tide-admin/role-policies",
  });

  // ── Change Request Activity & Comments ─────────────────────────

  /* # TIDECLOAK IMPLEMENTATION */
  public getChangeSetActivity = this.makeRequest<
    { id: string },
    {
      requestedBy: string;
      requestedByUsername: string;
      timestamp: number;
      approvals: Array<{
        userId: string;
        username: string;
        isApproval: boolean;
        timestamp: number;
      }>;
      comments: Array<{
        id: string;
        userId: string;
        username: string;
        comment: string;
        timestamp: number;
      }>;
    }
  >({
    method: "GET",
    path: "/tide-admin/change-set/{id}/activity",
    urlParamKeys: ["id"],
  });

  /* # TIDECLOAK IMPLEMENTATION */
  public addChangeSetComment = this.makeRequest<
    { id: string; comment: string },
    {
      id: string;
      userId: string;
      username: string;
      comment: string;
      timestamp: number;
    }
  >({
    method: "POST",
    path: "/tide-admin/change-set/{id}/comments",
    urlParamKeys: ["id"],
  });

  /* # TIDECLOAK IMPLEMENTATION */
  public updateChangeSetComment = this.makeRequest<
    { id: string; commentId: string; comment: string },
    {
      id: string;
      userId: string;
      username: string;
      comment: string;
      timestamp: number;
    }
  >({
    method: "PUT",
    path: "/tide-admin/change-set/{id}/comments/{commentId}",
    urlParamKeys: ["id", "commentId"],
  });

  /* # TIDECLOAK IMPLEMENTATION */
  public deleteChangeSetComment = this.makeRequest<
    { id: string; commentId: string },
    { deleted: boolean }
  >({
    method: "DELETE",
    path: "/tide-admin/change-set/{id}/comments/{commentId}",
    urlParamKeys: ["id", "commentId"],
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
