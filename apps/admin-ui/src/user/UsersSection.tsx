import { useState } from "react";
import { useHistory } from "react-router-dom";
import { Link, useNavigate } from "react-router-dom-v5-compat";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Dropdown,
  DropdownItem,
  EmptyState,
  InputGroup,
  KebabToggle,
  Label,
  PageSection,
  Tab,
  TabTitleText,
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

import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { emptyFormatter } from "../util";
import { toUser } from "./routes/User";
import { toAddUser } from "./routes/AddUser";
import helpUrls from "../help-urls";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { PermissionsTab } from "../components/permission-tab/PermissionTab";
import { toUsers, UserTab } from "./routes/Users";
import {
  routableTab,
  RoutableTabs,
} from "../components/routable-tabs/RoutableTabs";
import { useAccess } from "../context/access/Access";
import { BruteUser, findUsers } from "../components/role-mapping/resource";

import "./user-section.css";

export default function UsersSection() {
  const { t } = useTranslation("users");
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm: realmName } = useRealm();
  const history = useHistory();
  const navigate = useNavigate();
  const [userStorage, setUserStorage] = useState<ComponentRepresentation[]>();
  const [searchUser, setSearchUser] = useState<string>();
  const [realm, setRealm] = useState<RealmRepresentation | undefined>();
  const [kebabOpen, setKebabOpen] = useState(false);
  const [selectedRows, setSelectedRows] = useState<UserRepresentation[]>([]);
  const { profileInfo } = useServerInfo();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-users");

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
      setUserStorage(storageProviders);
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
        adminClient,
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
            {t("temporaryDisabled")}
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

  const toolbar = (
    <>
      <ToolbarItem>
        <Button data-testid="add-user" onClick={goToCreate}>
          {t("addUser")}
        </Button>
      </ToolbarItem>
      {!realm.bruteForceProtected ? (
        <ToolbarItem>
          <Button
            variant={ButtonVariant.link}
            onClick={toggleDeleteDialog}
            data-testid="delete-user-btn"
            isDisabled={selectedRows.length === 0}
          >
            {t("deleteUser")}
          </Button>
        </ToolbarItem>
      ) : (
        <ToolbarItem>
          <Dropdown
            toggle={<KebabToggle onToggle={(open) => setKebabOpen(open)} />}
            isOpen={kebabOpen}
            isPlain
            dropdownItems={[
              <DropdownItem
                key="deleteUser"
                component="button"
                isDisabled={selectedRows.length === 0}
                onClick={() => {
                  toggleDeleteDialog();
                  setKebabOpen(false);
                }}
              >
                {t("deleteUser")}
              </DropdownItem>,

              <DropdownItem
                key="unlock"
                component="button"
                onClick={() => {
                  toggleUnlockUsersDialog();
                  setKebabOpen(false);
                }}
              >
                {t("unlockAllUsers")}
              </DropdownItem>,
            ]}
          />
        </ToolbarItem>
      )}
    </>
  );

  const route = (tab: UserTab) =>
    routableTab({
      to: toUsers({
        realm: realmName,
        tab,
      }),
      history,
    });

  return (
    <>
      <DeleteConfirm />
      <UnlockUsersConfirm />
      <ViewHeader
        titleKey="users:title"
        subKey="users:usersExplain"
        helpUrl={helpUrls.usersUrl}
        divider={false}
      />
      <PageSection
        data-testid="users-page"
        variant="light"
        className="pf-u-p-0"
      >
        <RoutableTabs
          data-testid="user-tabs"
          defaultLocation={toUsers({
            realm: realmName,
            tab: "list",
          })}
          isBox
          mountOnEnter
        >
          <Tab
            id="list"
            data-testid="listTab"
            title={<TabTitleText>{t("userList")}</TabTitleText>}
            {...route("list")}
          >
            <KeycloakDataTable
              key={key}
              loader={loader}
              isPaginated
              ariaLabelKey="users:title"
              searchPlaceholderKey="users:searchForUser"
              canSelectAll
              onSelect={(rows) => setSelectedRows([...rows])}
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
                              onChange={(value) => {
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
                        {toolbar}
                      </ToolbarContent>
                    </Toolbar>
                    <EmptyState data-testid="empty-state" variant="large">
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
              toolbarItem={isManager ? toolbar : undefined}
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
          </Tab>
          {!profileInfo?.disabledFeatures?.includes(
            "ADMIN_FINE_GRAINED_AUTHZ"
          ) && (
            <Tab
              id="permissions"
              data-testid="permissionsTab"
              title={<TabTitleText>{t("common:permissions")}</TabTitleText>}
              {...route("permissions")}
            >
              <PermissionsTab type="users" />
            </Tab>
          )}
        </RoutableTabs>
      </PageSection>
    </>
  );
}
