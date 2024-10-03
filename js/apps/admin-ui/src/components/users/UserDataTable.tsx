import type { UserProfileConfig } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  KeycloakDataTable,
  KeycloakSpinner,
  ListEmptyState,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Chip,
  ChipGroup,
  EmptyState,
  FlexItem,
  Label,
  Text,
  TextContent,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Tooltip,
} from "@patternfly/react-core";
import {
  ExclamationCircleIcon,
  InfoCircleIcon,
  WarningTriangleIcon,
} from "@patternfly/react-icons";
import type { IRowData } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { fetchRealmInfo } from "../../context/auth/admin-ui-endpoint";
import { UiRealmInfo } from "../../context/auth/uiRealmInfo";
import { useRealm } from "../../context/realm-context/RealmContext";
import { SearchType } from "../../user/details/SearchFilter";
import { toAddUser } from "../../user/routes/AddUser";
import { toUser } from "../../user/routes/User";
import { emptyFormatter } from "../../util";
import { useConfirmDialog } from "../confirm-dialog/ConfirmDialog";
import { BruteUser, findUsers } from "../role-mapping/resource";
import { UserDataTableToolbarItems } from "./UserDataTableToolbarItems";

export type UserFilter = {
  exact: boolean;
  userAttribute: UserAttribute[];
};

export type UserAttribute = {
  name: string;
  displayName: string;
  value: string;
};

const UserDetailLink = (user: BruteUser) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  return (
    <>
      <Link to={toUser({ realm, id: user.id!, tab: "settings" })}>
        {user.username}
        <StatusRow user={user} />
      </Link>
      {user.attributes?.["is_temporary_admin"][0] === "true" && (
        <Tooltip content={t("temporaryAdmin")}>
          <WarningTriangleIcon
            className="pf-v5-u-ml-sm"
            id="temporary-admin-label"
          />
        </Tooltip>
      )}
    </>
  );
};

type StatusRowProps = {
  user: BruteUser;
};

const StatusRow = ({ user }: StatusRowProps) => {
  const { t } = useTranslation();
  return (
    <>
      {!user.enabled && (
        <Label color="red" icon={<InfoCircleIcon />}>
          {t("disabled")}
        </Label>
      )}
      {user.bruteForceStatus?.disabled && (
        <Label color="orange" icon={<WarningTriangleIcon />}>
          {t("temporaryLocked")}
        </Label>
      )}
    </>
  );
};

const ValidatedEmail = (user: UserRepresentation) => {
  const { t } = useTranslation();
  return (
    <>
      {!user.emailVerified && (
        <Tooltip content={t("notVerified")}>
          <ExclamationCircleIcon className="keycloak__user-section__email-verified" />
        </Tooltip>
      )}{" "}
      {emptyFormatter()(user.email)}
    </>
  );
};

export function UserDataTable() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { realm: realmName, realmRepresentation: realm } = useRealm();
  const navigate = useNavigate();
  const [uiRealmInfo, setUiRealmInfo] = useState<UiRealmInfo>({});
  const [searchUser, setSearchUser] = useState("");
  const [selectedRows, setSelectedRows] = useState<UserRepresentation[]>([]);
  const [searchType, setSearchType] = useState<SearchType>("default");
  const [searchDropdownOpen, setSearchDropdownOpen] = useState(false);
  const [activeFilters, setActiveFilters] = useState<UserFilter>({
    exact: false,
    userAttribute: [],
  });
  const [profile, setProfile] = useState<UserProfileConfig>({});
  const [query, setQuery] = useState("");

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  useFetch(
    async () => {
      try {
        return await Promise.all([
          fetchRealmInfo(adminClient),
          adminClient.users.getProfile(),
        ]);
      } catch {
        return [{}, {}] as [UiRealmInfo, UserProfileConfig];
      }
    },
    ([uiRealmInfo, profile]) => {
      setUiRealmInfo(uiRealmInfo);
      setProfile(profile);
    },
    [],
  );

  const loader = async (first?: number, max?: number, search?: string) => {
    const params: { [name: string]: string | number | boolean } = {
      first: first!,
      max: max!,
      q: query!,
      exact: activeFilters.exact,
    };

    const searchParam = search || searchUser || "";
    if (searchParam) {
      params.search = searchParam;
    }

    if (!listUsers && !(params.search || params.q)) {
      return [];
    }

    try {
      return await findUsers(adminClient, {
        briefRepresentation: true,
        ...params,
      });
    } catch (error) {
      if (uiRealmInfo.userProfileProvidersEnabled) {
        addError("noUsersFoundErrorStorage", error);
      } else {
        addError("noUsersFoundError", error);
      }
      return [];
    }
  };

  const [toggleUnlockUsersDialog, UnlockUsersConfirm] = useConfirmDialog({
    titleKey: "unlockAllUsers",
    messageKey: "unlockUsersConfirm",
    continueButtonLabel: "unlock",
    onConfirm: async () => {
      try {
        await adminClient.attackDetection.delAll();
        refresh();
        addAlert(t("unlockUsersSuccess"), AlertVariant.success);
      } catch (error) {
        addError("unlockUsersError", error);
      }
    },
  });

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteConfirmUsers",
    messageKey: t("deleteConfirmDialog", { count: selectedRows.length }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        for (const user of selectedRows) {
          await adminClient.users.del({ id: user.id! });
        }
        setSelectedRows([]);
        clearAllFilters();
        addAlert(t("userDeletedSuccess"), AlertVariant.success);
      } catch (error) {
        addError("userDeletedError", error);
      }
    },
  });

  const goToCreate = () => navigate(toAddUser({ realm: realmName }));

  if (!uiRealmInfo || !realm) {
    return <KeycloakSpinner />;
  }

  //should *only* list users when no user federation is configured
  const listUsers = !uiRealmInfo.userProfileProvidersEnabled;

  const clearAllFilters = () => {
    setActiveFilters({ exact: false, userAttribute: [] });
    setSearchUser("");
    setQuery("");
    refresh();
  };

  const createQueryString = (filters: UserFilter) => {
    return filters.userAttribute
      .map((filter) => `${filter.name}:${filter.value}`)
      .join(" ");
  };

  const searchUserWithAttributes = () => {
    const attributes = createQueryString(activeFilters);
    setQuery(attributes);
    refresh();
  };

  const createAttributeSearchChips = () => {
    return (
      <FlexItem>
        {activeFilters.userAttribute.length > 0 && (
          <>
            {Object.values(activeFilters.userAttribute).map((entry) => {
              return (
                <ChipGroup
                  className="pf-v5-u-mt-md pf-v5-u-mr-md"
                  data-testid="user-attribute-search-chips-group"
                  key={entry.name}
                  categoryName={
                    entry.displayName.length ? entry.displayName : entry.name
                  }
                  isClosable
                  onClick={(event) => {
                    event.stopPropagation();

                    const filtered = [...activeFilters.userAttribute].filter(
                      (chip) => chip.name !== entry.name,
                    );
                    const active = {
                      userAttribute: filtered,
                      exact: activeFilters.exact,
                    };

                    setActiveFilters(active);
                    setQuery(createQueryString(active));
                    refresh();
                  }}
                >
                  <Chip key={entry.name} isReadOnly>
                    {entry.value}
                  </Chip>
                </ChipGroup>
              );
            })}
          </>
        )}
      </FlexItem>
    );
  };

  const toolbar = () => {
    return (
      <UserDataTableToolbarItems
        searchDropdownOpen={searchDropdownOpen}
        setSearchDropdownOpen={setSearchDropdownOpen}
        realm={realm}
        hasSelectedRows={selectedRows.length === 0}
        toggleDeleteDialog={toggleDeleteDialog}
        toggleUnlockUsersDialog={toggleUnlockUsersDialog}
        goToCreate={goToCreate}
        searchType={searchType}
        setSearchType={setSearchType}
        searchUser={searchUser}
        setSearchUser={setSearchUser}
        activeFilters={activeFilters}
        setActiveFilters={setActiveFilters}
        refresh={refresh}
        profile={profile}
        clearAllFilters={clearAllFilters}
        createAttributeSearchChips={createAttributeSearchChips}
        searchUserWithAttributes={searchUserWithAttributes}
      />
    );
  };

  const subtoolbar = () => {
    if (!activeFilters.userAttribute.length) {
      return;
    }
    return (
      <div className="user-attribute-search-form-subtoolbar">
        <ToolbarItem>{createAttributeSearchChips()}</ToolbarItem>
        <ToolbarItem>
          <Button
            variant="link"
            onClick={() => {
              clearAllFilters();
            }}
          >
            {t("clearAllFilters")}
          </Button>
        </ToolbarItem>
      </div>
    );
  };

  return (
    <>
      <DeleteConfirm />
      <UnlockUsersConfirm />
      <KeycloakDataTable
        isSearching={
          searchUser !== "" || activeFilters.userAttribute.length !== 0
        }
        key={key}
        loader={loader}
        isPaginated
        ariaLabelKey="titleUsers"
        canSelectAll
        onSelect={(rows: UserRepresentation[]) => setSelectedRows([...rows])}
        emptyState={
          !listUsers ? (
            <>
              <Toolbar>
                <ToolbarContent>{toolbar()}</ToolbarContent>
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
        toolbarItem={toolbar()}
        subToolbar={subtoolbar()}
        actionResolver={(rowData: IRowData) => {
          const user: UserRepresentation = rowData.data;
          if (!user.access?.manage) return [];

          return [
            {
              title: t("delete"),
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
            displayKey: "username",
            cellRenderer: UserDetailLink,
          },
          {
            name: "email",
            displayKey: "email",
            cellRenderer: ValidatedEmail,
          },
          {
            name: "lastName",
            displayKey: "lastName",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "firstName",
            displayKey: "firstName",
            cellFormatters: [emptyFormatter()],
          },
        ]}
      />
    </>
  );
}
