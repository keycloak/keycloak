import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import {
  Breadcrumb,
  BreadcrumbItem,
  Button,
  DataList,
  DataListAction,
  DataListCell,
  DataListCheck,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { AngleRightIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import { adminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";
import { ListEmptyState } from "../list-empty-state/ListEmptyState";
import { PaginatingTableToolbar } from "../table-toolbar/PaginatingTableToolbar";
import { GroupPath } from "./GroupPath";

import "./group-picker-dialog.css";
import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";

export type GroupPickerDialogProps = {
  id?: string;
  type: "selectOne" | "selectMany";
  filterGroups?: GroupRepresentation[];
  text: { title: string; ok: string };
  canBrowse?: boolean;
  onConfirm: (groups: GroupRepresentation[] | undefined) => void;
  onClose: () => void;
};

type SelectableGroup = GroupRepresentation & {
  checked?: boolean;
};

export const GroupPickerDialog = ({
  id,
  type,
  filterGroups,
  text,
  canBrowse = true,
  onClose,
  onConfirm,
}: GroupPickerDialogProps) => {
  const { t } = useTranslation();
  const [selectedRows, setSelectedRows] = useState<SelectableGroup[]>([]);

  const [navigation, setNavigation] = useState<SelectableGroup[]>([]);
  const [groups, setGroups] = useState<SelectableGroup[]>([]);
  const [filter, setFilter] = useState("");
  const [joinedGroups, setJoinedGroups] = useState<GroupRepresentation[]>([]);
  const [groupId, setGroupId] = useState<string>();
  const [isSearching, setIsSearching] = useState(false);

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);

  const [count, setCount] = useState(0);

  const currentGroup = () => navigation[navigation.length - 1];

  useFetch(
    async () => {
      let group;
      let groups;
      let existingUserGroups;
      let count = 0;
      if (!groupId) {
        groups = await fetchAdminUI<GroupRepresentation[]>(
          "ui-ext/groups",
          Object.assign(
            {
              first: `${first}`,
              max: `${max + 1}`,
            },
            isSearching ? null : { search: filter },
          ),
        );
      } else if (!navigation.map(({ id }) => id).includes(groupId)) {
        group = await adminClient.groups.findOne({ id: groupId });
        if (!group) {
          throw new Error(t("notFound"));
        }
        groups = group.subGroups!;
      }

      if (isSearching) {
        count = (await adminClient.groups.count({ search: filter, top: true }))
          .count;
      }

      if (id) {
        existingUserGroups = await adminClient.users.listGroups({
          id,
        });
      }

      return { group, groups, existingUserGroups, count };
    },
    async ({ group: selectedGroup, groups, existingUserGroups, count }) => {
      setJoinedGroups(existingUserGroups || []);
      if (selectedGroup) {
        setNavigation([...navigation, selectedGroup]);
      }

      if (groups) {
        groups.forEach((group: SelectableGroup) => {
          group.checked = !!selectedRows.find((r) => r.id === group.id);
        });
        setGroups(groups);
      }
      setCount(count);
    },
    [groupId, filter, first, max],
  );

  const isRowDisabled = (row?: GroupRepresentation) => {
    return [
      ...joinedGroups.map((item) => item.id),
      ...(filterGroups || []).map((group) => group.id),
    ].some((group) => group === row?.id);
  };

  return (
    <Modal
      variant={isSearching ? ModalVariant.medium : ModalVariant.small}
      title={t(text.title, {
        group1: filterGroups?.[0]?.name,
        group2: navigation.length ? currentGroup().name : t("root"),
      })}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          data-testid={`${text.ok}-button`}
          key="confirm"
          variant="primary"
          form="group-form"
          onClick={() => {
            onConfirm(
              type === "selectMany"
                ? selectedRows
                : navigation.length
                ? [currentGroup()]
                : undefined,
            );
          }}
          isDisabled={type === "selectMany" && selectedRows.length === 0}
        >
          {t(text.ok)}
        </Button>,
      ]}
    >
      <PaginatingTableToolbar
        count={
          (isSearching ? count : groups.length) -
          (groupId || isSearching ? first : 0)
        }
        first={first}
        max={max}
        onNextClick={setFirst}
        onPreviousClick={setFirst}
        onPerPageSelect={(first, max) => {
          setFirst(first);
          setMax(max);
        }}
        inputGroupName={"search"}
        inputGroupOnEnter={(search) => {
          setFilter(search);
          setIsSearching(search !== "");
          setFirst(0);
          setMax(10);
          setNavigation([]);
          setGroupId(undefined);
        }}
        inputGroupPlaceholder={t("users:searchForGroups")}
      >
        <Breadcrumb>
          {navigation.length > 0 && (
            <BreadcrumbItem key="home">
              <Button
                variant="link"
                onClick={() => {
                  setGroupId(undefined);
                  setNavigation([]);
                  setFirst(0);
                  setMax(10);
                }}
              >
                {t("groups")}
              </Button>
            </BreadcrumbItem>
          )}
          {navigation.map((group, i) => (
            <BreadcrumbItem key={i}>
              {navigation.length - 1 !== i && (
                <Button
                  variant="link"
                  onClick={() => {
                    setGroupId(group.id);
                    setNavigation([...navigation].slice(0, i));
                    setFirst(0);
                    setMax(10);
                  }}
                >
                  {group.name}
                </Button>
              )}
              {navigation.length - 1 === i && group.name}
            </BreadcrumbItem>
          ))}
        </Breadcrumb>
        <DataList aria-label={t("groups")} isCompact>
          {groups
            .slice(groupId ? first : 0, max + (groupId ? first : 0))
            .map((group: SelectableGroup) => (
              <>
                <GroupRow
                  key={group.id}
                  group={group}
                  isRowDisabled={isRowDisabled}
                  onSelect={setGroupId}
                  type={type}
                  isSearching={isSearching}
                  setIsSearching={setIsSearching}
                  selectedRows={selectedRows}
                  setSelectedRows={setSelectedRows}
                  canBrowse={canBrowse}
                />
                {isSearching &&
                  group.subGroups?.length !== 0 &&
                  group.subGroups!.map((g) => (
                    <GroupRow
                      key={g.id}
                      group={g}
                      isRowDisabled={isRowDisabled}
                      onSelect={setGroupId}
                      type={type}
                      isSearching={isSearching}
                      setIsSearching={setIsSearching}
                      selectedRows={selectedRows}
                      setSelectedRows={setSelectedRows}
                      canBrowse={canBrowse}
                    />
                  ))}
              </>
            ))}
        </DataList>
        {groups.length === 0 && !isSearching && (
          <ListEmptyState
            hasIcon={false}
            message={t("moveGroupEmpty")}
            instructions={t("moveGroupEmptyInstructions")}
          />
        )}
        {groups.length === 0 && isSearching && (
          <ListEmptyState
            message={t("noSearchResults")}
            instructions={t("noSearchResultsInstructions")}
          />
        )}
      </PaginatingTableToolbar>
    </Modal>
  );
};

type GroupRowProps = {
  group: SelectableGroup;
  type: "selectOne" | "selectMany";
  isRowDisabled: (row?: GroupRepresentation) => boolean;
  isSearching: boolean;
  setIsSearching: (value: boolean) => void;
  onSelect: (groupId: string) => void;
  selectedRows: SelectableGroup[];
  setSelectedRows: (groups: SelectableGroup[]) => void;
  canBrowse: boolean;
};

const GroupRow = ({
  group,
  type,
  isRowDisabled,
  isSearching,
  setIsSearching,
  onSelect,
  selectedRows,
  setSelectedRows,
  canBrowse,
}: GroupRowProps) => {
  const { t } = useTranslation();

  const hasSubgroups = (group: GroupRepresentation) =>
    group.subGroups?.length !== 0;

  return (
    <DataListItem
      aria-labelledby={group.name}
      key={group.id}
      id={group.id}
      onClick={(e) => {
        if (type === "selectOne") {
          onSelect(group.id!);
        } else if (
          hasSubgroups(group) &&
          (e.target as HTMLInputElement).type !== "checkbox"
        ) {
          onSelect(group.id!);
          setIsSearching(false);
        }
      }}
    >
      <DataListItemRow
        className={`join-group-dialog-row-${
          isRowDisabled(group) ? "m-disabled" : ""
        }`}
        data-testid={group.name}
      >
        {type === "selectMany" && (
          <DataListCheck
            className="kc-join-group-modal-check"
            data-testid={`${group.name}-check`}
            aria-label={group.name}
            checked={group.checked}
            isDisabled={isRowDisabled(group)}
            onChange={(checked) => {
              group.checked = checked;
              let newSelectedRows: SelectableGroup[] = [];
              if (!group.checked) {
                newSelectedRows = selectedRows.filter((r) => r.id !== group.id);
              } else {
                newSelectedRows = [...selectedRows, group];
              }

              setSelectedRows(newSelectedRows);
            }}
            aria-labelledby={`select-${group.name}`}
          />
        )}

        <DataListItemCells
          dataListCells={[
            <DataListCell
              key={`name-${group.id}`}
              className="keycloak-groups-group-path"
            >
              {isSearching ? (
                <GroupPath id={`select-${group.name}`} group={group} />
              ) : (
                <span id={`select-${group.name}`}>{group.name}</span>
              )}
            </DataListCell>,
          ]}
        />
        <DataListAction
          id="actions"
          aria-labelledby={`select-${group.name}`}
          aria-label={t("groupName")}
          isPlainButtonAction
        >
          {((hasSubgroups(group) && canBrowse) || type === "selectOne") && (
            <Button variant="link" aria-label={t("select")}>
              <AngleRightIcon />
            </Button>
          )}
        </DataListAction>
      </DataListItemRow>
    </DataListItem>
  );
};
