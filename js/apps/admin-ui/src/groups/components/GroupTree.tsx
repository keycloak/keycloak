import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import {
  AlertVariant,
  Button,
  Checkbox,
  Dropdown,
  DropdownItem,
  DropdownPosition,
  DropdownSeparator,
  InputGroup,
  KebabToggle,
  Tooltip,
  TreeView,
  TreeViewDataItem,
} from "@patternfly/react-core";
import { AngleRightIcon } from "@patternfly/react-icons";
import { unionBy } from "lodash-es";
import { useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAlerts } from "../../components/alert/Alerts";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { PaginatingTableToolbar } from "../../components/table-toolbar/PaginatingTableToolbar";
import { useAccess } from "../../context/access/Access";
import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useFetch } from "../../utils/useFetch";
import useToggle from "../../utils/useToggle";
import { GroupsModal } from "../GroupsModal";
import { useSubGroups } from "../SubGroupsContext";
import { toGroups } from "../routes/Groups";
import { DeleteGroup } from "./DeleteGroup";
import { MoveDialog } from "./MoveDialog";

import "./group-tree.css";

type GroupTreeContextMenuProps = {
  group: GroupRepresentation;
  refresh: () => void;
};

export function countGroups(groups: GroupRepresentation[]) {
  let count = groups.length;
  for (const group of groups) {
    if (group.subGroups) {
      count += countGroups(group.subGroups);
    }
  }
  return count;
}

const GroupTreeContextMenu = ({
  group,
  refresh,
}: GroupTreeContextMenuProps) => {
  const { t } = useTranslation();

  const [isOpen, toggleOpen] = useToggle();
  const [renameOpen, toggleRenameOpen] = useToggle();
  const [createOpen, toggleCreateOpen] = useToggle();
  const [moveOpen, toggleMoveOpen] = useToggle();
  const [deleteOpen, toggleDeleteOpen] = useToggle();
  const navigate = useNavigate();
  const { realm } = useRealm();

  return (
    <>
      {renameOpen && (
        <GroupsModal
          id={group.id}
          rename={group}
          refresh={() => {
            navigate(toGroups({ realm }));
            refresh();
          }}
          handleModalToggle={toggleRenameOpen}
        />
      )}
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
        refresh={() => {
          navigate(toGroups({ realm }));
          refresh();
        }}
      />
      <Dropdown
        toggle={<KebabToggle onToggle={toggleOpen} />}
        isOpen={isOpen}
        isPlain
        position={DropdownPosition.right}
        dropdownItems={[
          <DropdownItem key="rename" onClick={toggleRenameOpen}>
            {t("rename")}
          </DropdownItem>,
          <DropdownItem key="move" onClick={toggleMoveOpen}>
            {t("moveTo")}
          </DropdownItem>,
          <DropdownItem key="create" onClick={toggleCreateOpen}>
            {t("createChildGroup")}
          </DropdownItem>,
          <DropdownSeparator key="separator" />,
          <DropdownItem key="delete" onClick={toggleDeleteOpen}>
            {t("delete")}
          </DropdownItem>,
        ]}
      />
    </>
  );
};

type GroupTreeProps = {
  refresh: () => void;
  canViewDetails: boolean;
};

const SUBGROUP_COUNT = 50;

export const GroupTree = ({
  refresh: viewRefresh,
  canViewDetails,
}: GroupTreeProps) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const navigate = useNavigate();
  const { addAlert } = useAlerts();
  const { hasAccess } = useAccess();

  const [data, setData] = useState<TreeViewDataItem[]>();
  const [groups, setGroups] = useState<GroupRepresentation[]>([]);
  const { subGroups, setSubGroups } = useSubGroups();

  const [search, setSearch] = useState("");
  const [max, setMax] = useState(20);
  const [first, setFirst] = useState(0);
  const prefFirst = useRef(0);
  const prefMax = useRef(20);
  const [count, setCount] = useState(0);
  const [exact, setExact] = useState(false);
  const [activeItem, setActiveItem] = useState<TreeViewDataItem>();

  const [firstSub, setFirstSub] = useState(0);

  const [key, setKey] = useState(0);
  const refresh = () => {
    setKey(key + 1);
    viewRefresh();
  };

  const mapGroup = (
    group: GroupRepresentation,
    refresh: () => void,
  ): TreeViewDataItem => {
    return {
      id: group.id,
      name: (
        <Tooltip content={group.name}>
          <span>{group.name}</span>
        </Tooltip>
      ),
      children:
        group.subGroups && group.subGroups.length > 0
          ? group.subGroups.map((g) => mapGroup(g, refresh))
          : undefined,
      action: (hasAccess("manage-users") || group.access?.manage) && (
        <GroupTreeContextMenu group={group} refresh={refresh} />
      ),
      defaultExpanded: subGroups.map((g) => g.id).includes(group.id),
    };
  };

  useFetch(
    async () => {
      const groups = await fetchAdminUI<GroupRepresentation[]>(
        "groups",
        Object.assign(
          {
            first: `${first}`,
            max: `${max + 1}`,
            exact: `${exact}`,
            global: `${search !== ""}`,
          },
          search === "" ? null : { search },
        ),
      );
      let subGroups: GroupRepresentation[] = [];
      if (activeItem) {
        subGroups = await fetchAdminUI<GroupRepresentation[]>(
          `groups/${activeItem.id}/children`,
          {
            first: `${firstSub}`,
            max: `${SUBGROUP_COUNT}`,
          },
        );
      }
      return { groups, subGroups };
    },
    ({ groups, subGroups }) => {
      const found: TreeViewDataItem[] = [];
      if (activeItem) findGroup(data || [], activeItem.id!, [], found);

      if (found.length && subGroups.length) {
        const foundTreeItem = found.pop()!;
        foundTreeItem.children = [
          ...(unionBy(foundTreeItem.children || []).splice(0, SUBGROUP_COUNT),
          subGroups.map((g) => mapGroup(g, refresh), "id")),
          ...(subGroups.length === SUBGROUP_COUNT
            ? [
                {
                  id: "next",
                  name: (
                    <Button
                      variant="plain"
                      onClick={() => setFirstSub(firstSub + SUBGROUP_COUNT)}
                    >
                      <AngleRightIcon />
                    </Button>
                  ),
                },
              ]
            : []),
        ];
      }
      setGroups(groups);
      if (search || prefFirst.current !== first || prefMax.current !== max) {
        setData(groups.map((g) => mapGroup(g, refresh)));
      } else {
        setData(
          unionBy(
            data,
            groups.map((g) => mapGroup(g, refresh)),
            "id",
          ),
        );
      }
      setCount(countGroups(groups));
      prefFirst.current = first;
      prefMax.current = max;
    },
    [key, first, firstSub, max, search, exact, activeItem],
  );

  const findGroup = (
    groups: GroupRepresentation[] | TreeViewDataItem[],
    id: string,
    path: (GroupRepresentation | TreeViewDataItem)[],
    found: (GroupRepresentation | TreeViewDataItem)[],
  ) => {
    return groups.map((group) => {
      if (found.length > 0) return;

      if ("subGroups" in group && group.subGroups?.length) {
        findGroup(group.subGroups, id, [...path, group], found);
      }

      if ("children" in group && group.children) {
        findGroup(group.children, id, [...path, group], found);
      }

      if (group.id === id) {
        found.push(...path, group);
      }
    });
  };

  return data ? (
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
      inputGroupName="searchForGroups"
      inputGroupPlaceholder={t("searchForGroups")}
      inputGroupOnEnter={setSearch}
      toolbarItem={
        <InputGroup className="pf-u-pt-sm">
          <Checkbox
            id="exact"
            data-testid="exact-search"
            name="exact"
            isChecked={exact}
            onChange={(value) => setExact(value)}
          />
          <label htmlFor="exact" className="pf-u-pl-sm">
            {t("exactSearch")}
          </label>
        </InputGroup>
      }
    >
      {data.length > 0 && (
        <TreeView
          data={data.slice(0, max)}
          allExpanded={search.length > 0}
          activeItems={activeItem ? [activeItem] : undefined}
          hasGuides
          hasSelectableNodes
          className="keycloak_groups_treeview"
          onSelect={(_, item) => {
            if (item.id === "next") return;
            setActiveItem(item);
            const id = item.id?.substring(item.id.lastIndexOf("/") + 1);
            const subGroups: GroupRepresentation[] = [];
            findGroup(groups, id!, [], subGroups);
            setSubGroups(subGroups);

            if (canViewDetails || subGroups.at(-1)?.access?.view) {
              navigate(toGroups({ realm, id: item.id }));
              refresh();
            } else {
              addAlert(t("noViewRights"), AlertVariant.warning);
              navigate(toGroups({ realm }));
            }
          }}
        />
      )}
    </PaginatingTableToolbar>
  ) : (
    <KeycloakSpinner />
  );
};
