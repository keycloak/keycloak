import type CredentialRepresentation from "@keycloak/keycloak-admin-client/lib/defs/credentialRepresentation";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { HelpItem, useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Divider,
  PageSection,
  PageSectionVariants,
} from "@patternfly/react-core";
import styles from "@patternfly/react-styles/css/components/Table/table";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import {
  Fragment,
  DragEvent as ReactDragEvent,
  useMemo,
  useRef,
  useState,
} from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { toUpperCase } from "../util";
import { FederatedUserLink } from "./FederatedUserLink";
import { CredentialRow } from "./user-credentials/CredentialRow";
import { InlineLabelEdit } from "./user-credentials/InlineLabelEdit";
import { ResetCredentialDialog } from "./user-credentials/ResetCredentialDialog";
import { ResetPasswordDialog } from "./user-credentials/ResetPasswordDialog";
import useFormatDate from "../utils/useFormatDate";

import "./user-credentials.css";

type UserCredentialsProps = {
  user: UserRepresentation;
  setUser: (user: UserRepresentation) => void;
};

type ExpandableCredentialRepresentation = {
  key: string;
  value: CredentialRepresentation[];
  isExpanded: boolean;
};

type UserLabelEdit = {
  status: boolean;
  rowKey: string;
};

type UserCredentialsRowProps = {
  credential: CredentialRepresentation;
  userId: string;
  toggleDelete: (credential: CredentialRepresentation) => void;
  resetPassword: () => void;
  isUserLabelEdit?: UserLabelEdit;
  setIsUserLabelEdit: (isUserLabelEdit: UserLabelEdit) => void;
  refresh: () => void;
};

const UserCredentialsRow = ({
  credential,
  userId,
  toggleDelete,
  resetPassword,
  isUserLabelEdit,
  setIsUserLabelEdit,
  refresh,
}: UserCredentialsRowProps) => (
  <CredentialRow
    key={credential.id}
    credential={credential}
    toggleDelete={() => toggleDelete(credential)}
    resetPassword={resetPassword}
  >
    <InlineLabelEdit
      credential={credential}
      userId={userId}
      isEditable={
        (isUserLabelEdit?.status && isUserLabelEdit.rowKey === credential.id) ||
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

export const UserCredentials = ({ user, setUser }: UserCredentialsProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const [key, setKey] = useState(0);
  const formatDate = useFormatDate();
  const refresh = () => setKey(key + 1);
  const [isOpen, setIsOpen] = useState(false);
  const [openCredentialReset, setOpenCredentialReset] = useState(false);
  const [userCredentials, setUserCredentials] = useState<
    CredentialRepresentation[]
  >([]);
  const [groupedUserCredentials, setGroupedUserCredentials] = useState<
    ExpandableCredentialRepresentation[]
  >([]);
  const [selectedCredential, setSelectedCredential] =
    useState<CredentialRepresentation>({});
  const [isResetPassword, setIsResetPassword] = useState(false);
  const [isUserLabelEdit, setIsUserLabelEdit] = useState<UserLabelEdit>();

  const bodyRef = useRef<HTMLTableSectionElement>(null);
  const [state, setState] = useState({
    draggedItemId: "",
    draggingToItemIndex: -1,
    dragging: false,
    tempItemOrder: [""],
  });

  useFetch(
    () => {
      if (user.enabled) {
        return adminClient.users.getCredentials({ id: user.id! });
      }
      return Promise.resolve([]);
    },
    (credentials) => {
      credentials = [
        ...credentials.filter((c: CredentialRepresentation) => {
          return c.federationLink === undefined;
        }),
      ];
      setUserCredentials(credentials);

      const groupedCredentials = credentials.reduce((r, a) => {
        r[a.type!] = r[a.type!] || [];
        r[a.type!].push(a);
        return r;
      }, Object.create(null));

      const groupedCredentialsArray = Object.keys(groupedCredentials).map(
        (key) => ({ key, value: groupedCredentials[key] }),
      );

      setGroupedUserCredentials(
        groupedCredentialsArray.map((groupedCredential) => ({
          ...groupedCredential,
          isExpanded: false,
        })),
      );
    },
    [key],
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
    continueButtonLabel: t("delete"),
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
        addError("deleteCredentialsError", error);
      }
    },
  });

  const itemOrder = useMemo(
    () =>
      groupedUserCredentials.flatMap((groupedCredential) => [
        groupedCredential.value.map(({ id }) => id).toString(),
        ...(groupedCredential.isExpanded
          ? groupedCredential.value.map((c) => c.id!)
          : []),
      ]),
    [groupedUserCredentials],
  );

  const onDragStart = (evt: ReactDragEvent) => {
    evt.dataTransfer.effectAllowed = "move";
    evt.dataTransfer.setData("text/plain", evt.currentTarget.id);
    const draggedItemId = evt.currentTarget.id;
    evt.currentTarget.classList.add(styles.modifiers.ghostRow);
    evt.currentTarget.setAttribute("aria-pressed", "true");
    setState({ ...state, draggedItemId, dragging: true });
  };

  const moveItem = (items: string[], targetItem: string, toIndex: number) => {
    const fromIndex = items.indexOf(targetItem);
    if (fromIndex === toIndex) {
      return items;
    }
    const result = [...items];
    result.splice(toIndex, 0, result.splice(fromIndex, 1)[0]);
    return result;
  };

  const move = (itemOrder: string[]) => {
    if (!bodyRef.current) return;
    const ulNode = bodyRef.current;
    const nodes = Array.from(ulNode.children);
    if (nodes.every(({ id }, i) => id === itemOrder[i])) {
      return;
    }
    ulNode.replaceChildren();
    itemOrder.forEach((itemId) => {
      ulNode.appendChild(nodes.find(({ id }) => id === itemId)!);
    });
  };

  const onDragCancel = () => {
    if (!bodyRef.current) return;
    Array.from(bodyRef.current.children).forEach((el) => {
      el.classList.remove(styles.modifiers.ghostRow);
      el.setAttribute("aria-pressed", "false");
    });
    setState({
      ...state,
      draggedItemId: "",
      draggingToItemIndex: -1,
      dragging: false,
    });
  };

  const onDragLeave = (evt: ReactDragEvent) => {
    if (!isValidDrop(evt)) {
      move(itemOrder);
      setState({ ...state, draggingToItemIndex: -1 });
    }
  };

  const isValidDrop = (evt: ReactDragEvent) => {
    if (!bodyRef.current) return false;
    const ulRect = bodyRef.current.getBoundingClientRect();
    return (
      evt.clientX > ulRect.x &&
      evt.clientX < ulRect.x + ulRect.width &&
      evt.clientY > ulRect.y &&
      evt.clientY < ulRect.y + ulRect.height
    );
  };

  const onDrop = async (evt: ReactDragEvent) => {
    if (isValidDrop(evt)) {
      await onDragFinish(state.draggedItemId, state.tempItemOrder);
    } else {
      onDragCancel();
    }
  };

  const onDragOver = (evt: ReactDragEvent) => {
    evt.preventDefault();
    const td = evt.target as HTMLTableCellElement;
    const curListItem = td.closest("tr");
    if (
      !curListItem ||
      (bodyRef.current && !bodyRef.current.contains(curListItem)) ||
      curListItem.id === state.draggedItemId
    ) {
      return;
    } else {
      const dragId = curListItem.id;
      const draggingToItemIndex = Array.from(
        bodyRef.current?.children || [],
      ).findIndex((item) => item.id === dragId);
      if (draggingToItemIndex === state.draggingToItemIndex) {
        return;
      }
      const tempItemOrder = moveItem(
        itemOrder,
        state.draggedItemId,
        draggingToItemIndex,
      );
      move(tempItemOrder);
      setState({
        ...state,
        draggingToItemIndex,
        tempItemOrder,
      });
    }
  };

  const onAddRequiredActions = (requiredActions: string[]) => {
    setUser({
      ...user,
      requiredActions: [...(user.requiredActions ?? []), ...requiredActions],
    });
  };

  const onDragEnd = ({ target }: ReactDragEvent) => {
    if (!(target instanceof HTMLTableRowElement)) {
      return;
    }
    target.classList.remove(styles.modifiers.ghostRow);
    target.setAttribute("aria-pressed", "false");
    setState({
      ...state,
      draggedItemId: "",
      draggingToItemIndex: -1,
      dragging: false,
    });
  };

  const onDragFinish = async (dragged: string, newOrder: string[]) => {
    const oldIndex = itemOrder.findIndex((key) => key === dragged);
    const newIndex = newOrder.findIndex((key) => key === dragged);
    const times = newIndex - oldIndex;

    const ids = dragged.split(",");

    try {
      for (const id of ids)
        for (let index = 0; index < Math.abs(times); index++) {
          if (times > 0) {
            await adminClient.users.moveCredentialPositionDown({
              id: user.id!,
              credentialId: id,
              newPreviousCredentialId: itemOrder[newIndex],
            });
          } else {
            await adminClient.users.moveCredentialPositionUp({
              id: user.id!,
              credentialId: id,
            });
          }
        }

      refresh();
      addAlert(t("updatedCredentialMoveSuccess"), AlertVariant.success);
    } catch (error) {
      addError("updatedCredentialMoveError", error);
    }
  };

  const onToggleDelete = (credential: CredentialRepresentation) => {
    setSelectedCredential(credential);
    toggleDeleteDialog();
  };

  const useFederatedCredentials = user.federationLink;
  const [credentialTypes, setCredentialTypes] = useState<
    CredentialRepresentation[]
  >([]);

  useFetch(
    () => {
      if (user.enabled) {
        return adminClient.users.getCredentials({ id: user.id! });
      }
      return Promise.resolve([]);
    },
    (credentials) => {
      credentials = [
        ...credentials.filter((c: CredentialRepresentation) => {
          return c.federationLink !== undefined;
        }),
      ];
      setCredentialTypes(credentials);
    },
    [key],
  );

  if (!credentialTypes) {
    return <KeycloakSpinner />;
  }

  const hasCredentialTypes = credentialTypes.length > 0;
  const noCredentials = groupedUserCredentials.length === 0;
  const noFederatedCredentials =
    !user.credentials || user.credentials.length === 0;
  const emptyState =
    noCredentials && noFederatedCredentials && !hasCredentialTypes;

  return (
    <>
      {isOpen && (
        <ResetPasswordDialog
          user={user}
          isResetPassword={isResetPassword}
          onAddRequiredActions={onAddRequiredActions}
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
      {user.email && !emptyState && (
        <Button
          className="kc-resetCredentialBtn-header"
          variant="primary"
          data-testid="credentialResetBtn"
          onClick={() => setOpenCredentialReset(true)}
        >
          {t("credentialResetBtn")}
        </Button>
      )}
      {userCredentials.length !== 0 &&
        !userCredentials.find((credential) => credential.type === "password") &&
        !credentialTypes.find(
          (credential) => credential.type === "password",
        ) && (
          <>
            <Button
              className="kc-setPasswordBtn-tbl"
              data-testid="setPasswordBtn-table"
              variant="primary"
              form="userCredentials-form"
              onClick={() => {
                setIsOpen(true);
              }}
            >
              {t("setPassword")}
            </Button>
            <Divider />
          </>
        )}
      {groupedUserCredentials.length !== 0 && (
        <PageSection variant={PageSectionVariants.light}>
          <Table variant={"compact"}>
            <Thead>
              <Tr className="kc-table-header">
                <Th>
                  <HelpItem
                    helpText={t("userCredentialsHelpText")}
                    fieldLabelId="userCredentialsHelpTextLabel"
                  />
                </Th>
                <Th aria-hidden="true" />
                <Th>{t("type")}</Th>
                <Th>{t("userLabel")}</Th>
                <Th>{t("createdAt")}</Th>
                <Th>{t("data")}</Th>
                <Th aria-hidden="true" />
                <Th aria-hidden="true" />
              </Tr>
            </Thead>
            <Tbody
              ref={bodyRef}
              onDragOver={onDragOver}
              onDrop={onDragOver}
              onDragLeave={onDragLeave}
            >
              {groupedUserCredentials.map((groupedCredential, rowIndex) => (
                <Fragment key={groupedCredential.key}>
                  <Tr
                    id={groupedCredential.value.map(({ id }) => id).toString()}
                    draggable={groupedUserCredentials.length > 1}
                    onDrop={onDrop}
                    onDragEnd={onDragEnd}
                    onDragStart={onDragStart}
                  >
                    <Td
                      className={
                        groupedUserCredentials.length === 1 ? "one-row" : ""
                      }
                      draggableRow={{
                        id: `draggable-row-${groupedCredential.value.map(
                          ({ id }) => id,
                        )}`,
                      }}
                    />
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
                                  : credential,
                            );
                            setGroupedUserCredentials(rows);
                          },
                        }}
                      />
                    ) : (
                      <Td />
                    )}
                    <Td
                      dataLabel={`columns-${groupedCredential.key}`}
                      className="kc-notExpandableRow-credentialType"
                      data-testid="credentialType"
                    >
                      {toUpperCase(groupedCredential.key)}
                    </Td>
                    {groupedCredential.value.length <= 1 &&
                      groupedCredential.value.map((credential) => (
                        <UserCredentialsRow
                          key={credential.id}
                          credential={credential}
                          userId={user.id!}
                          toggleDelete={onToggleDelete}
                          resetPassword={resetPassword}
                          isUserLabelEdit={isUserLabelEdit}
                          setIsUserLabelEdit={setIsUserLabelEdit}
                          refresh={refresh}
                        />
                      ))}
                  </Tr>
                  {groupedCredential.isExpanded &&
                    groupedCredential.value.map((credential) => (
                      <Tr
                        key={credential.id}
                        id={credential.id}
                        draggable
                        onDrop={onDrop}
                        onDragEnd={onDragEnd}
                        onDragStart={onDragStart}
                      >
                        <Td />
                        <Td
                          className="kc-draggable-dropdown-type-icon"
                          draggableRow={{
                            id: `draggable-row-${groupedCredential.value.map(
                              ({ id }) => id,
                            )}`,
                          }}
                        />
                        <Td
                          dataLabel={`child-columns-${credential.id}`}
                          className="kc-expandableRow-credentialType"
                        >
                          {toUpperCase(credential.type!)}
                        </Td>
                        <UserCredentialsRow
                          credential={credential}
                          userId={user.id!}
                          toggleDelete={onToggleDelete}
                          resetPassword={resetPassword}
                          isUserLabelEdit={isUserLabelEdit}
                          setIsUserLabelEdit={setIsUserLabelEdit}
                          refresh={refresh}
                        />
                      </Tr>
                    ))}
                </Fragment>
              ))}
            </Tbody>
          </Table>
        </PageSection>
      )}
      {useFederatedCredentials && hasCredentialTypes && (
        <PageSection variant={PageSectionVariants.light}>
          <Table variant="compact">
            <Thead>
              <Tr>
                <Th>{t("type")}</Th>
                <Th>{t("providedBy")}</Th>
                <Th>{t("createdAt")}</Th>
                <Th aria-hidden="true" />
              </Tr>
            </Thead>
            <Tbody>
              {credentialTypes.map((credential) => (
                <Tr key={credential.type}>
                  <Td>
                    <b>{credential.type}</b>
                  </Td>
                  <Td>
                    <FederatedUserLink user={user} />
                  </Td>
                  <Td>{formatDate(new Date(credential.createdDate!))}</Td>
                  {credential.type === "password" && (
                    <Td modifier="fitContent">
                      <Button variant="secondary" onClick={toggleModal}>
                        {t("setPassword")}
                      </Button>
                    </Td>
                  )}
                </Tr>
              ))}
            </Tbody>
          </Table>
        </PageSection>
      )}
      {emptyState && (
        <ListEmptyState
          hasIcon
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
