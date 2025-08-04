import type RequestedChanges from "@keycloak/keycloak-admin-client/lib/defs/RequestedChanges";
import type RoleChangeRequest from "@keycloak/keycloak-admin-client/lib/defs/RoleChangeRequest";
import type CompositeRoleChangeRequest from "@keycloak/keycloak-admin-client/lib/defs/CompositeRoleChangeRequest";

export interface BundledRequest<T = any> {
  draftRecordId: string;
  requests: T[];
  status: string;
  requestedBy: string;
  count: number;
}

export function groupRequestsByDraftId<T extends { draftRecordId: string; status: string; userRecord: any[] }>(
  requests: T[]
): BundledRequest<T>[] {
  // Group requests by draftRecordId
  const grouped = requests.reduce((acc, request) => {
    const id = request.draftRecordId;
    if (!acc[id]) {
      acc[id] = [];
    }
    acc[id].push(request);
    return acc;
  }, {} as Record<string, T[]>);

  return Object.entries(grouped).map(([draftRecordId, requests]) => {
    // Calculate bundle status more intelligently
    const statuses = [...new Set(requests.map(r => r.status))];
    let bundleStatus = requests[0].status;
    if (statuses.length > 1) {
      bundleStatus = "MIXED";
    }

    return {
      draftRecordId,
      requests,
      status: bundleStatus,
      requestedBy: requests[0].userRecord[0]?.username || 'Unknown',
      count: requests.length,
    };
  });
}