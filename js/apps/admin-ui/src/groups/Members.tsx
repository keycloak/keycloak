import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { SubGroupQuery } from "@keycloak/keycloak-admin-client/lib/resources/groups";
import {
  Action,
  KeycloakDataTable,
  ListEmptyState,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  Checkbox,
  Dropdown,
  DropdownItem,
  DropdownList,
  Label,
  MenuToggle,
  ToolbarItem,
} from "@patternfly/react-core";
import { EllipsisVIcon, InfoCircleIcon } from "@patternfly/react-icons";
import { uniqBy } from "lodash-es";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useLocation } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import { toUser } from "../user/routes/User";
import { emptyFormatter } from "../util";
import { MemberModal } from "./MembersModal";
import { useSubGroups } from "./SubGroupsContext";
import { getLastId } from "./groupIdUtils";
import { MembershipsModal } from "./MembershipsModal";
import useToggle from "../utils/useToggle";

const UserDetailLink = (user: UserRepresentation) => {
  const { realm } = useRealm();
  const { t } = useTranslation();
  return (
    <Link key={user.id} to={toUser({ realm, id: user.id!, tab: "settings" })}>
      {user.username}{" "}
      {!user.enabled && (
        <Label color="red" icon={<InfoCircleIcon />}>
          {t("disabled")}
        </Label>
      )}
    </Link>
  );
};

export const Members = () => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const location = useLocation();
  const id = getLastId(location.pathname);
  const [includeSubGroup, setIncludeSubGroup] = useState(false);
  const { currentGroup: group } = useSubGroups();
  const [currentGroup, setCurrentGroup] = useState<GroupRepresentation>();
  const [addMembers, setAddMembers] = useState(false);
  const [isKebabOpen, setIsKebabOpen] = useState(false);
  const [selectedRows, setSelectedRows] = useState<UserRepresentation[]>([]);
  const [selectedUser, setSelectedUser] = useState<UserRepresentation>();
  const [showMemberships, toggleShowMemberships] = useToggle();
  const { hasAccess } = useAccess();

  useFetch(
    () => adminClient.groups.findOne({ id: group()!.id! }),
    setCurrentGroup,
    [],
  );

  const isManager =
    hasAccess("manage-users") || currentGroup?.access!.manageMembership;

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  // this queries the subgroups using the new search paradigm but doesn't
  // account for pagination and therefore isn't going to scale well
  const getSubGroups = async (groupId?: string, count = 0) => {
    let nestedGroups: GroupRepresentation[] = [];
    if (!count || !groupId) {
      return nestedGroups;
    }
    const args: SubGroupQuery = {
      parentId: groupId,
      first: 0,
      max: count,
    };
    const subGroups: GroupRepresentation[] =
      await adminClient.groups.listSubGroups(args);
    nestedGroups = nestedGroups.concat(subGroups);

    await Promise.all(
      subGroups.map((g) => getSubGroups(g.id, g.subGroupCount)),
    ).then((values: GroupRepresentation[][]) => {
      values.forEach((groups) => (nestedGroups = nestedGroups.concat(groups)));
    });
    return nestedGroups;
  };

  const loader = async (first?: number, max?: number) => {
    if (!id) {
      return [];
    }

    let members = await adminClient.groups.listMembers({
      id: id!,
      briefRepresentation: true,
      first,
      max,
    });

    if (includeSubGroup && currentGroup?.subGroupCount && currentGroup.id) {
      const subGroups = await getSubGroups(
        currentGroup.id,
        currentGroup.subGroupCount,
      );
      await Promise.all(
        subGroups.map((g) =>
          adminClient.groups.listMembers({
            id: g.id!,
            briefRepresentation: true,
          }),
        ),
      ).then((values: UserRepresentation[][]) => {
        values.forEach((users) => (members = members.concat(users)));
      });
      members = uniqBy(members, (member) => member.username);
    }

    return members;
  };

  if (!currentGroup) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      {addMembers && (
        <MemberModal
          membersQuery={(first, max) =>
            adminClient.groups.listMembers({ id: id!, first, max })
          }
          onAdd={async (selectedRows) => {
            try {
              await Promise.all(
                selectedRows.map((user) =>
                  adminClient.users.addToGroup({ id: user.id!, groupId: id! }),
                ),
              );
              addAlert(t("usersAdded", { count: selectedRows.length }));
            } catch (error) {
              addError("usersAddedError", error);
            }
          }}
          onClose={() => {
            setAddMembers(false);
            refresh();
          }}
        />
      )}
      {showMemberships && (
        <MembershipsModal
          onClose={() => {
            toggleShowMemberships();
          }}
          user={selectedUser!}
        />
      )}
      <KeycloakDataTable
        data-testid="members-table"
        key={`${id}${key}${includeSubGroup}`}
        loader={loader}
        ariaLabelKey="members"
        isPaginated
        canSelectAll
        onSelect={(rows) => setSelectedRows([...rows])}
        toolbarItem={
          isManager && (
            <>
              <ToolbarItem>
                <Button
                  data-testid="addMember"
                  variant="primary"
                  onClick={() => setAddMembers(true)}
                >
                  {t("addMember")}
                </Button>
              </ToolbarItem>
              <ToolbarItem>
                <Checkbox
                  data-testid="includeSubGroupsCheck"
                  label={t("includeSubGroups")}
                  id="kc-include-sub-groups"
                  isChecked={includeSubGroup}
                  onChange={() => setIncludeSubGroup(!includeSubGroup)}
                />
              </ToolbarItem>
              <ToolbarItem>
                <Dropdown
                  onOpenChange={(isOpen) => setIsKebabOpen(isOpen)}
                  toggle={(ref) => (
                    <MenuToggle
                      data-testid="kebab"
                      ref={ref}
                      variant="plain"
                      onClick={() => setIsKebabOpen(!isKebabOpen)}
                      isExpanded={isKebabOpen}
                      isDisabled={selectedRows.length === 0}
                      aria-label="Actions"
                    >
                      <EllipsisVIcon />
                    </MenuToggle>
                  )}
                  shouldFocusToggleOnSelect
                  isOpen={isKebabOpen}
                >
                  <DropdownList>
                    <DropdownItem
                      key="action"
                      component="button"
                      onClick={async () => {
                        try {
                          await Promise.all(
                            selectedRows.map((user) =>
                              adminClient.users.delFromGroup({
                                id: user.id!,
                                groupId: id!,
                              }),
                            ),
                          );
                          setIsKebabOpen(false);
                          addAlert(
                            t("usersLeft", { count: selectedRows.length }),
                          );
                        } catch (error) {
                          addError("usersLeftError", error);
                        }

                        refresh();
                      }}
                    >
                      {t("leave")}
                    </DropdownItem>
                  </DropdownList>
                </Dropdown>
              </ToolbarItem>
            </>
          )
        }
        actions={[
          ...(isManager
            ? [
                {
                  title: t("leave"),
                  onRowClick: async (user) => {
                    try {
                      await adminClient.users.delFromGroup({
                        id: user.id!,
                        groupId: id!,
                      });
                      addAlert(t("usersLeft", { count: 1 }));
                    } catch (error) {
                      addError("usersLeftError", error);
                    }
                    return true;
                  },
                } as Action<UserRepresentation>,
              ]
            : []),
          {
            title: t("showMemberships"),
            onRowClick: (user) => {
              setSelectedUser(user);
              toggleShowMemberships();
            },
          } as Action<UserRepresentation>,
        ]}
        columns={[
          {
            name: "username",
            displayKey: "name",
            cellRenderer: UserDetailLink,
          },
          {
            name: "email",
            displayKey: "email",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "firstName",
            displayKey: "firstName",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "lastName",
            displayKey: "lastName",
            cellFormatters: [emptyFormatter()],
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("noUsersFound")}
            instructions={isManager ? t("emptyInstructions") : undefined}
            primaryActionText={isManager ? t("addMember") : undefined}
            onPrimaryAction={() => setAddMembers(true)}
            secondaryActions={[
              {
                text: t("includeSubGroups"),
                onClick: () => setIncludeSubGroup(true),
              },
            ]}
          />
        }
      />
    </>
  );
};
