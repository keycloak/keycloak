import React, { useState } from "react";
import { Link, useHistory, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import _ from "lodash";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Dropdown,
  DropdownItem,
  KebabToggle,
  ToolbarItem,
} from "@patternfly/react-core";
import { cellWidth } from "@patternfly/react-table";
import { UsersIcon } from "@patternfly/react-icons";

import type GroupRepresentation from "keycloak-admin/lib/defs/groupRepresentation";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { useRealm } from "../context/realm-context/RealmContext";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { GroupsModal } from "./GroupsModal";
import { getLastId } from "./groupIdUtils";
import { MoveGroupDialog } from "./MoveGroupDialog";
import { useSubGroups } from "./SubGroupsContext";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";

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

  const { subGroups, setSubGroups } = useSubGroups();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const history = useHistory();
  const location = useLocation();
  const id = getLastId(location.pathname);

  const getMembers = async (id: string) => {
    const response = await adminClient.groups.listMembers({ id });
    return response ? response.length : 0;
  };

  const loader = async () => {
    const groupsData = id
      ? (await adminClient.groups.findOne({ id })).subGroups
      : await adminClient.groups.find();

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

  const multiDelete = async () => {
    try {
      for (const group of selectedRows) {
        await adminClient.groups.del({
          id: group.id!,
        });
      }
      addAlert(
        t("groupDeleted", { count: selectedRows.length }),
        AlertVariant.success
      );
      setSelectedRows([]);
    } catch (error) {
      addAlert(t("groupDeleteError", { error }), AlertVariant.danger);
    }
    refresh();
  };

  const GroupNameCell = (group: GroupTableData) => (
    <>
      <Link
        key={group.id}
        to={`${location.pathname}/${group.id}`}
        onClick={() => {
          delete group.membersLength;
          setSubGroups([...subGroups, group]);
        }}
      >
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

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteConfirmTitle", { count: selectedRows.length }),
    messageKey: t("deleteConfirm", { count: selectedRows.length }),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: multiDelete,
  });

  return (
    <>
      <DeleteConfirm />
      <KeycloakDataTable
        key={`${id}${key}`}
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
                  <KebabToggle
                    onToggle={() => setIsKebabOpen(!isKebabOpen)}
                    isDisabled={selectedRows!.length === 0}
                  />
                }
                isOpen={isKebabOpen}
                isPlain
                dropdownItems={[
                  <DropdownItem
                    key="action"
                    component="button"
                    onClick={() => {
                      toggleDeleteDialog();
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
              setSelectedRows([group]);
              toggleDeleteDialog();
              return true;
            },
          },
        ]}
        columns={[
          {
            name: "name",
            displayKey: "groups:groupName",
            cellRenderer: GroupNameCell,
            transforms: [cellWidth(15)],
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
