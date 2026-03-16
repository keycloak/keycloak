/** TIDECLOAK IMPLEMENTATION */
import type RequestedChanges from "@keycloak/keycloak-admin-client/lib/defs/RequestedChanges";
import type RoleChangeRequest from "@keycloak/keycloak-admin-client/lib/defs/RoleChangeRequest";
import type CompositeRoleChangeRequest from "@keycloak/keycloak-admin-client/lib/defs/CompositeRoleChangeRequest";

export interface BundledRequest<T = any> {
  draftRecordId: string;
  requests: T[];
  status: string;
  requestedBy: string;
  requestedByUserId: string;
  approvalCount: number;
  rejectionCount: number;
  approvedBy: string[];
  deniedBy: string[];
  commentCount: number;
  count: number;
}

export function groupRequestsByDraftId<T extends { draftRecordId: string; status: string; deleteStatus?: string; userRecord: any[]; requestedBy?: string; requestedByUsername?: string }>(
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
    // Calculate bundle status - for deletions, use deleteStatus when status is ACTIVE
    const effectiveStatus = (r: T) => r.status === "ACTIVE" ? r.deleteStatus || r.status : r.status;
    const statuses = [...new Set(requests.map(effectiveStatus))];
    let bundleStatus = effectiveStatus(requests[0]);
    if (statuses.length > 1) {
      bundleStatus = "MIXED";
    }

    const first = requests[0] as any;
    return {
      draftRecordId,
      requests,
      status: bundleStatus,
      requestedBy: first.requestedByUsername || first.requestedBy || 'Unknown',
      requestedByUserId: first.requestedBy || '',
      approvalCount: first.approvalCount ?? 0,
      rejectionCount: first.rejectionCount ?? 0,
      approvedBy: first.approvedBy ?? [],
      deniedBy: first.deniedBy ?? [],
      commentCount: first.commentCount ?? 0,
      count: requests.length,
    };
  });
}