import React, { useEffect, useState } from "react";
import { Link, useHistory, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import _ from "lodash";
import {
  AlertVariant,
  Button,
  Dropdown,
  DropdownItem,
  KebabToggle,
  ToolbarItem,
} from "@patternfly/react-core";
import { UsersIcon } from "@patternfly/react-icons";

import GroupRepresentation from "keycloak-admin/lib/defs/groupRepresentation";
import { useAdminClient } from "../context/auth/AdminClient";
import { useSubGroups } from "./SubGroupsContext";
import { useAlerts } from "../components/alert/Alerts";
import { useRealm } from "../context/realm-context/RealmContext";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { GroupsModal } from "./GroupsModal";
import { getLastId } from "./groupIdUtils";
import { MoveGroupDialog } from "./MoveGroupDialog";

type GroupTableData = GroupRepresentation & {
  membersLength?: number;
};

export const GroupTable = () => {
  const { t } = useTranslation("groups");

  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();
  const { realm } = useRealm();
  const [isKebabOpen, setIsKebabOpen] = useState(false);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [selectedRows, setSelectedRows] = useState<GroupRepresentation[]>([]);
  const [move, setMove] = useState<GroupTableData>();

  const { subGroups } = useSubGroups();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const history = useHistory();
  const location = useLocation();
  const id = getLastId(location.pathname);

  useEffect(() => {
    refresh();
  }, [subGroups]);

  const getMembers = async (id: string) => {
    const response = await adminClient.groups.listMembers({ id });
    return response ? response.length : 0;
  };

  const loader = async (first?: number, max?: number, search?: string) => {
    const params: { [name: string]: string | number } = {
      first: first!,
      max: max!,
    };

    const searchParam = search || "";

    if (searchParam) {
      params.search = searchParam;
    }

    const groupsData = id
      ? (await adminClient.groups.findOne({ id, ...params })).subGroups
      : await adminClient.groups.find(params);

    if (groupsData) {
      const memberPromises = groupsData.map((group) => getMembers(group.id!));
      const memberData = await Promise.all(memberPromises);
      return _.cloneDeep(groupsData).map((group: GroupTableData, i) => {
        group.membersLength = memberData[i];
        return group;
      });
    } else {
      history.push(`/${realm}/groups`);
    }

    return [];
  };

  const deleteGroup = async (group: GroupRepresentation) => {
    try {
      await adminClient.groups.del({
        id: group.id!,
      });
      addAlert(t("groupDelete"), AlertVariant.success);
    } catch (error) {
      addAlert(t("groupDeleteError", { error }), AlertVariant.danger);
    }
    return true;
  };

  const multiDelete = async () => {
    if (selectedRows!.length !== 0) {
      const chainedPromises = selectedRows!.map((group) => deleteGroup(group));

      await Promise.all(chainedPromises);
      addAlert(t("groupsDeleted"), AlertVariant.success);
      setSelectedRows([]);
      refresh();
    }
  };

  const GroupNameCell = (group: GroupTableData) => (
    <>
      <Link key={group.id} to={`${location.pathname}/${group.id}`}>
        {group.name}
      </Link>
    </>
  );

  const GroupMemberCell = (group: GroupTableData) => (
    <div className="keycloak-admin--groups__member-count">
      <UsersIcon key={`user-icon-${group.id}`} />
      {group.membersLength}
    </div>
  );

  const handleModalToggle = () => {
    setIsCreateModalOpen(!isCreateModalOpen);
  };

  return (
    <>
      <KeycloakDataTable
        key={key}
        isPaginated
        onSelect={(rows) => setSelectedRows([...rows])}
        canSelectAll={false}
        loader={loader}
        ariaLabelKey="groups:groups"
        searchPlaceholderKey="groups:searchForGroups"
        toolbarItem={
          <>
            <ToolbarItem>
              <Button
                data-testid="openCreateGroupModal"
                variant="primary"
                onClick={handleModalToggle}
              >
                {t("createGroup")}
              </Button>
            </ToolbarItem>
            <ToolbarItem>
              <Dropdown
                toggle={
                  <KebabToggle onToggle={() => setIsKebabOpen(!isKebabOpen)} />
                }
                isOpen={isKebabOpen}
                isPlain
                dropdownItems={[
                  <DropdownItem
                    key="action"
                    component="button"
                    onClick={() => {
                      multiDelete();
                      setIsKebabOpen(false);
                    }}
                  >
                    {t("common:delete")}
                  </DropdownItem>,
                ]}
              />
            </ToolbarItem>
          </>
        }
        actions={[
          {
            title: t("moveTo"),
            onRowClick: async (group) => {
              setMove(group);
              return false;
            },
          },
          {
            title: t("common:delete"),
            onRowClick: async (group: GroupRepresentation) => {
              await deleteGroup(group);
              refresh();
              return true;
            },
          },
        ]}
        columns={[
          {
            name: "name",
            displayKey: "groups:groupName",
            cellRenderer: GroupNameCell,
          },
          {
            name: "members",
            displayKey: "groups:members",
            cellRenderer: GroupMemberCell,
          },
        ]}
        emptyState={
          <ListEmptyState
            hasIcon={true}
            message={t(`noGroupsInThis${id ? "SubGroup" : "Realm"}`)}
            instructions={t(
              `noGroupsInThis${id ? "SubGroup" : "Realm"}Instructions`
            )}
            primaryActionText={t("createGroup")}
            onPrimaryAction={handleModalToggle}
          />
        }
      />
      {isCreateModalOpen && (
        <GroupsModal
          id={id}
          handleModalToggle={handleModalToggle}
          refresh={refresh}
        />
      )}
      {move && (
        <MoveGroupDialog
          group={move}
          onClose={() => setMove(undefined)}
          onMove={async (id) => {
            delete move.membersLength;
            try {
              try {
                if (id) {
                  await adminClient.groups.setOrCreateChild({ id }, move);
                } else {
                  await adminClient.groups.create(move);
                }
              } catch (error) {
                if (error.response) {
                  throw error;
                }
              }
              setMove(undefined);
              refresh();
              addAlert(t("moveGroupSuccess"), AlertVariant.success);
            } catch (error) {
              addAlert(
                t("moveGroupError", {
                  error: error.response?.data?.errorMessage || error,
                }),
                AlertVariant.danger
              );
            }
          }}
        />
      )}
    </>
  );
};
