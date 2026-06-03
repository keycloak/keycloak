import {
  useEnvironment,
  KeycloakDataTable,
  ErrorBoundaryProvider,
} from "@keycloak/keycloak-ui-shared";
import type { Action } from "@keycloak/keycloak-ui-shared";
import { Button, Label, Modal, ModalVariant } from "@patternfly/react-core";
import { ExclamationTriangleIcon } from "@patternfly/react-icons";
import { cellWidth } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getIssuedVerifiableCredentials,
  revokeIssuedVerifiableCredential,
} from "../api/methods";
import { IssuedUserVerifiableCredentialRepresentation } from "../api/representations";
import { useAccountAlerts } from "../utils/useAccountAlerts";

type IssuedCredentialsModalProps = {
  credentialScopeName: string;
  parentRevision?: string;
  onClose: () => void;
};

type IssuedCredentialWithClientName =
  IssuedUserVerifiableCredentialRepresentation & {
    clientName?: string;
  };

type RevokeDialogProps = {
  isOpen: boolean;
  credential: IssuedCredentialWithClientName | undefined;
  onClose: () => void;
  onConfirm: (credentialId: string) => Promise<void>;
  t: (key: string) => string;
};

const RevokeDialog = ({
  isOpen,
  credential,
  onClose,
  onConfirm,
  t,
}: RevokeDialogProps) => {
  if (!isOpen || !credential) return null;

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("Revoke Issued Credential")}
      isOpen={true}
      onClose={onClose}
      actions={[
        <Button
          key="confirm"
          variant="danger"
          onClick={async () => {
            await onConfirm(credential.id!);
            onClose();
          }}
        >
          {t("revoke")}
        </Button>,
        <Button key="cancel" variant="link" onClick={onClose}>
          {t("cancel")}
        </Button>,
      ]}
    >
      {t("Confirm Revoke Issued Credential")}
    </Modal>
  );
};

export const IssuedCredentialsModal = ({
  credentialScopeName,
  parentRevision,
  onClose,
}: IssuedCredentialsModalProps) => {
  const { t } = useTranslation();
  const context = useEnvironment();
  const { addAlert, addError } = useAccountAlerts();
  const [selectedCredential, setSelectedCredential] =
    useState<IssuedCredentialWithClientName>();
  const [key, setKey] = useState(0);
  const [showRevokeDialog, setShowRevokeDialog] = useState(false);

  const refresh = () => setKey((prev) => prev + 1);

  const toggleRevokeDialog = () => setShowRevokeDialog(!showRevokeDialog);

  const hasManageRole = () => {
    const token = context.keycloak.tokenParsed;
    const accountRoles = token?.resource_access?.["account"]?.roles || [];
    return (
      accountRoles.includes("manage-account") ||
      accountRoles.includes("manage-verifiable-credentials")
    );
  };

  const loader = async (first?: number, max?: number, search?: string) => {
    const data = await getIssuedVerifiableCredentials({
      signal: new AbortController().signal,
      context,
    });

    // Filter by credential type
    let filtered = data.filter(
      (ic) => ic.credentialType === credentialScopeName,
    );

    // Apply search filter if provided
    if (search) {
      const searchLower = search.toLowerCase();
      filtered = filtered.filter(
        (ic) =>
          ic.id?.toLowerCase().includes(searchLower) ||
          ic.clientId?.toLowerCase().includes(searchLower) ||
          ic.revision?.toLowerCase().includes(searchLower),
      );
    }

    const credentialsWithClientNames: IssuedCredentialWithClientName[] =
      filtered.map((ic) => ({
        ...ic,
        clientName: ic.clientId,
      }));

    // Apply pagination if provided
    if (first !== undefined && max !== undefined) {
      return credentialsWithClientNames.slice(first, first + max);
    }

    return credentialsWithClientNames;
  };

  const handleRevoke = async (credentialId: string) => {
    try {
      await revokeIssuedVerifiableCredential(context, credentialId);
      addAlert(t("Revoke Issued Credential Success"));
      refresh();
    } catch (error) {
      addError("Revoke Issued Credential Error", error);
    }
  };

  return (
    <>
      <RevokeDialog
        isOpen={showRevokeDialog}
        credential={selectedCredential}
        onClose={toggleRevokeDialog}
        onConfirm={handleRevoke}
        t={t}
      />
      <Modal
        variant={ModalVariant.large}
        title={t("Issued Credentials")}
        isOpen={true}
        onClose={onClose}
        width="90%"
      >
        <ErrorBoundaryProvider>
          <KeycloakDataTable
            loader={loader}
            key={key}
            ariaLabelKey="issuedCredentials"
            searchPlaceholderKey=" "
            columns={[
              {
                name: "id",
                displayKey: "ID",
                cellRenderer: ({ id }) => (
                  <code className="pf-v5-u-font-size-sm">{id}</code>
                ),
                transforms: [cellWidth(25)],
              },
              {
                name: "issuedAt",
                displayKey: "Issued At",
                cellRenderer: ({ issuedAt }) =>
                  issuedAt ? new Date(issuedAt).toLocaleString("en-US") : "—",
                transforms: [cellWidth(15)],
              },
              {
                name: "expiresAt",
                displayKey: "Expires At",
                cellRenderer: ({ expiresAt }) => {
                  if (!expiresAt) return "—";
                  const expirationDate = new Date(expiresAt);
                  const isExpired = expirationDate < new Date();
                  return (
                    <span
                      style={{
                        color: isExpired
                          ? "var(--pf-v5-global--danger-color--100)"
                          : "inherit",
                      }}
                    >
                      {isExpired && (
                        <ExclamationTriangleIcon
                          style={{ marginRight: "0.25rem" }}
                        />
                      )}
                      {expirationDate.toLocaleString("en-US")}
                    </span>
                  );
                },
                transforms: [cellWidth(15)],
              },
              {
                name: "clientId",
                displayKey: "Wallet Client",
                cellRenderer: (credential: IssuedCredentialWithClientName) =>
                  credential.clientName || credential.clientId || "—",
                transforms: [cellWidth(20)],
              },
              {
                name: "revision",
                displayKey: "Revision",
                cellRenderer: ({ revision }) => (
                  <>
                    {revision || "—"}
                    {parentRevision &&
                      revision &&
                      revision !== parentRevision && (
                        <>
                          {" "}
                          <Label
                            color="orange"
                            icon={<ExclamationTriangleIcon />}
                          >
                            {t("outdated")}
                          </Label>
                        </>
                      )}
                  </>
                ),
                transforms: [cellWidth(20)],
              },
            ]}
            actions={
              hasManageRole()
                ? [
                    {
                      title: t("revoke"),
                      onRowClick: (credential) => {
                        setSelectedCredential(credential);
                        toggleRevokeDialog();
                      },
                    } as Action<IssuedCredentialWithClientName>,
                  ]
                : []
            }
            emptyState={
              <div className="pf-v5-u-text-align-center pf-v5-u-py-md">
                <span className="pf-v5-u-color-200">
                  {t("No Issued Credentials")}
                </span>
              </div>
            }
          />
        </ErrorBoundaryProvider>
      </Modal>
    </>
  );
};
