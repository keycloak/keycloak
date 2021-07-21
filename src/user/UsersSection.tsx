import {
  AlertVariant,
  Button,
  ButtonVariant,
  Label,
  PageSection,
  ToolbarItem,
  Tooltip,
} from "@patternfly/react-core";
import {
  ExclamationCircleIcon,
  InfoCircleIcon,
  WarningTriangleIcon,
} from "@patternfly/react-icons";
import type UserRepresentation from "keycloak-admin/lib/defs/userRepresentation";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useHistory, useRouteMatch } from "react-router-dom";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { emptyFormatter } from "../util";
import { SearchUser } from "./SearchUser";
import "./user-section.css";

type BruteUser = UserRepresentation & {
  brute?: Record<string, object>;
};

export const UsersSection = () => {
  const { t } = useTranslation("users");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();
  const { realm: realmName } = useRealm();
  const history = useHistory();
  const { url } = useRouteMatch();
  const [listUsers, setListUsers] = useState(false);
  const [initialSearch, setInitialSearch] = useState("");
  const [selectedRows, setSelectedRows] = useState<UserRepresentation[]>([]);
  const [search, setSearch] = useState("");

  const [key, setKey] = useState("");
  const refresh = () => setKey(`${new Date().getTime()}`);

  useFetch(
    () => {
      const testParams = {
        type: "org.keycloak.storage.UserStorageProvider",
      };

      return Promise.all([
        adminClient.components.find(testParams),
        adminClient.users.count(),
      ]);
    },
    (response) => {
      //should *only* list users when no user federation is configured and uses count > 100
      setListUsers(
        !((response[0] && response[0].length > 0) || response[1] > 100)
      );
    },
    []
  );

  const UserDetailLink = (user: UserRepresentation) => (
    <>
      <Link key={user.username} to={`${url}/${user.id}/settings`}>
        {user.username}
      </Link>
    </>
  );

  const loader = async (first?: number, max?: number, search?: string) => {
    const params: { [name: string]: string | number } = {
      first: first!,
      max: max!,
    };
    const searchParam = search || initialSearch || "";
    if (searchParam) {
      params.search = searchParam;
      setSearch(searchParam);
    }

    if (!listUsers && !searchParam) {
      return [];
    }
    try {
      const users = await adminClient.users.find({ ...params });
      const realm = await adminClient.realms.findOne({ realm: realmName });
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
      addAlert(t("noUsersFoundError", { error }), AlertVariant.danger);
      return [];
    }
  };

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
        addAlert(t("userDeletedError", { error }), AlertVariant.danger);
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

  const goToCreate = () => history.push(`${url}/add-user`);

  return (
    <>
      <DeleteConfirm />
      <ViewHeader titleKey="users:title" />
      <PageSection
        data-testid="users-page"
        variant="light"
        className="pf-u-p-0"
      >
        {!listUsers && !initialSearch && (
          <SearchUser
            onSearch={(search) => {
              setInitialSearch(search);
            }}
          />
        )}
        {(listUsers || initialSearch) && (
          <KeycloakDataTable
            key={key}
            loader={loader}
            isPaginated
            ariaLabelKey="users:title"
            searchPlaceholderKey="users:searchForUser"
            canSelectAll
            onSelect={(rows) => setSelectedRows([...rows])}
            emptyState={
              !search ? (
                <ListEmptyState
                  message={t("noUsersFound")}
                  instructions={t("emptyInstructions")}
                  primaryActionText={t("createNewUser")}
                  onPrimaryAction={goToCreate}
                />
              ) : (
                ""
              )
            }
            toolbarItem={
              <>
                <ToolbarItem>
                  <Button data-testid="add-user" onClick={goToCreate}>
                    {t("addUser")}
                  </Button>
                </ToolbarItem>
                <ToolbarItem>
                  <Button
                    variant={ButtonVariant.plain}
                    onClick={toggleDeleteDialog}
                  >
                    {t("deleteUser")}
                  </Button>
                </ToolbarItem>
              </>
            }
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
        )}
      </PageSection>
    </>
  );
};
