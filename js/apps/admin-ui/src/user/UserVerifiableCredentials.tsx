import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import type UserVerifiableCredentialRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userVerifiableCredentialRepresentation";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
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
import { CreateVerifiableCredentialModal } from "./verifiable-credentials/CreateVerifiableCredentialModal";
import { UserAttributesDialog } from "./verifiable-credentials/UserAttributesDialog";
import { IssuedCredentialsDetailCell } from "./verifiable-credentials/IssuedCredentialsDetailCell";
import { UserVerifiableCredentialOfferDialog } from "./verifiable-credentials/UserVerifiableCredentialOfferDialog";

type UserVerifiableCredentialsProps = {
  user: UserRepresentation;
};

export const UserVerifiableCredentials = ({
  user,
}: UserVerifiableCredentialsProps) => {
  const userId = user.id!;
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const formatDate = useFormatDate();
  const [key, setKey] = useState(0);
  const [selectedCredential, setSelectedCredential] =
    useState<UserVerifiableCredentialRepresentation>();
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [credentialOfferDialogCredential, setCredentialOfferDialogCredential] =
    useState<UserVerifiableCredentialRepresentation>();
  const [attributesDialogCredential, setAttributesDialogCredential] =
    useState<UserVerifiableCredentialRepresentation>();
  const [issuedCredentialsModalOpen, setIssuedCredentialsModalOpen] =
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

  const verifiableCredentialActions: Action<UserVerifiableCredentialRepresentation>[] =
    [
      {
        title: t("credentialViewAttributes"),
        onRowClick: (credential) => {
          setAttributesDialogCredential(credential);
        },
      },
      {
        title: t("viewIssuedCredentials"),
        onRowClick: (credential) => {
          setIssuedCredentialsModalOpen(credential);
        },
      },
      {
        title: t("updateCredential"),
        onRowClick: (credential) => {
          setSelectedCredential(credential);
          toggleUpdateDialog();
        },
      },
      {
        title: t("revoke"),
        onRowClick: (credential) => {
          setSelectedCredential(credential);
          toggleDeleteDialog();
        },
      },
    ];
  if (user.email) {
    verifiableCredentialActions.push({
      title: t("credentialOfferSend"),
      onRowClick: (credential) => {
        setCredentialOfferDialogCredential(credential);
      },
    });
  }

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
      {issuedCredentialsModalOpen && (
        <Modal
          variant={ModalVariant.large}
          title={t("issuedCredentials")}
          isOpen={true}
          onClose={() => setIssuedCredentialsModalOpen(undefined)}
          width="90%"
        >
          <IssuedCredentialsDetailCell
            userId={userId}
            credentialScopeName={
              issuedCredentialsModalOpen.credentialScopeName!
            }
            parentRevision={issuedCredentialsModalOpen.revision}
          />
        </Modal>
      )}
      {credentialOfferDialogCredential && (
        <UserVerifiableCredentialOfferDialog
          userId={userId}
          credential={credentialOfferDialogCredential}
          onClose={() => setCredentialOfferDialogCredential(undefined)}
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
            transforms: [cellWidth(30)],
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
            transforms: [cellWidth(15)],
            cellRenderer: ({ createdDate }) =>
              createdDate ? formatDate(new Date(createdDate)) : "—",
          },
          {
            name: "updatedDate",
            displayKey: "updatedAt",
            transforms: [cellWidth(15)],
            cellRenderer: ({ updatedDate }) =>
              updatedDate ? formatDate(new Date(updatedDate)) : "—",
          },
        ]}
        actions={verifiableCredentialActions}
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
