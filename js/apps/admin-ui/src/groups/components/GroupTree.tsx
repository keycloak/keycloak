import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import {
  AlertVariant,
  Button,
  Checkbox,
  Divider,
  Dropdown,
  DropdownItem,
  DropdownList,
  InputGroup,
  InputGroupItem,
  MenuToggle,
  Spinner,
  Tooltip,
  TreeView,
  TreeViewDataItem,
} from "@patternfly/react-core";

import {
  PaginatingTableToolbar,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { AngleRightIcon, EllipsisVIcon } from "@patternfly/react-icons";
import { unionBy } from "lodash-es";
import { useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { useAccess } from "../../context/access/Access";
import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";
import { useRealm } from "../../context/realm-context/RealmContext";
import useToggle from "../../utils/useToggle";
import { GroupsModal } from "../GroupsModal";
import { useSubGroups } from "../SubGroupsContext";
import { toGroups } from "../routes/Groups";
import { DeleteGroup } from "./DeleteGroup";
import { MoveDialog } from "./MoveDialog";

import "./group-tree.css";

type ExtendedTreeViewDataItem = TreeViewDataItem & {
  access?: Record<string, boolean>;
};

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
        popperProps={{
          position: "right",
        }}
        onOpenChange={toggleOpen}
        toggle={(ref) => (
          <MenuToggle
            ref={ref}
            onClick={toggleOpen}
            isExpanded={isOpen}
            variant="plain"
            aria-label="Actions"
          >
            <EllipsisVIcon />
          </MenuToggle>
        )}
        isOpen={isOpen}
      >
        <DropdownList>
          <DropdownItem key="rename" onClick={toggleRenameOpen}>
            {t("rename")}
          </DropdownItem>
          <DropdownItem key="move" onClick={toggleMoveOpen}>
            {t("moveTo")}
          </DropdownItem>
          <DropdownItem key="create" onClick={toggleCreateOpen}>
            {t("createChildGroup")}
          </DropdownItem>
          <Divider key="separator" />,
          <DropdownItem key="delete" onClick={toggleDeleteOpen}>
            {t("delete")}
          </DropdownItem>
        </DropdownList>
      </Dropdown>
    </>
  );
};

type GroupTreeProps = {
  refresh: () => void;
  canViewDetails: boolean;
};

const SUBGROUP_COUNT = 50;

const TreeLoading = () => {
  const { t } = useTranslation();
  return (
    <>
      <Spinner size="sm" /> {t("spinnerLoading")}
    </>
  );
};

const LOADING_TREE = [
  {
    name: <TreeLoading />,
  },
];

export const GroupTree = ({
  refresh: viewRefresh,
  canViewDetails,
}: GroupTreeProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { realm } = useRealm();
  const navigate = useNavigate();
  const { addAlert } = useAlerts();
  const { hasAccess } = useAccess();

  const [data, setData] = useState<ExtendedTreeViewDataItem[]>();
  const { subGroups, clear } = useSubGroups();

  const [search, setSearch] = useState("");
  const [max, setMax] = useState(20);
  const [first, setFirst] = useState(0);
  const prefFirst = useRef(0);
  const prefMax = useRef(20);
  const [count, setCount] = useState(0);
  const [exact, setExact] = useState(false);
  const [activeItem, setActiveItem] = useState<ExtendedTreeViewDataItem>();

  const [firstSub, setFirstSub] = useState(0);

  const [key, setKey] = useState(0);
  const refresh = () => {
    setKey(key + 1);
    viewRefresh();
  };

  const mapGroup = (
    group: GroupRepresentation,
    refresh: () => void,
  ): ExtendedTreeViewDataItem => {
    const hasSubGroups = group.subGroupCount;
    return {
      id: group.id,
      name: (
        <Tooltip content={group.name}>
          <span>{group.name}</span>
        </Tooltip>
      ),
      access: group.access || {},
      children: hasSubGroups
        ? search.length === 0
          ? LOADING_TREE
          : group.subGroups?.map((g) => mapGroup(g, refresh))
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
        adminClient,
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
          adminClient,
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
      if (activeItem) {
        const found = findGroup(data || [], activeItem.id!, []);
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
      }
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
    groups: ExtendedTreeViewDataItem[],
    id: string,
    path: ExtendedTreeViewDataItem[],
  ) => {
    for (let index = 0; index < groups.length; index++) {
      const group = groups[index];
      if (group.id === id) {
        path.push(group);
        return path;
      }

      if (group.children) {
        path.push(group);
        findGroup(group.children, id, path);
        if (path[path.length - 1].id !== id) {
          path.pop();
        }
      }
    }
    return path;
  };

  const nav = (item: TreeViewDataItem, data: ExtendedTreeViewDataItem[]) => {
    if (item.id === "next") return;
    setActiveItem(item);

    const path = findGroup(data, item.id!, []);
    if (!subGroups.every(({ id }) => path.find((t) => t.id === id))) clear();
    if (
      canViewDetails ||
      path.at(-1)?.access?.view ||
      subGroups.at(-1)?.access?.view
    ) {
      navigate(
        toGroups({
          realm,
          id: path.map((g) => g.id).join("/"),
        }),
      );
    } else {
      addAlert(t("noViewRights"), AlertVariant.warning);
      navigate(toGroups({ realm }));
    }
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
        <InputGroup className="pf-v5-u-pt-sm">
          <InputGroupItem>
            <Checkbox
              id="exact"
              data-testid="exact-search"
              name="exact"
              isChecked={exact}
              onChange={(_event, value) => setExact(value)}
              className="pf-v5-u-mr-xs"
            />
          </InputGroupItem>
          <InputGroupItem>
            <label htmlFor="exact" className="pf-v5-u-pl-sm">
              {t("exactSearch")}
            </label>
          </InputGroupItem>
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
          onExpand={(_, item) => {
            nav(item, data);
          }}
          onSelect={(_, item) => {
            nav(item, data);
          }}
        />
      )}
    </PaginatingTableToolbar>
  ) : (
    <KeycloakSpinner />
  );
};
