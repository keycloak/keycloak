/** TIDECLOAK IMPLEMENTATION */
import { useState, useEffect, useCallback, Fragment } from "react";
import { useTranslation } from "react-i18next";
import {
  TextContent,
  Text,
  EmptyState,
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
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import {
  CheckCircleIcon,
  BanIcon,
  InProgressIcon,
  CertificateIcon,
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
  const { realm } = useRealm();
  const { t } = useTranslation();
  const { addAlert } = useAlerts();
  const [certs, setCerts] = useState<ServerCertEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());

  const fetchCerts = useCallback(async () => {
    setLoading(true);
    try {
      const response = await adminClient.fetchWithError(
        `/admin/realms/${realm}/tide-admin/server-cert/requests`,
      );
      const data: ServerCertEntry[] = await response.json();
      setCerts(data);
    } catch (e) {
      console.error("Failed to fetch server certs", e);
    }
    setLoading(false);
  }, [adminClient, realm]);

  useEffect(() => {
    void fetchCerts();
  }, [fetchCerts]);

  const handleRevoke = async (instanceId: string) => {
    try {
      await adminClient.fetchWithError(
        `/admin/realms/${realm}/tide-admin/server-cert/revoke`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ instanceId }),
        },
      );
      addAlert(t("Server certificate revoked"), AlertVariant.success);
      void fetchCerts();
    } catch (e: any) {
      addAlert(e.message || "Revocation failed", AlertVariant.danger);
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
    return <EmptyState>Loading server certificates...</EmptyState>;
  }

  if (certs.length === 0) {
    return (
      <EmptyState icon={CertificateIcon}>
        <TextContent>
          <Text component="h4">No server certificates</Text>
          <Text>
            Server certificates are requested by app servers via the public
            /tide-server-identity/request endpoint. They require admin quorum
            approval before a VVK-signed SVID is issued.
          </Text>
        </TextContent>
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
                  {cert.status === "ACTIVE" && !cert.revoked && (
                    <Button
                      variant="danger"
                      isSmall
                      onClick={() => handleRevoke(cert.instanceId)}
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
        onClick={fetchCerts}
        style={{ marginTop: "1rem" }}
      >
        Refresh
      </Button>
    </>
  );
};
