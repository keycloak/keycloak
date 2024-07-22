import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import {
  GroupQuery,
  SubGroupQuery,
} from "@keycloak/keycloak-admin-client/lib/resources/groups";
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
import { Fragment, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";
import { ListEmptyState } from "../list-empty-state/ListEmptyState";
import { PaginatingTableToolbar } from "../table-toolbar/PaginatingTableToolbar";
import { GroupPath } from "./GroupPath";

import "./group-picker-dialog.css";

export type GroupPickerDialogProps = {
  id?: string;
  type: "selectOne" | "selectMany";
  filterGroups?: GroupRepresentation[];
  text: { title: string; ok: string };
  canBrowse?: boolean;
  isMove?: boolean;
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
  isMove = false,
  onClose,
  onConfirm,
}: GroupPickerDialogProps) => {
  const { adminClient } = useAdminClient();

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

      if (!groupId) {
        const args: GroupQuery = {
          first,
          max: max + 1,
        };
        if (isSearching) {
          args.search = filter;
        }
        groups = await adminClient.groups.find(args);
      } else {
        if (!navigation.map(({ id }) => id).includes(groupId)) {
          group = await adminClient.groups.findOne({ id: groupId });
          if (!group) {
            throw new Error(t("notFound"));
          }
        }

        const args: SubGroupQuery = {
          first,
          max,
          parentId: groupId,
        };
        groups = await adminClient.groups.listSubGroups(args);
      }

      if (id) {
        existingUserGroups = await adminClient.users.listGroups({
          id,
        });
      }

      return { group, groups, existingUserGroups };
    },
    async ({ group: selectedGroup, groups, existingUserGroups }) => {
      setJoinedGroups(existingUserGroups || []);
      if (selectedGroup) {
        setNavigation([...navigation, selectedGroup]);
        setCount(selectedGroup.subGroupCount!);
      }

      groups.forEach((group: SelectableGroup) => {
        group.checked = !!selectedRows.find((r) => r.id === group.id);
      });
      setGroups(groups);
      if (isSearching || !groupId) {
        setCount(groups.length);
      }
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
        count={count}
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
        inputGroupPlaceholder={t("searchForGroups")}
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
          {groups.slice(0, max).map((group: SelectableGroup) => (
            <Fragment key={group.id}>
              {(!isSearching || group.name?.includes(filter)) && (
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
              )}
              {isSearching &&
                group.subGroups?.map((g) => (
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
            </Fragment>
          ))}
        </DataList>
        {groups.length === 0 && !isSearching && (
          <ListEmptyState
            hasIcon={false}
            message={t("moveGroupEmpty")}
            instructions={isMove ? t("moveGroupEmptyInstructions") : undefined}
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

  return (
    <DataListItem
      aria-labelledby={group.name}
      key={group.id}
      id={group.id}
      onClick={(e) => {
        if (type === "selectOne") {
          onSelect(group.id!);
        } else if (
          (e.target as HTMLInputElement).type !== "checkbox" &&
          group.subGroupCount !== 0
        ) {
          onSelect(group.id!);
          setIsSearching(false);
        }
      }}
    >
      <DataListItemRow
        className={`join-group-dialog-row${
          isRowDisabled(group) ? "-m-disabled" : ""
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
            onChange={(_event, checked) => {
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
          {(canBrowse || type === "selectOne") && group.subGroupCount !== 0 && (
            <Button variant="link" aria-label={t("select")}>
              <AngleRightIcon />
            </Button>
          )}
        </DataListAction>
      </DataListItemRow>
    </DataListItem>
  );
};
