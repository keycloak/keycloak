import { useState } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Dropdown,
  DropdownItem,
  DropdownPosition,
  KebabToggle,
  TreeViewDataItem,
} from "@patternfly/react-core";

import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { TableToolbar } from "../../components/table-toolbar/TableToolbar";
import useToggle from "../../utils/useToggle";
import { CheckableTreeView } from "./CheckableTreeView";
import { DeleteGroup } from "./DeleteGroup";
import { GroupToolbar, ViewType } from "./GroupToolbar";
import { GroupsModal } from "../GroupsModal";
import { MoveDialog } from "./MoveDialog";

type GroupTreeContextMenuProps = {
  group: GroupRepresentation;
  refresh: () => void;
};

const GroupTreeContextMenu = ({
  group,
  refresh,
}: GroupTreeContextMenuProps) => {
  const { t } = useTranslation("groups");

  const location = useLocation();
  const history = useHistory();

  const [isOpen, toggleOpen] = useToggle();
  const [createOpen, toggleCreateOpen] = useToggle();
  const [moveOpen, toggleMoveOpen] = useToggle();
  const [deleteOpen, toggleDeleteOpen] = useToggle();

  return (
    <>
      {createOpen && (
        <GroupsModal
          id={group.id}
          handleModalToggle={toggleCreateOpen}
          refresh={refresh}
        />
      )}
      {moveOpen && (
        <MoveDialog source={group} refresh={refresh} onClose={toggleMoveOpen} />
      )}
      <DeleteGroup
        show={deleteOpen}
        toggleDialog={toggleDeleteOpen}
        selectedRows={[group]}
        refresh={refresh}
      />
      <Dropdown
        toggle={<KebabToggle onToggle={toggleOpen} />}
        isOpen={isOpen}
        isPlain
        position={DropdownPosition.right}
        dropdownItems={[
          <DropdownItem key="create" onClick={toggleCreateOpen}>
            {t("createGroup")}
          </DropdownItem>,
          <DropdownItem key="move" onClick={toggleMoveOpen}>
            {t("moveTo")}
          </DropdownItem>,
          <DropdownItem
            key="edit"
            onClick={() => history.push(`${location.pathname}/${group.id}`)}
          >
            {t("common:edit")}
          </DropdownItem>,
          <DropdownItem key="delete" onClick={toggleDeleteOpen}>
            {t("common:delete")}
          </DropdownItem>,
        ]}
      />
    </>
  );
};

const mapGroup = (
  group: GroupRepresentation,
  refresh: () => void
): TreeViewDataItem => ({
  id: group.id,
  name: group.name,
  checkProps: { checked: false },
  children:
    group.subGroups && group.subGroups.length > 0
      ? group.subGroups.map((g) => mapGroup(g, refresh))
      : undefined,
  action: <GroupTreeContextMenu group={group} refresh={refresh} />,
});

const filterGroup = (
  group: TreeViewDataItem,
  search: string
): TreeViewDataItem | null => {
  const name = group.name as string;
  if (name.toLowerCase().includes(search)) {
    return { ...group, defaultExpanded: true, children: undefined };
  }

  const children: TreeViewDataItem[] = [];
  if (group.children) {
    for (const g of group.children) {
      const found = filterGroup(g, search);
      if (found) children.push(found);
    }
    if (children.length > 0) {
      return { ...group, defaultExpanded: true, children };
    }
  }
  return null;
};

const filterGroups = (
  groups: TreeViewDataItem[],
  search: string
): TreeViewDataItem[] => {
  const result: TreeViewDataItem[] = [];
  groups
    .map((g) => filterGroup(g, search))
    .forEach((g) => {
      if (g !== null) result.push(g);
    });

  return result;
};

type GroupTreeProps = {
  toggleView?: (viewType: ViewType) => void;
};

export const GroupTree = ({ toggleView }: GroupTreeProps) => {
  const { t } = useTranslation("groups");
  const { adminClient } = useAdminClient();

  const [data, setData] = useState<TreeViewDataItem[]>();
  const [filteredData, setFilteredData] = useState<TreeViewDataItem[]>();
  const [selectedRows, setSelectedRows] = useState<GroupRepresentation[]>([]);
  const [showDelete, toggleShowDelete] = useToggle();
  const [showCreate, toggleShowCreate] = useToggle();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  useFetch(
    () =>
      adminClient.groups.find({
        briefRepresentation: false,
      }),
    (groups) => setData(groups.map((g) => mapGroup(g, refresh))),
    [key]
  );

  return (
    <>
      <DeleteGroup
        show={showDelete}
        toggleDialog={toggleShowDelete}
        selectedRows={selectedRows}
        refresh={refresh}
      />
      {showCreate && (
        <GroupsModal handleModalToggle={toggleShowCreate} refresh={refresh} />
      )}
      {data ? (
        <>
          <TableToolbar
            inputGroupName="searchForGroups"
            inputGroupPlaceholder={t("groups:searchForGroups")}
            inputGroupOnEnter={(search) => {
              if (search === "") {
                setFilteredData(undefined);
              } else {
                setFilteredData(filterGroups(data, search));
              }
            }}
            toolbarItem={
              <GroupToolbar
                currentView={ViewType.Tree}
                toggleView={toggleView}
                toggleDelete={toggleShowDelete}
                toggleCreate={toggleShowCreate}
                kebabDisabled={selectedRows.length === 0}
              />
            }
          />
          <CheckableTreeView
            data={filteredData || data}
            onSelect={(items) =>
              setSelectedRows(items.reverse() as GroupRepresentation[])
            }
          />
        </>
      ) : (
        <KeycloakSpinner />
      )}
    </>
  );
};
