import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { SearchInput, ToolbarItem } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useLocation } from "react-router-dom";

import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useAccess } from "../context/access/Access";
import { fetchAdminUI } from "../context/auth/admin-ui-endpoint";
import useToggle from "../utils/useToggle";
import { GroupsModal } from "./GroupsModal";
import { useSubGroups } from "./SubGroupsContext";
import { DeleteGroup } from "./components/DeleteGroup";
import { GroupToolbar } from "./components/GroupToolbar";
import { MoveDialog } from "./components/MoveDialog";
import { getLastId } from "./groupIdUtils";

type GroupTableProps = {
  refresh: () => void;
  canViewDetails: boolean;
};

export const GroupTable = ({
  refresh: viewRefresh,
  canViewDetails,
}: GroupTableProps) => {
  const { t } = useTranslation();

  const [selectedRows, setSelectedRows] = useState<GroupRepresentation[]>([]);

  const [rename, setRename] = useState<GroupRepresentation>();
  const [isCreateModalOpen, toggleCreateOpen] = useToggle();
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
    const params: Record<string, string> = {
      search: search || "",
      first: first?.toString() || "",
      max: max?.toString() || "",
    };

    let groupsData = undefined;
    if (id) {
      groupsData = await fetchAdminUI<GroupRepresentation[]>(
        "ui-ext/groups/subgroup",
        { ...params, id },
      );
    } else {
      groupsData = await fetchAdminUI<GroupRepresentation[]>("ui-ext/groups", {
        ...params,
        global: "false",
      });
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
                  title: t("rename"),
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
              canViewDetails ? (
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
