/** TIDECLOAK IMPLEMENTATION */
import { useState, useEffect, useCallback, Fragment } from "react";
import { useTranslation } from "react-i18next";
import {
  TextContent,
  Text,
  EmptyState,
  EmptyStateBody,
  Label,
  Button,
  AlertVariant,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
} from "@patternfly/react-core";
import { Table, Thead, Tr, Th, Tbody, Td } from "@patternfly/react-table";
import { useAdminClient } from "../admin-client";
import { useRealm } from "../context/realm-context/RealmContext";
import { useEnvironment, useAlerts } from "@keycloak/keycloak-ui-shared";
import {
  base64ToBytes,
  bytesToBase64,
} from "../tide-change-requests/utils/blockchain/tideSerialization";
import {
  CheckCircleIcon,
  BanIcon,
  InProgressIcon,
} from "@patternfly/react-icons";

interface ServerCertEntry {
  id: string;
  changeRequestId: string;
  clientId: string;
  instanceId: string;
  spiffeId: string;
  fingerprint: string;
  status: string;
  revoked: boolean;
  timestamp: number;
}

export const ServerCertsTab = () => {
  const { adminClient } = useAdminClient();
  const { approveTideRequests } = useEnvironment();
  const { realm } = useRealm();
  const { t } = useTranslation();
  const { addAlert } = useAlerts();
  const [certs, setCerts] = useState<ServerCertEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());

  const fetchCerts = useCallback(async () => {
    setLoading(true);
    try {
      const token = await adminClient.getAccessToken();
      const response = await fetch(
        `${adminClient.baseUrl}/admin/realms/${realm}/tide-admin/server-cert/requests`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            Accept: "application/json",
          },
        },
      );
      if (response.ok) {
        const data: ServerCertEntry[] = await response.json();
        setCerts(data);
      }
    } catch (e) {
      console.error("Failed to fetch server certs", e);
    }
    setLoading(false);
  }, [adminClient, realm]);

  useEffect(() => {
    void fetchCerts();
  }, [fetchCerts]);

  const handleApprove = async (changeRequestId: string) => {
    try {
      const changeRequests = [
        {
          changeSetId: changeRequestId,
          changeSetType: "SERVER_CERT",
          actionType: "CREATE",
        },
      ];

      const respObj: any = await adminClient.tideUsersExt.approveDraftChangeSet(
        {
          changeSets: changeRequests,
        },
      );

      if (respObj.length > 0) {
        try {
          const firstRespObj = respObj[0];
          if (
            firstRespObj.requiresApprovalPopup === true ||
            firstRespObj.requiresApprovalPopup === "true"
          ) {
            const respMetaMap: Record<
              string,
              { actionType: string; changeSetType: string }
            > = {};

            const missingDraft = respObj.find(
              (resp: any) => !resp.changeSetDraftRequests,
            );
            if (missingDraft) {
              addAlert(
                "Server cert request model not available. Please rebuild the IGA extensions.",
                AlertVariant.danger,
              );
              return;
            }

            const changereqs = respObj.map((resp: any) => {
              respMetaMap[resp.changesetId] = {
                actionType: resp.actionType || "CREATE",
                changeSetType: resp.changeSetType || "SERVER_CERT",
              };
              return {
                id: resp.changesetId,
                request: base64ToBytes(resp.changeSetDraftRequests),
              };
            });

            const reviewResponses = await approveTideRequests(changereqs);

            for (const reviewResp of reviewResponses) {
              if (reviewResp.approved) {
                const meta = respMetaMap[reviewResp.id] || {
                  actionType: "CREATE",
                  changeSetType: "SERVER_CERT",
                };
                const msg = reviewResp.approved.request;
                const formData = new FormData();
                formData.append("changeSetId", reviewResp.id);
                formData.append("actionType", meta.actionType);
                formData.append("changeSetType", meta.changeSetType);
                formData.append("requests", bytesToBase64(msg));

                await adminClient.tideAdmin.addReview(formData);
              } else if (reviewResp.denied) {
                const meta = respMetaMap[reviewResp.id] || {
                  actionType: "CREATE",
                  changeSetType: "SERVER_CERT",
                };
                const formData = new FormData();
                formData.append("changeSetId", reviewResp.id);
                formData.append("actionType", meta.actionType);
                formData.append("changeSetType", meta.changeSetType);

                await adminClient.tideAdmin.addRejection(formData);
              }
            }
            // Commit after enclave approval
            await adminClient.tideUsersExt.commitDraftChangeSet({
              changeSets: changeRequests,
            });
            addAlert(
              "Server certificate approved and signed",
              AlertVariant.success,
            );
          } else {
            // No enclave needed — commit directly
            await adminClient.tideUsersExt.commitDraftChangeSet({
              changeSets: changeRequests,
            });
            addAlert(
              "Server certificate approved and signed",
              AlertVariant.success,
            );
          }
        } catch (error: any) {
          addAlert(
            error.responseData || "Approval failed",
            AlertVariant.danger,
          );
        } finally {
          void fetchCerts();
        }
      }
    } catch (error: any) {
      addAlert(error.responseData || "Approval failed", AlertVariant.danger);
    }
  };

  const handleRevoke = async (instanceId: string) => {
    try {
      const token = await adminClient.getAccessToken();
      const response = await fetch(
        `${adminClient.baseUrl}/admin/realms/${realm}/tide-admin/server-cert/revoke`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ instanceId }),
        },
      );
      if (response.ok) {
        addAlert(t("Server certificate revoked"), AlertVariant.success);
        void fetchCerts();
      } else {
        const err = await response.text();
        addAlert(err || "Revocation failed", AlertVariant.danger);
      }
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "Revocation failed";
      addAlert(msg, AlertVariant.danger);
    }
  };

  const toggleRow = (id: string) => {
    setExpandedRows((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const getStatusIcon = (cert: ServerCertEntry) => {
    if (cert.revoked)
      return (
        <Label color="red" icon={<BanIcon />}>
          Revoked
        </Label>
      );
    switch (cert.status) {
      case "ACTIVE":
        return (
          <Label color="green" icon={<CheckCircleIcon />}>
            Active
          </Label>
        );
      case "DRAFT":
        return (
          <Label color="blue" icon={<InProgressIcon />}>
            Pending Approval
          </Label>
        );
      case "APPROVED":
        return (
          <Label color="cyan" icon={<CheckCircleIcon />}>
            Approved
          </Label>
        );
      default:
        return <Label>{cert.status}</Label>;
    }
  };

  if (loading) {
    return (
      <EmptyState>
        <EmptyStateBody>Loading server certificates...</EmptyStateBody>
      </EmptyState>
    );
  }

  if (certs.length === 0) {
    return (
      <EmptyState>
        <EmptyStateBody>
          No server certificates. Server certificates are requested by app
          servers via the public /tide-server-identity/request endpoint. They
          require admin quorum approval before a VVK-signed SVID is issued.
        </EmptyStateBody>
      </EmptyState>
    );
  }

  return (
    <>
      <TextContent style={{ marginBottom: "1rem" }}>
        <Text component="h3">Server Identity Certificates (SPIFFE SVIDs)</Text>
        <Text>
          VVK-signed X.509 certificates for server mTLS authentication. Each
          certificate binds a server instance to a client via a SPIFFE ID.
        </Text>
      </TextContent>

      <Table aria-label="Server certificates">
        <Thead>
          <Tr>
            <Th />
            <Th>Client</Th>
            <Th>Instance</Th>
            <Th>Fingerprint</Th>
            <Th>Status</Th>
            <Th>Requested</Th>
            <Th>Actions</Th>
          </Tr>
        </Thead>
        <Tbody>
          {certs.map((cert) => (
            <Fragment key={cert.id}>
              <Tr>
                <Td
                  expand={{
                    rowIndex: 0,
                    isExpanded: expandedRows.has(cert.id),
                    onToggle: () => toggleRow(cert.id),
                  }}
                />
                <Td>{cert.clientId}</Td>
                <Td>
                  <code>{cert.instanceId}</code>
                </Td>
                <Td>
                  <code>{cert.fingerprint?.substring(0, 20)}...</code>
                </Td>
                <Td>{getStatusIcon(cert)}</Td>
                <Td>{new Date(cert.timestamp).toLocaleString()}</Td>
                <Td>
                  {cert.status === "DRAFT" && (
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => void handleApprove(cert.changeRequestId)}
                    >
                      Approve
                    </Button>
                  )}
                  {cert.status === "APPROVED" && (
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={async () => {
                        try {
                          await adminClient.tideUsersExt.commitDraftChangeSet({
                            changeSets: [
                              {
                                changeSetId: cert.changeRequestId,
                                changeSetType: "SERVER_CERT",
                                actionType: "CREATE",
                              },
                            ],
                          });
                          addAlert(
                            "Server certificate signed and committed",
                            AlertVariant.success,
                          );
                          void fetchCerts();
                        } catch (e: any) {
                          addAlert(
                            e.responseData || "Commit failed",
                            AlertVariant.danger,
                          );
                        }
                      }}
                    >
                      Commit
                    </Button>
                  )}
                  {cert.status === "ACTIVE" && !cert.revoked && (
                    <Button
                      variant="danger"
                      size="sm"
                      onClick={() => void handleRevoke(cert.instanceId)}
                    >
                      Revoke
                    </Button>
                  )}
                </Td>
              </Tr>
              {expandedRows.has(cert.id) && (
                <Tr isExpanded>
                  <Td colSpan={7}>
                    <DescriptionList isHorizontal>
                      <DescriptionListGroup>
                        <DescriptionListTerm>SPIFFE ID</DescriptionListTerm>
                        <DescriptionListDescription>
                          <code>{cert.spiffeId}</code>
                        </DescriptionListDescription>
                      </DescriptionListGroup>
                      <DescriptionListGroup>
                        <DescriptionListTerm>
                          Full Fingerprint
                        </DescriptionListTerm>
                        <DescriptionListDescription>
                          <code>{cert.fingerprint}</code>
                        </DescriptionListDescription>
                      </DescriptionListGroup>
                      <DescriptionListGroup>
                        <DescriptionListTerm>
                          Change Request ID
                        </DescriptionListTerm>
                        <DescriptionListDescription>
                          <code>{cert.changeRequestId}</code>
                        </DescriptionListDescription>
                      </DescriptionListGroup>
                    </DescriptionList>
                  </Td>
                </Tr>
              )}
            </Fragment>
          ))}
        </Tbody>
      </Table>

      <Button
        variant="secondary"
        onClick={() => void fetchCerts()}
        style={{ marginTop: "1rem" }}
      >
        Refresh
      </Button>
    </>
  );
};
