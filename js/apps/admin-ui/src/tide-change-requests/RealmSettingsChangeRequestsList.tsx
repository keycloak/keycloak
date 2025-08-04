import { useEffect, useState } from "react";
import { Spinner, Card, CardBody, CardTitle, Badge, ExpandableSection } from "@patternfly/react-core";
import { useAdminClient } from "../admin-client";
import RequestedChanges from "@keycloak/keycloak-admin-client/lib/defs/RequestedChanges";
import { Table, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table';
import { Button } from "@patternfly/react-core";
import { CheckIcon, TimesIcon, AngleRightIcon, AngleDownIcon } from "@patternfly/react-icons";

interface RealmSettingsChangeRequestsListProps {
  updateCounter: (count: number) => void;
}

interface BundledRequest {
  draftRecordId: string;
  requests: RequestedChanges[];
  status: string;
  requestedBy: string;
  count: number;
}

export const RealmSettingsChangeRequestsList = ({ updateCounter }: RealmSettingsChangeRequestsListProps) => {
  const { adminClient } = useAdminClient();
  const [realmSettingsRequests, setRealmSettingsRequests] = useState<RequestedChanges[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedBundles, setExpandedBundles] = useState<Set<string>>(new Set());

  const loadRealmSettingsRequests = async () => {
    try {
      setLoading(true);
      const requests = await adminClient.tideUsersExt.getRequestedChangesForRagnarokSettings();
      setRealmSettingsRequests(requests);
      const bundleCount = groupRequestsByDraftId(requests).length;
      updateCounter(bundleCount);
    } catch (error) {
      console.error("Failed to load realm settings requests:", error);
      setRealmSettingsRequests([]);
      updateCounter(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRealmSettingsRequests();
  }, []);

  const handleApprove = async (draftRecordId: string) => {
    try {
      // Implementation for approval
      console.log("Approving realm settings request:", draftRecordId);
      await loadRealmSettingsRequests();
    } catch (error) {
      console.error("Failed to approve request:", error);
    }
  };

  const handleReject = async (draftRecordId: string) => {
    try {
      // Implementation for rejection
      console.log("Rejecting realm settings request:", draftRecordId);
      await loadRealmSettingsRequests();
    } catch (error) {
      console.error("Failed to reject request:", error);
    }
  };

  const groupRequestsByDraftId = (requests: RequestedChanges[]): BundledRequest[] => {
    const grouped = requests.reduce((acc, request) => {
      const { draftRecordId } = request;
      if (!acc[draftRecordId]) {
        acc[draftRecordId] = [];
      }
      acc[draftRecordId].push(request);
      return acc;
    }, {} as Record<string, RequestedChanges[]>);

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
  };

  const toggleBundle = (draftRecordId: string) => {
    const newExpanded = new Set(expandedBundles);
    if (newExpanded.has(draftRecordId)) {
      newExpanded.delete(draftRecordId);
    } else {
      newExpanded.add(draftRecordId);
    }
    setExpandedBundles(newExpanded);
  };

  const bundledRequests = groupRequestsByDraftId(realmSettingsRequests);

  if (loading) {
    return <Spinner size="lg" />;
  }

  if (realmSettingsRequests.length === 0) {
    return <div>No realm settings change requests found.</div>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      {bundledRequests.map((bundle) => (
        <Card key={bundle.draftRecordId} isCompact>
          <CardTitle>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <Button
                  variant="plain"
                  onClick={() => toggleBundle(bundle.draftRecordId)}
                  icon={expandedBundles.has(bundle.draftRecordId) ? <AngleDownIcon /> : <AngleRightIcon />}
                  style={{ padding: '4px' }}
                />
                <span style={{ fontWeight: 'bold', fontSize: '16px' }}>
                  {bundle.count > 1 ? `Bundle (${bundle.count} requests)` : 'Single Request'}: {bundle.draftRecordId}
                </span>
                <Badge isRead={bundle.status === 'APPROVED'}>
                  {bundle.status}
                </Badge>
                {bundle.count > 1 && (
                  <Badge>
                    {bundle.requests.map(r => r.requestType).join(', ')}
                  </Badge>
                )}
              </div>
              <div style={{ display: 'flex', gap: '8px' }}>
                <Button
                  variant="primary"
                  size="sm"
                  icon={<CheckIcon />}
                  onClick={() => handleApprove(bundle.draftRecordId)}
                >
                  Approve Bundle
                </Button>
                <Button
                  variant="danger"
                  size="sm"
                  icon={<TimesIcon />}
                  onClick={() => handleReject(bundle.draftRecordId)}
                >
                  Reject Bundle
                </Button>
              </div>
            </div>
          </CardTitle>
          <CardBody>
            <div style={{ marginBottom: '8px', color: '#6a6e73' }}>
              Requested by: {bundle.requestedBy}
            </div>
            {expandedBundles.has(bundle.draftRecordId) && (
              <Table aria-label={`Bundle ${bundle.draftRecordId} Details`} variant="compact">
                <Thead>
                  <Tr>
                    <Th>Request Type</Th>
                    <Th>Action</Th>
                    <Th>Status</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {bundle.requests.map((request, index) => (
                    <Tr key={`${request.draftRecordId}-${index}`}>
                      <Td>{request.requestType}</Td>
                      <Td>{request.actionType}</Td>
                      <Td>
                        <Badge isRead={request.status === 'APPROVED'}>
                          {request.status}
                        </Badge>
                      </Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            )}
          </CardBody>
        </Card>
      ))}
    </div>
  );
};