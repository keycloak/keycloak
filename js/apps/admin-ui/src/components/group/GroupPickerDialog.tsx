import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import type OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import {
  GroupQuery,
  SubGroupQuery,
} from "@keycloak/keycloak-admin-client/lib/resources/groups";
import { OrganizationQuery } from "@keycloak/keycloak-admin-client/lib/resources/organizations";
import {
  ListEmptyState,
  PaginatingTableToolbar,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  Breadcrumb,
  BreadcrumbItem,
  Button,
  DataList,
  DataListAction,
  DataListCell,
  DataListCheck,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { AngleRightIcon } from "@patternfly/react-icons";
import { NetworkError } from "@keycloak/keycloak-admin-client/lib/utils/fetchWithError";
import { useContext, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { GroupPath } from "./GroupPath";
import { GroupsResourceContext } from "../../context/group-resource/GroupResourceContext";

import "./group-picker-dialog.css";

export type GroupPickerDialogProps = {
  id?: string;
  type: "selectOne" | "selectMany";
  filterGroups?: GroupRepresentation[];
  text: { title: string; ok: string };
  canBrowse?: boolean;
  isMove?: boolean;
  /**
   * When set, the dialog opens on the organizations linked to this identity provider and the groups are the ones of
   * the organization picked there. Without it the dialog browses the groups of the resource from the context.
   */
  identityProviderAlias?: string;
  onConfirm: (
    groups: GroupRepresentation[] | undefined,
    organization?: OrganizationRepresentation,
  ) => void;
  onClose: () => void;
};

type SelectableGroup = GroupRepresentation & {
  checked?: boolean;
};

export const GroupPickerDialog = ({
  id,
  type,
  filterGroups,
  text,
  canBrowse = true,
  isMove = false,
  identityProviderAlias,
  onClose,
  onConfirm,
}: GroupPickerDialogProps) => {
  const { adminClient } = useAdminClient();
  const contextGroupResource = useContext(GroupsResourceContext);

  const { t } = useTranslation();
  const [selectedRows, setSelectedRows] = useState<SelectableGroup[]>([]);

  const [navigation, setNavigation] = useState<SelectableGroup[]>([]);
  const [groups, setGroups] = useState<SelectableGroup[]>([]);
  const [filter, setFilter] = useState("");
  const [joinedGroups, setJoinedGroups] = useState<GroupRepresentation[]>([]);
  const [groupId, setGroupId] = useState<string>();

  const [organizations, setOrganizations] = useState<
    OrganizationRepresentation[]
  >([]);
  const [organization, setOrganization] =
    useState<OrganizationRepresentation>();

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);

  const [count, setCount] = useState(0);

  // the organizations are the root level of the dialog, the groups of the picked one the levels below it
  const isPickingOrganization = !!identityProviderAlias && !organization;

  const groupResource = useMemo(
    () =>
      organization
        ? adminClient.organizations.groups(organization.id!)
        : contextGroupResource,
    [adminClient, organization, contextGroupResource],
  );
  const isOrgGroups = groupResource?.isOrgGroups() ?? false;

  const currentGroup = () => navigation[navigation.length - 1];

  const resetPagination = () => {
    setFirst(0);
    setMax(10);
  };

  const resetToGroupRoot = () => {
    setGroupId(undefined);
    setNavigation([]);
    resetPagination();
  };

  const selectOrganization = (organization?: OrganizationRepresentation) => {
    setOrganization(organization);
    setFilter("");
    setGroups([]);
    setCount(0);
    setSelectedRows([]);
    setJoinedGroups([]);
    resetToGroupRoot();
  };

  useFetch(
    async () => {
      if (!isPickingOrganization) {
        return undefined;
      }

      const params: OrganizationQuery = {
        identityProvider: identityProviderAlias,
        first,
        max: max + 1,
      };
      if (filter !== "") {
        params.search = filter;
      }

      return await adminClient.organizations.find(params);
    },
    (organizations) => {
      if (!organizations) {
        return;
      }

      setOrganizations(organizations);
      setCount(organizations.length);
    },
    [identityProviderAlias, organization?.id, filter, first, max],
  );

  useFetch(
    async () => {
      if (isPickingOrganization || !groupResource) {
        return undefined;
      }

      let group;
      let groups;
      let existingUserGroups;

      if (!groupId) {
        const args: GroupQuery = {
          first,
          max: max + 1,
        };
        if (filter !== "") {
          args.search = filter;
        }
        groups = await groupResource.find(args);
      } else {
        if (!navigation.map(({ id }) => id).includes(groupId)) {
          try {
            group = await groupResource.findOne({ id: groupId });
          } catch (error) {
            if (
              error instanceof NetworkError &&
              error.response.status === 403
            ) {
              group = undefined;
            } else {
              throw error;
            }
          }
          if (!group) {
            throw new Error(t("notFound"));
          }
        }

        const args: SubGroupQuery = {
          first,
          max,
          parentId: groupId,
        };
        groups = await groupResource.listSubGroups(args);
      }

      if (id) {
        existingUserGroups = await adminClient.users.listGroups({
          id,
        });
      }

      return { group, groups, existingUserGroups };
    },
    async (result) => {
      if (!result) {
        return;
      }

      const { group: selectedGroup, groups, existingUserGroups } = result;

      setJoinedGroups(existingUserGroups || []);
      if (selectedGroup) {
        setNavigation([...navigation, selectedGroup]);
        setCount(selectedGroup.subGroupCount!);
      }

      groups.forEach((group: SelectableGroup) => {
        group.checked = !!selectedRows.find((r) => r.id === group.id);
      });
      setGroups(groups);
      if (filter !== "" || !groupId) {
        setCount(groups.length);
      }
    },
    [organization?.id, groupId, filter, first, max],
  );

  const isRowDisabled = (row?: GroupRepresentation) => {
    return [
      ...joinedGroups.map((item) => item.id),
      ...(filterGroups || []).map((group) => group.id),
    ].some((group) => group === row?.id);
  };

  return (
    <Modal
      variant={filter !== "" ? ModalVariant.medium : ModalVariant.small}
      title={t(text.title, {
        group1: filterGroups?.[0]?.name,
        group2: navigation.length ? currentGroup().name : t("root"),
      })}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          data-testid={`${text.ok}-button`}
          key="confirm"
          variant="primary"
          form="group-form"
          onClick={() => {
            onConfirm(
              type === "selectMany"
                ? selectedRows
                : navigation.length
                  ? [currentGroup()]
                  : undefined,
              organization,
            );
          }}
          isDisabled={
            // in the organization flow there is nothing to confirm until a group below the organization is picked
            (!!identityProviderAlias && navigation.length === 0) ||
            (type === "selectMany" && selectedRows.length === 0)
          }
        >
          {t(text.ok)}
        </Button>,
      ]}
    >
      <PaginatingTableToolbar
        count={count}
        first={first}
        max={max}
        onNextClick={setFirst}
        onPreviousClick={setFirst}
        onPerPageSelect={(first, max) => {
          setFirst(first);
          setMax(max);
        }}
        inputGroupName={"search"}
        inputGroupOnEnter={(search) => {
          setFilter(search);
          resetToGroupRoot();
        }}
        inputGroupPlaceholder={
          isPickingOrganization
            ? t("searchForOrganizations")
            : t("searchForGroups")
        }
      >
        <Breadcrumb>
          {organization && (
            <>
              <BreadcrumbItem key="organizations">
                <Button variant="link" onClick={() => selectOrganization()}>
                  {t("organizations")}
                </Button>
              </BreadcrumbItem>
              <BreadcrumbItem key="organization">
                {navigation.length > 0 ? (
                  <Button variant="link" onClick={resetToGroupRoot}>
                    {organization.name}
                  </Button>
                ) : (
                  organization.name
                )}
              </BreadcrumbItem>
            </>
          )}
          {!identityProviderAlias && navigation.length > 0 && (
            <BreadcrumbItem key="home">
              <Button variant="link" onClick={resetToGroupRoot}>
                {t("groups")}
              </Button>
            </BreadcrumbItem>
          )}
          {navigation.map((group, i) => (
            <BreadcrumbItem key={i}>
              {navigation.length - 1 !== i && (
                <Button
                  variant="link"
                  onClick={() => {
                    setGroupId(group.id);
                    setNavigation([...navigation].slice(0, i));
                    setFirst(0);
                    setMax(10);
                  }}
                >
                  {group.name}
                </Button>
              )}
              {navigation.length - 1 === i && group.name}
            </BreadcrumbItem>
          ))}
        </Breadcrumb>
        {isPickingOrganization ? (
          <>
            <DataList aria-label={t("organizations")} isCompact>
              {organizations.slice(0, max).map((organization) => (
                <OrganizationRow
                  key={organization.id}
                  organization={organization}
                  onSelect={() => selectOrganization(organization)}
                />
              ))}
            </DataList>
            {organizations.length === 0 && (
              <ListEmptyState
                hasIcon={false}
                message={
                  filter === ""
                    ? t("noLinkedOrganizations")
                    : t("noSearchResults")
                }
                instructions={
                  filter === "" ? undefined : t("noSearchResultsInstructions")
                }
              />
            )}
          </>
        ) : (
          <>
            <DataList aria-label={t("groups")} isCompact>
              {filter == ""
                ? groups.slice(0, max).map((group: SelectableGroup) => (
                    <GroupRow
                      key={group.id}
                      group={group}
                      isRowDisabled={isRowDisabled}
                      onSelect={(group) => {
                        setGroupId(group.id);
                        setFirst(0);
                      }}
                      type={type}
                      isSearching={false}
                      selectedRows={selectedRows}
                      setSelectedRows={setSelectedRows}
                      canBrowse={canBrowse}
                    />
                  ))
                : groups
                    .map((g) => deepGroup([g]))
                    .flat()
                    .filter((g) => isOrgGroups || g.access)
                    .map((g) => (
                      <GroupRow
                        key={g.id}
                        group={g}
                        isRowDisabled={isRowDisabled}
                        onSelect={(group) => {
                          setGroupId(group.id);
                          setFilter("");
                          setFirst(0);
                        }}
                        type={type}
                        isSearching
                        selectedRows={selectedRows}
                        setSelectedRows={setSelectedRows}
                        canBrowse={false}
                      />
                    ))}
            </DataList>
            {groups.length === 0 && filter === "" && (
              <ListEmptyState
                hasIcon={false}
                message={t("moveGroupEmpty")}
                instructions={
                  isMove ? t("moveGroupEmptyInstructions") : undefined
                }
              />
            )}
            {groups.length === 0 && filter !== "" && (
              <ListEmptyState
                message={t("noSearchResults")}
                instructions={t("noSearchResultsInstructions")}
              />
            )}
          </>
        )}
      </PaginatingTableToolbar>
    </Modal>
  );
};

type OrganizationRowProps = {
  organization: OrganizationRepresentation;
  onSelect: () => void;
};

const OrganizationRow = ({ organization, onSelect }: OrganizationRowProps) => {
  const { t } = useTranslation();
  const labelId = `select-${organization.id}`;

  return (
    <DataListItem
      aria-labelledby={labelId}
      key={organization.id}
      id={organization.id}
      onClick={onSelect}
    >
      <DataListItemRow
        className="join-group-dialog-row"
        data-testid={organization.name}
      >
        <DataListItemCells
          dataListCells={[
            <DataListCell
              key={`name-${organization.id}`}
              className="keycloak-groups-group-path"
            >
              <span id={labelId}>{organization.name}</span>
            </DataListCell>,
          ]}
        />
        <DataListAction
          id="actions"
          aria-labelledby={labelId}
          aria-label={t("organization")}
          isPlainButtonAction
        >
          <Button variant="link" aria-label={t("select")}>
            <AngleRightIcon />
          </Button>
        </DataListAction>
      </DataListItemRow>
    </DataListItem>
  );
};

function deepGroup(groups: GroupRepresentation[]) {
  const flattened: GroupRepresentation[] = [];
  for (const group of groups) {
    flattened.push(group);
    if (group.subGroups && group.subGroups.length > 0) {
      flattened.push(...deepGroup(group.subGroups));
    }
  }
  return flattened;
}

type GroupRowProps = {
  group: SelectableGroup;
  type: "selectOne" | "selectMany";
  isRowDisabled: (row?: GroupRepresentation) => boolean;
  isSearching: boolean;
  setIsSearching?: (value: boolean) => void;
  onSelect?: (group: GroupRepresentation) => void;
  selectedRows: SelectableGroup[];
  setSelectedRows: (groups: SelectableGroup[]) => void;
  canBrowse: boolean;
};

const GroupRow = ({
  group,
  type,
  isRowDisabled,
  isSearching,
  setIsSearching,
  onSelect,
  selectedRows,
  setSelectedRows,
  canBrowse,
}: GroupRowProps) => {
  const { t } = useTranslation();

  return (
    <DataListItem
      aria-labelledby={group.name}
      key={group.id}
      id={group.id}
      onClick={(e) => {
        if (type === "selectOne") {
          onSelect?.(group);
        } else if (
          (e.target as HTMLInputElement).type !== "checkbox" &&
          group.subGroupCount !== 0
        ) {
          onSelect?.(group);
          setIsSearching?.(false);
        }
      }}
    >
      <DataListItemRow
        className={`join-group-dialog-row${
          isRowDisabled(group) ? "-m-disabled" : ""
        }`}
        data-testid={group.name}
      >
        {type === "selectMany" && (
          <DataListCheck
            className="kc-join-group-modal-check"
            data-testid={`${group.name}-check`}
            aria-label={group.name}
            checked={group.checked}
            isDisabled={isRowDisabled(group)}
            onChange={(_event, checked) => {
              group.checked = checked;
              let newSelectedRows: SelectableGroup[] = [];
              if (!group.checked) {
                newSelectedRows = selectedRows.filter((r) => r.id !== group.id);
              } else {
                newSelectedRows = [...selectedRows, group];
              }

              setSelectedRows(newSelectedRows);
            }}
            aria-labelledby={`select-${group.name}`}
          />
        )}

        <DataListItemCells
          dataListCells={[
            <DataListCell
              key={`name-${group.id}`}
              className="keycloak-groups-group-path"
            >
              {isSearching ? (
                <GroupPath id={`select-${group.name}`} group={group} />
              ) : (
                <span id={`select-${group.name}`}>{group.name}</span>
              )}
            </DataListCell>,
          ]}
        />
        <DataListAction
          id="actions"
          aria-labelledby={`select-${group.name}`}
          aria-label={t("groupName")}
          isPlainButtonAction
        >
          {(canBrowse || type === "selectOne") && group.subGroupCount !== 0 && (
            <Button variant="link" aria-label={t("select")}>
              <AngleRightIcon />
            </Button>
          )}
        </DataListAction>
      </DataListItemRow>
    </DataListItem>
  );
};
