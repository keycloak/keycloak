import React, { Fragment, useState } from "react";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Divider,
} from "@patternfly/react-core";
import {
  TableComposable,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { useTranslation } from "react-i18next";
import { useAlerts } from "../components/alert/Alerts";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import type CredentialRepresentation from "@keycloak/keycloak-admin-client/lib/defs/credentialRepresentation";
import { ResetPasswordDialog } from "./user-credentials/ResetPasswordDialog";
import { ResetCredentialDialog } from "./user-credentials/ResetCredentialDialog";
import { InlineLabelEdit } from "./user-credentials/InlineLabelEdit";

import "./user-credentials.css";
import { CredentialRow } from "./user-credentials/CredentialRow";
import { toUpperCase } from "../util";

type UserCredentialsProps = {
  user: UserRepresentation;
};

type ExpandableCredentialRepresentation = {
  key: string;
  value: CredentialRepresentation[];
  isExpanded: boolean;
};

export const UserCredentials = ({ user }: UserCredentialsProps) => {
  const { t } = useTranslation("users");
  const { addAlert, addError } = useAlerts();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const [isOpen, setIsOpen] = useState(false);
  const [openCredentialReset, setOpenCredentialReset] = useState(false);
  const adminClient = useAdminClient();
  const [userCredentials, setUserCredentials] = useState<
    CredentialRepresentation[]
  >([]);
  const [groupedUserCredentials, setGroupedUserCredentials] = useState<
    ExpandableCredentialRepresentation[]
  >([]);
  const [selectedCredential, setSelectedCredential] =
    useState<CredentialRepresentation>({});
  const [isResetPassword, setIsResetPassword] = useState(false);
  const [isUserLabelEdit, setIsUserLabelEdit] = useState<{
    status: boolean;
    rowKey: string;
  }>();

  useFetch(
    () => adminClient.users.getCredentials({ id: user.id! }),
    (credentials) => {
      setUserCredentials(credentials);

      const groupedCredentials = credentials.reduce((r, a) => {
        r[a.type!] = r[a.type!] || [];
        r[a.type!].push(a);
        return r;
      }, Object.create(null));

      const groupedCredentialsArray = Object.keys(groupedCredentials).map(
        (key) => ({ key, value: groupedCredentials[key] })
      );

      setGroupedUserCredentials(
        groupedCredentialsArray.map((groupedCredential) => ({
          ...groupedCredential,
          isExpanded: false,
        }))
      );
    },
    [key]
  );

  const passwordTypeFinder = userCredentials.find(
    (credential) => credential.type === "password"
  );

  const toggleModal = () => setIsOpen(!isOpen);

  const toggleCredentialsResetModal = () => {
    setOpenCredentialReset(!openCredentialReset);
  };

  const resetPassword = () => {
    setIsResetPassword(true);
    toggleModal();
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteCredentialsConfirmTitle"),
    messageKey: t("deleteCredentialsConfirm"),
    continueButtonLabel: t("common:delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.users.deleteCredential({
          id: user.id!,
          credentialId: selectedCredential.id!,
        });
        addAlert(t("deleteCredentialsSuccess"), AlertVariant.success);
        setKey((key) => key + 1);
      } catch (error) {
        addError("users:deleteCredentialsError", error);
      }
    },
  });

  const Row = ({ credential }: { credential: CredentialRepresentation }) => (
    <CredentialRow
      key={credential.id}
      credential={credential}
      toggleDelete={() => {
        setSelectedCredential(credential);
        toggleDeleteDialog();
      }}
      resetPassword={resetPassword}
    >
      <InlineLabelEdit
        credential={credential}
        userId={user.id!}
        isEditable={
          (isUserLabelEdit?.status &&
            isUserLabelEdit.rowKey === credential.id) ||
          false
        }
        toggle={() => {
          setIsUserLabelEdit({
            status: !isUserLabelEdit?.status,
            rowKey: credential.id!,
          });
          if (isUserLabelEdit?.status) {
            refresh();
          }
        }}
      />
    </CredentialRow>
  );
  return (
    <>
      {isOpen && (
        <ResetPasswordDialog
          user={user}
          isResetPassword={isResetPassword}
          refresh={refresh}
          onClose={() => setIsOpen(false)}
        />
      )}
      {openCredentialReset && (
        <ResetCredentialDialog
          userId={user.id!}
          onClose={() => setOpenCredentialReset(false)}
        />
      )}
      <DeleteConfirm />
      {userCredentials.length !== 0 && passwordTypeFinder === undefined && (
        <>
          <Button
            key={`confirmSaveBtn-table-${user.id}`}
            className="kc-setPasswordBtn-tbl"
            data-testid="setPasswordBtn-table"
            variant="primary"
            form="userCredentials-form"
            onClick={() => {
              setIsOpen(true);
            }}
          >
            {t("savePassword")}
          </Button>
          <Divider />
        </>
      )}
      {groupedUserCredentials.length !== 0 ? (
        <>
          {user.email && (
            <Button
              className="resetCredentialBtn-header"
              variant="primary"
              data-testid="credentialResetBtn"
              onClick={() => setOpenCredentialReset(true)}
            >
              {t("credentialResetBtn")}
            </Button>
          )}
          <TableComposable aria-label="password-data-table" variant={"compact"}>
            <Thead>
              <Tr>
                <Th>
                  <HelpItem
                    helpText="users:userCredentialsHelpText"
                    fieldLabelId="users:userCredentialsHelpTextLabel"
                  />
                </Th>
                <Th>{t("type")}</Th>
                <Th>{t("userLabel")}</Th>
                <Th>{t("data")}</Th>
                <Th />
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {groupedUserCredentials.map((groupedCredential, rowIndex) => (
                <Fragment key={`table-${groupedCredential.key}`}>
                  <Tr>
                    {groupedCredential.value.length > 1 ? (
                      <Td
                        className="kc-expandRow-btn"
                        expand={{
                          rowIndex,
                          isExpanded: groupedCredential.isExpanded,
                          onToggle: (_, rowIndex) => {
                            const rows = groupedUserCredentials.map(
                              (credential, index) =>
                                index === rowIndex
                                  ? {
                                      ...credential,
                                      isExpanded: !credential.isExpanded,
                                    }
                                  : credential
                            );
                            setGroupedUserCredentials(rows);
                          },
                        }}
                      />
                    ) : (
                      <Td />
                    )}
                    <Td
                      key={`table-item-${groupedCredential.key}`}
                      dataLabel={`columns-${groupedCredential.key}`}
                      className="kc-notExpandableRow-credentialType"
                    >
                      {toUpperCase(groupedCredential.key)}
                    </Td>
                    {groupedCredential.value.length <= 1 &&
                      groupedCredential.value.map((credential) => (
                        <Row
                          key={`subrow-${credential.id}`}
                          credential={credential}
                        />
                      ))}
                  </Tr>
                  {groupedCredential.isExpanded &&
                    groupedCredential.value.map((credential) => (
                      <Tr key={`child-key-${credential.id}`}>
                        <Td />
                        <Td
                          dataLabel={`child-columns-${credential.id}`}
                          className="kc-expandableRow-credentialType"
                        >
                          {toUpperCase(credential.type!)}
                        </Td>
                        <Row credential={credential} />
                      </Tr>
                    ))}
                </Fragment>
              ))}
            </Tbody>
          </TableComposable>
        </>
      ) : (
        <ListEmptyState
          hasIcon={true}
          message={t("noCredentials")}
          instructions={t("noCredentialsText")}
          primaryActionText={t("setPassword")}
          onPrimaryAction={toggleModal}
          secondaryActions={
            user.email
              ? [
                  {
                    text: t("credentialResetBtn"),
                    onClick: toggleCredentialsResetModal,
                    type: ButtonVariant.link,
                  },
                ]
              : undefined
          }
        />
      )}
    </>
  );
};
