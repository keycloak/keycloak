import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  EmptyState,
  InputGroup,
  Label,
  Text,
  TextContent,
  TextInput,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Tooltip,
} from "@patternfly/react-core";
import {
  ExclamationCircleIcon,
  InfoCircleIcon,
  SearchIcon,
  WarningTriangleIcon,
} from "@patternfly/react-icons";
import type { IRowData } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";

import { adminClient } from "../../admin-client";
import { useAlerts } from "../alert/Alerts";
import { useConfirmDialog } from "../confirm-dialog/ConfirmDialog";
import { KeycloakSpinner } from "../keycloak-spinner/KeycloakSpinner";
import { ListEmptyState } from "../list-empty-state/ListEmptyState";
import { BruteUser, findUsers } from "../role-mapping/resource";
import { KeycloakDataTable } from "../table-toolbar/KeycloakDataTable";
import { useRealm } from "../../context/realm-context/RealmContext";
import { emptyFormatter } from "../../util";
import { useFetch } from "../../utils/useFetch";
import { toAddUser } from "../../user/routes/AddUser";
import { toUser } from "../../user/routes/User";
import { UserDataTableToolbarItems } from "./UserDataTableToolbarItems";

export function UserDataTable() {
  const { t } = useTranslation("users");
  const { addAlert, addError } = useAlerts();
  const { realm: realmName } = useRealm();
  const navigate = useNavigate();
  const [userStorage, setUserStorage] = useState<ComponentRepresentation[]>();
  const [searchUser, setSearchUser] = useState<string>();
  const [realm, setRealm] = useState<RealmRepresentation | undefined>();
  const [selectedRows, setSelectedRows] = useState<UserRepresentation[]>([]);

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  useFetch(
    async () => {
      const testParams = {
        type: "org.keycloak.storage.UserStorageProvider",
      };

      try {
        return await Promise.all([
          adminClient.components.find(testParams),
          adminClient.realms.findOne({ realm: realmName }),
        ]);
      } catch {
        return [[], {}] as [
          ComponentRepresentation[],
          RealmRepresentation | undefined
        ];
      }
    },
    ([storageProviders, realm]) => {
      setUserStorage(
        storageProviders.filter((p) => p.config?.enabled[0] === "true")
      );
      setRealm(realm);
    },
    []
  );

  const UserDetailLink = (user: UserRepresentation) => (
    <Link
      key={user.username}
      to={toUser({ realm: realmName, id: user.id!, tab: "settings" })}
    >
      {user.username}
    </Link>
  );

  const loader = async (first?: number, max?: number, search?: string) => {
    const params: { [name: string]: string | number } = {
      first: first!,
      max: max!,
    };

    const searchParam = search || searchUser || "";
    if (searchParam) {
      params.search = searchParam;
    }

    if (!listUsers && !searchParam) {
      return [];
    }

    try {
      return await findUsers({
        briefRepresentation: true,
        ...params,
      });
    } catch (error) {
      if (userStorage?.length) {
        addError("users:noUsersFoundErrorStorage", error);
      } else {
        addError("users:noUsersFoundError", error);
      }
      return [];
    }
  };

  const [toggleUnlockUsersDialog, UnlockUsersConfirm] = useConfirmDialog({
    titleKey: "users:unlockAllUsers",
    messageKey: "users:unlockUsersConfirm",
    continueButtonLabel: "users:unlock",
    onConfirm: async () => {
      try {
        await adminClient.attackDetection.delAll();
        refresh();
        addAlert(t("unlockUsersSuccess"), AlertVariant.success);
      } catch (error) {
        addError("users:unlockUsersError", error);
      }
    },
  });

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "users:deleteConfirm",
    messageKey: t("deleteConfirmDialog", { count: selectedRows.length }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        for (const user of selectedRows) {
          await adminClient.users.del({ id: user.id! });
        }
        setSelectedRows([]);
        refresh();
        addAlert(t("userDeletedSuccess"), AlertVariant.success);
      } catch (error) {
        addError("users:userDeletedError", error);
      }
    },
  });

  const StatusRow = (user: BruteUser) => {
    return (
      <>
        {!user.enabled && (
          <Label key={user.id} color="red" icon={<InfoCircleIcon />}>
            {t("disabled")}
          </Label>
        )}
        {user.bruteForceStatus?.disabled && (
          <Label key={user.id} color="orange" icon={<WarningTriangleIcon />}>
            {t("temporaryLocked")}
          </Label>
        )}
        {user.enabled && !user.bruteForceStatus?.disabled && "—"}
      </>
    );
  };

  const ValidatedEmail = (user: UserRepresentation) => {
    return (
      <>
        {!user.emailVerified && (
          <Tooltip
            key={`email-verified-${user.id}`}
            content={<>{t("notVerified")}</>}
          >
            <ExclamationCircleIcon className="keycloak__user-section__email-verified" />
          </Tooltip>
        )}{" "}
        {emptyFormatter()(user.email)}
      </>
    );
  };

  const goToCreate = () => navigate(toAddUser({ realm: realmName }));

  if (!userStorage || !realm) {
    return <KeycloakSpinner />;
  }

  //should *only* list users when no user federation is configured
  const listUsers = !(userStorage.length > 0);

  return (
    <>
      <DeleteConfirm />
      <UnlockUsersConfirm />
      <KeycloakDataTable
        key={key}
        loader={loader}
        isPaginated
        ariaLabelKey="users:title"
        searchPlaceholderKey="users:searchForUser"
        canSelectAll
        onSelect={(rows: any[]) => setSelectedRows([...rows])}
        emptyState={
          !listUsers ? (
            <>
              <Toolbar>
                <ToolbarContent>
                  <ToolbarItem>
                    <InputGroup>
                      <TextInput
                        name="search-input"
                        type="search"
                        aria-label={t("search")}
                        placeholder={t("users:searchForUser")}
                        onChange={(_e, value) => {
                          setSearchUser(value);
                        }}
                        onKeyDown={(e) => {
                          if (e.key === "Enter") {
                            refresh();
                          }
                        }}
                      />
                      <Button
                        variant={ButtonVariant.control}
                        aria-label={t("common:search")}
                        onClick={refresh}
                      >
                        <SearchIcon />
                      </Button>
                    </InputGroup>
                  </ToolbarItem>
                  <UserDataTableToolbarItems
                    realm={realm}
                    hasSelectedRows={selectedRows.length === 0}
                    toggleDeleteDialog={toggleDeleteDialog}
                    toggleUnlockUsersDialog={toggleUnlockUsersDialog}
                    goToCreate={goToCreate}
                  />
                </ToolbarContent>
              </Toolbar>
              <EmptyState data-testid="empty-state" variant="lg">
                <TextContent className="kc-search-users-text">
                  <Text>{t("searchForUserDescription")}</Text>
                </TextContent>
              </EmptyState>
            </>
          ) : (
            <ListEmptyState
              message={t("noUsersFound")}
              instructions={t("emptyInstructions")}
              primaryActionText={t("createNewUser")}
              onPrimaryAction={goToCreate}
            />
          )
        }
        toolbarItem={
          <UserDataTableToolbarItems
            realm={realm}
            hasSelectedRows={selectedRows.length === 0}
            toggleDeleteDialog={toggleDeleteDialog}
            toggleUnlockUsersDialog={toggleUnlockUsersDialog}
            goToCreate={goToCreate}
          />
        }
        actionResolver={(rowData: IRowData) => {
          const user: UserRepresentation = rowData.data;
          if (!user.access?.manage) return [];

          return [
            {
              title: t("common:delete"),
              onClick: () => {
                setSelectedRows([user]);
                toggleDeleteDialog();
              },
            },
          ];
        }}
        columns={[
          {
            name: "username",
            displayKey: "users:username",
            cellRenderer: UserDetailLink,
          },
          {
            name: "email",
            displayKey: "users:email",
            cellRenderer: ValidatedEmail,
          },
          {
            name: "lastName",
            displayKey: "users:lastName",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "firstName",
            displayKey: "users:firstName",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "status",
            displayKey: "users:status",
            cellRenderer: StatusRow,
          },
        ]}
      />
    </>
  );
}
