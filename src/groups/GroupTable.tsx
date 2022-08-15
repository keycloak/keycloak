import { useState } from "react";
import { Link, useHistory, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { cellWidth } from "@patternfly/react-table";

import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { GroupsModal } from "./GroupsModal";
import { getLastId } from "./groupIdUtils";
import { useSubGroups } from "./SubGroupsContext";
import { toGroups } from "./routes/Groups";
import { useAccess } from "../context/access/Access";
import useToggle from "../utils/useToggle";
import { DeleteGroup } from "./components/DeleteGroup";
import { GroupToolbar, ViewType } from "./components/GroupToolbar";
import { MoveDialog } from "./components/MoveDialog";

type GroupTableProps = {
  toggleView?: (viewType: ViewType) => void;
};

export const GroupTable = ({ toggleView }: GroupTableProps) => {
  const { t } = useTranslation("groups");

  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [selectedRows, setSelectedRows] = useState<GroupRepresentation[]>([]);
  const [showDelete, toggleShowDelete] = useToggle();
  const [move, setMove] = useState<GroupRepresentation>();

  const { subGroups, currentGroup, setSubGroups } = useSubGroups();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const history = useHistory();
  const location = useLocation();
  const id = getLastId(location.pathname);

  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-users") || currentGroup()?.access?.manage;
  const canView =
    hasAccess("query-groups", "view-users") ||
    hasAccess("manage-users", "query-groups");

  const loader = async () => {
    let groupsData = undefined;
    if (id) {
      const group = await adminClient.groups.findOne({ id });
      if (!group) {
        throw new Error(t("common:notFound"));
      }

      groupsData = group.subGroups;
    } else {
      groupsData = await adminClient.groups.find({
        briefRepresentation: false,
      });
    }

    if (!groupsData) {
      history.push(toGroups({ realm }));
    }

    return groupsData || [];
  };

  const GroupNameCell = (group: GroupRepresentation) => {
    if (!canView) return <span>{group.name}</span>;

    return (
      <Link
        key={group.id}
        to={`${location.pathname}/${group.id}`}
        onClick={async () => {
          const loadedGroup = await adminClient.groups.findOne({
            id: group.id!,
          });
          setSubGroups([...subGroups, loadedGroup!]);
        }}
      >
        {group.name}
      </Link>
    );
  };

  const handleModalToggle = () => {
    setIsCreateModalOpen(!isCreateModalOpen);
  };

  return (
    <>
      <DeleteGroup
        show={showDelete}
        toggleDialog={toggleShowDelete}
        selectedRows={selectedRows}
        refresh={() => {
          refresh();
          setSelectedRows([]);
        }}
      />
      <KeycloakDataTable
        key={`${id}${key}`}
        onSelect={(rows) => setSelectedRows([...rows])}
        canSelectAll
        loader={loader}
        ariaLabelKey="groups:groups"
        searchPlaceholderKey="groups:searchForGroups"
        toolbarItem={
          <GroupToolbar
            currentView={ViewType.Table}
            toggleView={toggleView}
            toggleCreate={handleModalToggle}
            toggleDelete={toggleShowDelete}
            kebabDisabled={selectedRows!.length === 0}
          />
        }
        actions={
          !isManager
            ? []
            : [
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
                    toggleShowDelete();
                    return true;
                  },
                },
              ]
        }
        columns={[
          {
            name: "name",
            displayKey: "groups:groupName",
            cellRenderer: GroupNameCell,
            transforms: [cellWidth(90)],
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
        <MoveDialog
          source={move}
          refresh={() => {
            setMove(undefined);
            refresh();
          }}
          onClose={() => setMove(undefined)}
        />
      )}
    </>
  );
};
