import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import {
  GroupQuery,
  SubGroupQuery,
} from "@keycloak/keycloak-admin-client/lib/resources/groups";
import { SearchInput, ToolbarItem } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useLocation } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { useAccess } from "../context/access/Access";
import useToggle from "../utils/useToggle";
import { GroupsModal } from "./GroupsModal";
import { useSubGroups } from "./SubGroupsContext";
import { DeleteGroup } from "./components/DeleteGroup";
import { GroupToolbar } from "./components/GroupToolbar";
import { MoveDialog } from "./components/MoveDialog";
import { getLastId } from "./groupIdUtils";

type GroupTableProps = {
  refresh: () => void;
};

export const GroupTable = ({ refresh: viewRefresh }: GroupTableProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const [selectedRows, setSelectedRows] = useState<GroupRepresentation[]>([]);
  const [rename, setRename] = useState<GroupRepresentation>();
  const [isCreateModalOpen, toggleCreateOpen] = useToggle();
  const [duplicateId, setDuplicateId] = useState<string>();
  const [showDelete, toggleShowDelete] = useToggle();
  const [move, setMove] = useState<GroupRepresentation>();
  const { currentGroup } = useSubGroups();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const [search, setSearch] = useState<string>();
  const location = useLocation();
  const id = getLastId(location.pathname);
  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-users") || currentGroup()?.access?.manage;

  const loader = async (first?: number, max?: number) => {
    let groupsData = undefined;
    if (id) {
      const args: SubGroupQuery = {
        search: search || "",
        first: first,
        max: max,
        parentId: id,
      };
      groupsData = await adminClient.groups.listSubGroups(args);
    } else {
      const args: GroupQuery = {
        search: search || "",
        first: first || undefined,
        max: max || undefined,
      };
      groupsData = await adminClient.groups.find(args);
    }

    return groupsData;
  };

  return (
    <>
      <DeleteGroup
        show={showDelete}
        toggleDialog={toggleShowDelete}
        selectedRows={selectedRows}
        refresh={() => {
          refresh();
          viewRefresh();
          setSelectedRows([]);
        }}
      />
      {rename && (
        <GroupsModal
          id={rename.id}
          rename={rename}
          refresh={() => {
            refresh();
            viewRefresh();
          }}
          handleModalToggle={() => setRename(undefined)}
        />
      )}
      {isCreateModalOpen && (
        <GroupsModal
          id={selectedRows[0]?.id || id}
          handleModalToggle={toggleCreateOpen}
          refresh={() => {
            setSelectedRows([]);
            refresh();
            viewRefresh();
          }}
        />
      )}
      {duplicateId && (
        <GroupsModal
          id={duplicateId}
          duplicateId={duplicateId}
          refresh={() => {
            refresh();
            viewRefresh();
          }}
          handleModalToggle={() => setDuplicateId(undefined)}
        />
      )}
      {move && (
        <MoveDialog
          source={move}
          refresh={() => {
            setMove(undefined);
            refresh();
            viewRefresh();
          }}
          onClose={() => setMove(undefined)}
        />
      )}
      <KeycloakDataTable
        key={`${id}${key}`}
        onSelect={(rows) => setSelectedRows([...rows])}
        canSelectAll
        loader={loader}
        ariaLabelKey="groups"
        isPaginated
        isSearching={!!search}
        toolbarItem={
          <>
            <ToolbarItem>
              <SearchInput
                data-testid="group-search"
                placeholder={t("filterGroups")}
                value={search}
                onChange={(_, value) => {
                  setSearch(value);
                  if (value === "") {
                    refresh();
                  }
                }}
                onSearch={refresh}
                onClear={() => {
                  setSearch("");
                  refresh();
                }}
              />
            </ToolbarItem>
            <GroupToolbar
              toggleCreate={toggleCreateOpen}
              toggleDelete={toggleShowDelete}
              kebabDisabled={selectedRows!.length === 0}
            />
          </>
        }
        actions={
          !isManager
            ? []
            : [
                {
                  title: t("edit"),
                  onRowClick: async (group) => {
                    setRename(group);
                    return false;
                  },
                },
                {
                  title: t("moveTo"),
                  onRowClick: async (group) => {
                    setMove(group);
                    return false;
                  },
                },
                {
                  title: t("createChildGroup"),
                  onRowClick: async (group) => {
                    setSelectedRows([group]);
                    toggleCreateOpen();
                    return false;
                  },
                },
                ...(!id
                  ? [
                      {
                        title: t("duplicate"),
                        onRowClick: async (group: GroupRepresentation) => {
                          setDuplicateId(group.id);
                          return false;
                        },
                      },
                    ]
                  : []),
                {
                  isSeparator: true,
                },
                {
                  title: t("delete"),
                  onRowClick: async (group: GroupRepresentation) => {
                    setSelectedRows([group]);
                    toggleShowDelete();
                    return true;
                  },
                },
              ]
        }
        columns={[
          {
            name: "name",
            displayKey: "groupName",
            cellRenderer: (group) =>
              group.access?.view ? (
                <Link key={group.id} to={`${location.pathname}/${group.id}`}>
                  {group.name}
                </Link>
              ) : (
                <span>{group.name}</span>
              ),
          },
        ]}
        emptyState={
          <ListEmptyState
            hasIcon={true}
            message={t(`noGroupsInThis${id ? "SubGroup" : "Realm"}`)}
            instructions={t(
              `noGroupsInThis${id ? "SubGroup" : "Realm"}Instructions`,
            )}
            primaryActionText={t("createGroup")}
            onPrimaryAction={toggleCreateOpen}
          />
        }
      />
    </>
  );
};
