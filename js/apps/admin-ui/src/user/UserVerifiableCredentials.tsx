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

  return (
    <>
      <DeleteConfirm />
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
            transforms: [cellWidth(40)],
          },
          {
            name: "revision",
            displayKey: "revision",
            cellFormatters: [emptyFormatter()],
            transforms: [cellWidth(30)],
          },
          {
            name: "createdDate",
            displayKey: "created",
            transforms: [cellWidth(30)],
            cellRenderer: ({ createdDate }) =>
              createdDate ? formatDate(new Date(createdDate)) : "—",
          },
        ]}
        actions={[
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
