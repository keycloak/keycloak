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
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useHistory } from "react-router-dom";
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

import "./user-section.css";
import helpUrls from "../help-urls";

type BruteUser = UserRepresentation & {
  brute?: Record<string, object>;
};

export default function UsersSection() {
  const { t } = useTranslation("users");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm: realmName } = useRealm();
  const history = useHistory();
  const [listUsers, setListUsers] = useState(false);
  const [searchUser, setSearchUser] = useState<string>();
  const [realm, setRealm] = useState<RealmRepresentation | undefined>();
  const [kebabOpen, setKebabOpen] = useState(false);
  const [selectedRows, setSelectedRows] = useState<UserRepresentation[]>([]);

  const [key, setKey] = useState("");
  const refresh = () => setKey(`${new Date().getTime()}`);

  useFetch(
    () => {
      const testParams = {
        type: "org.keycloak.storage.UserStorageProvider",
      };

      return Promise.all([
        adminClient.components.find(testParams),
        adminClient.realms.findOne({ realm: realmName }),
      ]).catch(
        () =>
          [[], undefined] as [
            ComponentRepresentation[],
            RealmRepresentation | undefined
          ]
      );
    },
    ([storageProviders, realm]) => {
      //should *only* list users when no user federation is configured
      setListUsers(!(storageProviders.length > 0));
      setRealm(realm);
      refresh();
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
      const users = await adminClient.users.find({ ...params });
      if (realm?.bruteForceProtected) {
        const brutes = await Promise.all(
          users.map((user: BruteUser) =>
            adminClient.attackDetection.findOne({
              id: user.id!,
            })
          )
        );
        for (let index = 0; index < users.length; index++) {
          const user: BruteUser = users[index];
          user.brute = brutes[index];
        }
      }
      return users;
    } catch (error) {
      addError("users:noUsersFoundError", error);
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
        {user.brute?.disabled && (
          <Label key={user.id} color="orange" icon={<WarningTriangleIcon />}>
            {t("temporaryDisabled")}
          </Label>
        )}
        {user.enabled && !user.brute?.disabled && "—"}
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

  const goToCreate = () => history.push(toAddUser({ realm: realmName }));

  const toolbar = (
    <>
      <ToolbarItem>
        <Button data-testid="add-user" onClick={goToCreate}>
          {t("addUser")}
        </Button>
      </ToolbarItem>
      {!realm?.bruteForceProtected ? (
        <ToolbarItem>
          <Button
            variant={ButtonVariant.plain}
            onClick={toggleDeleteDialog}
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

  return (
    <>
      <DeleteConfirm />
      <UnlockUsersConfirm />
      <ViewHeader
        titleKey="users:title"
        subKey="users:usersExplain"
        helpUrl={helpUrls.usersUrl}
      />
      <PageSection
        data-testid="users-page"
        variant="light"
        className="pf-u-p-0"
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
          toolbarItem={toolbar}
          actions={[
            {
              title: t("common:delete"),
              onRowClick: (user) => {
                setSelectedRows([user]);
                toggleDeleteDialog();
              },
            },
          ]}
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
      </PageSection>
    </>
  );
}
