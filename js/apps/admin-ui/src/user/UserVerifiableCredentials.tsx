import type UserVerifiableCredentialRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userVerifiableCredentialRepresentation";
import { AlertVariant, Button, ButtonVariant } from "@patternfly/react-core";
import { CubesIcon } from "@patternfly/react-icons";
import { cellWidth } from "@patternfly/react-table";
import { sortBy } from "lodash-es";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { Action, KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { emptyFormatter } from "../util";
import useFormatDate from "../utils/useFormatDate";
import { CreateVerifiableCredentialModal } from "./CreateVerifiableCredentialModal";
import { UserAttributesDialog } from "./UserAttributesDialog";

type UserVerifiableCredentialsProps = {
  userId: string;
};

export const UserVerifiableCredentials = ({
  userId,
}: UserVerifiableCredentialsProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const formatDate = useFormatDate();
  const [key, setKey] = useState(0);
  const [selectedCredential, setSelectedCredential] =
    useState<UserVerifiableCredentialRepresentation>();
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [attributesDialogCredential, setAttributesDialogCredential] =
    useState<UserVerifiableCredentialRepresentation>();

  const refresh = () => setKey(new Date().getTime());

  const loader = async () => {
    const credentials = await adminClient.users.listVerifiableCredentials({
      id: userId,
    });
    return sortBy(credentials, (c) => c.credentialScopeName?.toUpperCase());
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "revokeVerifiableCredentialTitle",
    messageKey: t("revokeVerifiableCredentialConfirm", {
      credentialScopeName: selectedCredential?.credentialScopeName,
    }),
    continueButtonLabel: "revoke",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.users.revokeVerifiableCredential({
          id: userId,
          credentialScopeName: selectedCredential!.credentialScopeName!,
        });
        refresh();
        addAlert(t("revokeVerifiableCredentialSuccess"), AlertVariant.success);
      } catch (error) {
        addError("revokeVerifiableCredentialError", error);
      }
    },
  });

  const [toggleUpdateDialog, UpdateConfirm] = useConfirmDialog({
    titleKey: "updateVerifiableCredentialTitle",
    messageKey: t("updateVerifiableCredentialConfirm", {
      credentialScopeName: selectedCredential?.credentialScopeName,
    }),
    continueButtonLabel: "update",
    continueButtonVariant: ButtonVariant.primary,
    onConfirm: async () => {
      try {
        await adminClient.users.updateVerifiableCredential({
          id: userId,
          credentialScopeName: selectedCredential!.credentialScopeName!,
        });
        refresh();
        addAlert(t("updateVerifiableCredentialSuccess"), AlertVariant.success);
      } catch (error) {
        addError("updateVerifiableCredentialError", error);
      }
    },
  });

  return (
    <>
      <DeleteConfirm />
      <UpdateConfirm />
      {attributesDialogCredential?.userAttributes &&
        Object.keys(attributesDialogCredential.userAttributes).length > 0 && (
          <UserAttributesDialog
            credentialScopeName={
              attributesDialogCredential.credentialScopeName ?? ""
            }
            userAttributes={attributesDialogCredential.userAttributes}
            onClose={() => setAttributesDialogCredential(undefined)}
          />
        )}
      {isCreateModalOpen && (
        <CreateVerifiableCredentialModal
          userId={userId}
          onClose={() => setIsCreateModalOpen(false)}
          onCreated={() => {
            refresh();
            setIsCreateModalOpen(false);
          }}
        />
      )}
      <KeycloakDataTable
        loader={loader}
        key={key}
        ariaLabelKey="verifiableCredentials"
        searchPlaceholderKey=" "
        toolbarItem={
          <Button onClick={() => setIsCreateModalOpen(true)}>
            {t("createVerifiableCredential")}
          </Button>
        }
        columns={[
          {
            name: "credentialScopeName",
            displayKey: "credentialScopeName",
            cellFormatters: [emptyFormatter()],
            transforms: [cellWidth(25)],
          },
          {
            name: "revision",
            displayKey: "revision",
            cellFormatters: [emptyFormatter()],
            transforms: [cellWidth(15)],
          },
          {
            name: "createdDate",
            displayKey: "created",
            transforms: [cellWidth(20)],
            cellRenderer: ({ createdDate }) =>
              createdDate ? formatDate(new Date(createdDate)) : "—",
          },
          {
            name: "userAttributes",
            displayKey: "userAttributes",
            transforms: [cellWidth(40)],
            cellRenderer: (credential) => {
              if (
                !credential.userAttributes ||
                Object.keys(credential.userAttributes).length === 0
              ) {
                return (
                  <span className="pf-v5-u-color-200">
                    {t("credentialNoUserAttributes")}
                  </span>
                );
              }
              return (
                <Button
                  variant="link"
                  onClick={() => setAttributesDialogCredential(credential)}
                >
                  {t("credentialViewAttributes")}
                </Button>
              );
            },
          },
        ]}
        actions={[
          {
            title: t("updateCredential"),
            onRowClick: (credential) => {
              setSelectedCredential(credential);
              toggleUpdateDialog();
            },
          } as Action<UserVerifiableCredentialRepresentation>,
          {
            title: t("revoke"),
            onRowClick: (credential) => {
              setSelectedCredential(credential);
              toggleDeleteDialog();
            },
          } as Action<UserVerifiableCredentialRepresentation>,
        ]}
        emptyState={
          <ListEmptyState
            hasIcon={true}
            icon={CubesIcon}
            message={t("noVerifiableCredentials")}
            instructions={t("noVerifiableCredentialsText")}
            primaryActionText={t("createVerifiableCredential")}
            onPrimaryAction={() => setIsCreateModalOpen(true)}
          />
        }
      />
    </>
  );
};
