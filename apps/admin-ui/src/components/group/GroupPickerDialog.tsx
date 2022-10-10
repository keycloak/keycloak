import { useState } from "react";
import { useTranslation } from "react-i18next";
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

import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { ListEmptyState } from "../list-empty-state/ListEmptyState";
import { PaginatingTableToolbar } from "../table-toolbar/PaginatingTableToolbar";
import { GroupPath } from "./GroupPath";

export type GroupPickerDialogProps = {
  id?: string;
  type: "selectOne" | "selectMany";
  filterGroups?: string[];
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
  const { adminClient } = useAdminClient();
  const [selectedRows, setSelectedRows] = useState<SelectableGroup[]>([]);

  const [navigation, setNavigation] = useState<SelectableGroup[]>([]);
  const [groups, setGroups] = useState<SelectableGroup[]>([]);
  const [filter, setFilter] = useState("");
  const [joinedGroups, setJoinedGroups] = useState<GroupRepresentation[]>([]);
  const [groupId, setGroupId] = useState<string>();

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);

  const currentGroup = () => navigation[navigation.length - 1];

  useFetch(
    async () => {
      let group;
      let groups;
      let existingUserGroups;
      if (!groupId) {
        groups = await adminClient.groups.find({
          first,
          max: max + 1,
          search: filter,
        });
      } else {
        group = await adminClient.groups.findOne({ id: groupId });
        if (!group) {
          throw new Error(t("common:notFound"));
        }
        groups = group.subGroups!;
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
      }

      groups.forEach((group: SelectableGroup) => {
        group.checked = !!selectedRows.find((r) => r.id === group.id);
      });
      setGroups(groups);
    },
    [groupId, filter, first, max]
  );

  const isRowDisabled = (row?: GroupRepresentation) => {
    return [
      ...joinedGroups.map((item) => item.name),
      ...(filterGroups || []),
    ].some((group) => group === row?.name);
  };

  const hasSubgroups = (group: GroupRepresentation) =>
    group.subGroups?.length !== 0;

  const findSubGroup = (
    group: GroupRepresentation,
    name: string
  ): GroupRepresentation => {
    if (group.name?.includes(name)) {
      return group;
    }
    if (group.subGroups) {
      for (const g of group.subGroups) {
        const found = findSubGroup(g, name);
        return found;
      }
    }
    return group;
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t(text.title, {
        group1: filterGroups?.[0],
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
                : undefined
            );
          }}
          isDisabled={type === "selectMany" && selectedRows.length === 0}
        >
          {t(text.ok)}
        </Button>,
      ]}
    >
      <PaginatingTableToolbar
        count={groups.length}
        first={first}
        max={max}
        onNextClick={setFirst}
        onPreviousClick={setFirst}
        onPerPageSelect={(first, max) => {
          setFirst(first);
          setMax(max);
        }}
        inputGroupName={"common:search"}
        inputGroupOnEnter={(search) => {
          setFilter(search);
          setFirst(0);
          setMax(10);
          setNavigation([]);
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
            <DataListItem
              className={`join-group-dialog-row-${
                isRowDisabled(group) ? "disabled" : ""
              }`}
              aria-labelledby={group.name}
              key={group.id}
              id={group.id}
              onClick={(e) => {
                const g = filter !== "" ? findSubGroup(group, filter) : group;
                if (isRowDisabled(g)) return;
                if (type === "selectOne") {
                  setGroupId(g.id);
                } else if (
                  hasSubgroups(group) &&
                  filter === "" &&
                  (e.target as HTMLInputElement).type !== "checkbox"
                ) {
                  setGroupId(group.id);
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
                        newSelectedRows = selectedRows.filter(
                          (r) => r.id !== group.id
                        );
                      } else {
                        newSelectedRows = [
                          ...selectedRows,
                          filter === "" ? group : findSubGroup(group, filter),
                        ];
                      }

                      setSelectedRows(newSelectedRows);
                    }}
                    aria-labelledby={`select-${group.name}`}
                  />
                )}

                <DataListItemCells
                  dataListCells={[
                    <DataListCell key={`name-${group.id}`}>
                      {filter === "" ? (
                        <span id={`select-${group.name}`}>{group.name}</span>
                      ) : (
                        <GroupPath
                          id={`select-${group.name}`}
                          group={findSubGroup(group, filter)}
                        />
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
                  {((hasSubgroups(group) && filter === "" && canBrowse) ||
                    type === "selectOne") && (
                    <Button
                      isDisabled
                      variant="link"
                      aria-label={t("common:select")}
                    >
                      <AngleRightIcon />
                    </Button>
                  )}
                </DataListAction>
              </DataListItemRow>
            </DataListItem>
          ))}
          {groups.length === 0 && filter === "" && (
            <ListEmptyState
              hasIcon={false}
              message={t("groups:moveGroupEmpty")}
              instructions={t("groups:moveGroupEmptyInstructions")}
            />
          )}
          {groups.length === 0 && filter !== "" && (
            <ListEmptyState
              message={t("common:noSearchResults")}
              instructions={t("common:noSearchResultsInstructions")}
            />
          )}
        </DataList>
      </PaginatingTableToolbar>
    </Modal>
  );
};
