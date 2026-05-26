import type IssuedVerifiableCredentialRepresentation from "libs/keycloak-admin-client/src/defs/issuedUserVerifiableCredentialRepresentation";
import { AlertVariant, ButtonVariant, Label } from "@patternfly/react-core";
import { ExclamationTriangleIcon } from "@patternfly/react-icons";
import { cellWidth } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { Action, KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import useFormatDate from "../utils/useFormatDate";

type IssuedCredentialsDetailCellProps = {
  userId: string;
  credentialScopeName: string;
  parentRevision?: string;
};

export const IssuedCredentialsDetailCell = ({
  userId,
  credentialScopeName,
  parentRevision,
}: IssuedCredentialsDetailCellProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const formatDate = useFormatDate();

  const [selectedCredential, setSelectedCredential] =
    useState<IssuedVerifiableCredentialRepresentation>();
  const [key, setKey] = useState(0);

  const refresh = () => setKey((prev) => prev + 1);

  const loader = async () => {
    const allIssued = await adminClient.users.listIssuedVerifiableCredentials({
      id: userId,
    });
    return allIssued.filter((ic) => ic.credentialType === credentialScopeName);
  };

  const [toggleRevokeDialog, RevokeConfirm] = useConfirmDialog({
    titleKey: "revokeIssuedCredentialTitle",
    messageKey: t("revokeIssuedCredentialConfirm"),
    continueButtonLabel: "revoke",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.users.revokeIssuedVerifiableCredential({
          id: userId,
          credentialId: selectedCredential!.id!,
        });
        addAlert(t("revokeIssuedCredentialSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("revokeIssuedCredentialError", error);
      }
    },
  });

  return (
    <>
      <RevokeConfirm />
      <KeycloakDataTable
        loader={loader}
        key={key}
        ariaLabelKey="issuedCredentials"
        searchPlaceholderKey=" "
        columns={[
          {
            name: "id",
            displayKey: "issuedCredentialId",
            cellRenderer: ({ id }) => (
              <code className="pf-v5-u-font-size-sm">{id}</code>
            ),
            transforms: [cellWidth(30)],
          },
          {
            name: "issuedAt",
            displayKey: "issuedAt",
            cellRenderer: ({ issuedAt }) =>
              issuedAt ? formatDate(new Date(issuedAt)) : "—",
            transforms: [cellWidth(15)],
          },
          {
            name: "expiresAt",
            displayKey: "expiresAt",
            cellRenderer: ({ expiresAt }) =>
              expiresAt ? formatDate(new Date(expiresAt)) : "—",
            transforms: [cellWidth(15)],
          },
          {
            name: "clientId",
            displayKey: "walletClient",
            cellRenderer: ({ clientId }) => (
              <code className="pf-v5-u-font-size-sm">{clientId}</code>
            ),
            transforms: [cellWidth(20)],
          },
          {
            name: "revision",
            displayKey: "revision",
            cellRenderer: ({ revision }) => (
              <>
                {revision}{" "}
                {parentRevision && revision !== parentRevision && (
                  <Label color="orange" icon={<ExclamationTriangleIcon />}>
                    {t("outdated")}
                  </Label>
                )}
              </>
            ),
            transforms: [cellWidth(20)],
          },
        ]}
        actions={[
          {
            title: t("revoke"),
            onRowClick: (credential) => {
              setSelectedCredential(credential);
              toggleRevokeDialog();
            },
          } as Action<IssuedVerifiableCredentialRepresentation>,
        ]}
        emptyState={
          <div className="pf-v5-u-text-align-center pf-v5-u-py-md">
            <span className="pf-v5-u-color-200">
              {t("noIssuedCredentials")}
            </span>
          </div>
        }
      />
    </>
  );
};
