import { useState } from "react";
import { Link } from "react-router-dom-v5-compat";
import { useLocation, useNavigate } from "react-router-dom-v5-compat";
import { useTranslation } from "react-i18next";
import {
  Radio,
  SearchInput,
  Split,
  SplitItem,
  ToolbarItem,
} from "@patternfly/react-core";

import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { useAdminClient } from "../context/auth/AdminClient";
import { fetchAdminUI } from "../context/auth/admin-ui-endpoint";
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
import { GroupPath } from "../components/group/GroupPath";

type GroupTableProps = {
  toggleView?: (viewType: ViewType) => void;
};

type SearchType = "global" | "local";

type SearchGroup = GroupRepresentation & {
  link?: string;
};

const flatten = (groups: GroupRepresentation[], id?: string): SearchGroup[] => {
  let result: SearchGroup[] = [];
  for (const group of groups) {
    const link = `${id || ""}${id ? "/" : ""}${group.id}`;
    result.push({ ...group, link });
    if (group.subGroups) {
      result = [...result, ...flatten(group.subGroups, link)];
    }
  }
  return result;
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
  const refresh = () => setKey(key + 1);
  const [search, setSearch] = useState<string>();

  const navigate = useNavigate();
  const location = useLocation();
  const id = getLastId(location.pathname);
  const [searchType, setSearchType] = useState<SearchType>(
    id ? "local" : "global"
  );

  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-users") || currentGroup()?.access?.manage;
  const canView =
    hasAccess("query-groups", "view-users") ||
    hasAccess("manage-users", "query-groups");

  const loader = async (
    first?: number,
    max?: number
  ): Promise<SearchGroup[]> => {
    const params: Record<string, string> = {
      search: search || "",
      first: first?.toString() || "",
      max: max?.toString() || "",
    };
    if (searchType === "global" && search) {
      const result = await fetchAdminUI(adminClient, "admin-ui-groups", params);
      return flatten(result);
    }

    let groupsData = undefined;
    if (id) {
      const group = await adminClient.groups.findOne({ id });
      if (!group) {
        throw new Error(t("common:notFound"));
      }

      groupsData = !search
        ? group.subGroups
        : group.subGroups?.filter((g) => g.name?.includes(search));
    } else {
      groupsData = await fetchAdminUI(adminClient, "admin-ui-groups", {
        ...params,
        global: "false",
      });
    }

    if (!groupsData) {
      navigate(toGroups({ realm }));
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

  const Path = (group: SearchGroup) =>
    group.link ? <GroupPath group={group} /> : undefined;

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
        isPaginated
        isSearching={!!search}
        toolbarItem={
          <>
            <ToolbarItem>
              <SearchInput
                data-testid="group-search"
                placeholder={t("searchForGroups")}
                value={search}
                onChange={setSearch}
                onSearch={refresh}
                onClear={() => {
                  setSearch("");
                  refresh();
                }}
              />
            </ToolbarItem>
            <GroupToolbar
              currentView={ViewType.Table}
              toggleView={toggleView}
              toggleCreate={handleModalToggle}
              toggleDelete={toggleShowDelete}
              kebabDisabled={selectedRows!.length === 0}
            />
          </>
        }
        subToolbar={
          !!search &&
          !id && (
            <ToolbarItem>
              <Split hasGutter>
                <SplitItem>{t("searchFor")}</SplitItem>
                <SplitItem>
                  <Radio
                    id="global"
                    isChecked={searchType === "global"}
                    onChange={() => {
                      setSearchType("global");
                      refresh();
                    }}
                    name="searchType"
                    label={t("global")}
                  />
                </SplitItem>
                <SplitItem>
                  <Radio
                    id="local"
                    isChecked={searchType === "local"}
                    onChange={() => {
                      setSearchType("local");
                      refresh();
                    }}
                    name="searchType"
                    label={t("local")}
                  />
                </SplitItem>
              </Split>
            </ToolbarItem>
          )
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
          },
          {
            name: "path",
            displayKey: "groups:path",
            cellRenderer: Path,
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
