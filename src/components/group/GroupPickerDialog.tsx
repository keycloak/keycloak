import React, { useState } from "react";
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

import type GroupRepresentation from "keycloak-admin/lib/defs/groupRepresentation";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { ListEmptyState } from "../list-empty-state/ListEmptyState";
import { PaginatingTableToolbar } from "../table-toolbar/PaginatingTableToolbar";

export type GroupPickerDialogProps = {
  id?: string;
  type: "selectOne" | "selectMany";
  filterGroups?: string[];
  text: { title: string; ok: string };
  onConfirm: (groups: GroupRepresentation[]) => void;
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
  onClose,
  onConfirm,
}: GroupPickerDialogProps) => {
  const { t } = useTranslation();
  const adminClient = useAdminClient();
  const [selectedRows, setSelectedRows] = useState<SelectableGroup[]>([]);

  const [navigation, setNavigation] = useState<SelectableGroup[]>([]);
  const [groups, setGroups] = useState<SelectableGroup[]>([]);
  const [filtered, setFiltered] = useState<GroupRepresentation[]>();
  const [filter, setFilter] = useState("");
  const [joinedGroups, setJoinedGroups] = useState<GroupRepresentation[]>([]);
  const [groupId, setGroupId] = useState<string>();

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);

  const currentGroup = () => navigation[navigation.length - 1];

  useFetch(
    async () => {
      const allGroups = await adminClient.groups.find();

      if (groupId) {
        const group = await adminClient.groups.findOne({ id: groupId });
        return { group, groups: group.subGroups! };
      } else if (id) {
        const existingUserGroups = await adminClient.users.listGroups({
          id,
        });
        return {
          groups: allGroups,
          existingUserGroups,
        };
      } else
        return {
          groups: allGroups,
        };
    },
    async ({ group: selectedGroup, groups, existingUserGroups }) => {
      setJoinedGroups(existingUserGroups || []);
      if (selectedGroup) {
        setNavigation([...navigation, selectedGroup]);
      }

      groups.forEach((group: SelectableGroup) => {
        group.checked = !!selectedRows.find((r) => r.id === group.id);
      });
      setFiltered(undefined);
      setFilter("");
      setFirst(0);
      setMax(10);
      setGroups(
        filterGroups
          ? [
              ...groups.filter(
                (row) => filterGroups && !filterGroups.includes(row.name!)
              ),
            ]
          : groups
      );
    },
    [groupId]
  );

  const isRowDisabled = (row?: GroupRepresentation) => {
    return !!joinedGroups.find((group) => group.id === row?.id);
  };

  const hasSubgroups = (group: GroupRepresentation) => {
    return group.subGroups!.length !== 0;
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t(text.title, {
        group1: filterGroups && filterGroups[0],
        group2: currentGroup() ? currentGroup().name : t("root"),
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
            onConfirm(type === "selectMany" ? selectedRows : [currentGroup()]);
          }}
          isDisabled={type === "selectMany" && selectedRows.length === 0}
        >
          {t(text.ok)}
        </Button>,
      ]}
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
            {navigation.length - 1 === i && <>{group.name}</>}
          </BreadcrumbItem>
        ))}
      </Breadcrumb>

      <PaginatingTableToolbar
        count={(filtered || groups).slice(first, first + max).length}
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
          setFiltered(
            groups.filter((group) =>
              group.name?.toLowerCase().includes(search.toLowerCase())
            )
          );
        }}
        inputGroupPlaceholder={t("users:searchForGroups")}
      >
        <DataList aria-label={t("groups")} isCompact>
          {(filtered || groups)
            .slice(first, first + max)
            .map((group: SelectableGroup) => (
              <DataListItem
                aria-labelledby={group.name}
                key={group.id}
                id={group.id}
                onClick={(e) => {
                  if (type === "selectOne") {
                    setGroupId(group.id);
                  } else if (
                    hasSubgroups(group) &&
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
                      className="join-group-modal-check"
                      data-testid={`${group.name}-check`}
                      checked={group.checked}
                      isDisabled={isRowDisabled(group)}
                      onChange={(checked) => {
                        group.checked = checked;
                        let newSelectedRows: SelectableGroup[] = [];
                        if (!group.checked) {
                          newSelectedRows = selectedRows.filter(
                            (r) => r.id !== group.id
                          );
                        } else if (group.checked) {
                          newSelectedRows = [...selectedRows, group];
                        }

                        setSelectedRows(newSelectedRows);
                      }}
                      aria-labelledby="data-list-check"
                    />
                  )}

                  <DataListItemCells
                    dataListCells={[
                      <DataListCell key={`name-${group.id}`}>
                        <>{group.name}</>
                      </DataListCell>,
                    ]}
                  />
                  <DataListAction
                    aria-labelledby={`select-${group.name}`}
                    id={`select-${group.name}`}
                    aria-label={t("groupName")}
                    isPlainButtonAction
                  >
                    {(hasSubgroups(group) || type === "selectOne") && (
                      <Button isDisabled variant="link">
                        <AngleRightIcon />
                      </Button>
                    )}
                  </DataListAction>
                </DataListItemRow>
              </DataListItem>
            ))}
          {(filtered || groups).length === 0 && filter === "" && (
            <ListEmptyState
              hasIcon={false}
              message={t("groups:moveGroupEmpty")}
              instructions={t("groups:moveGroupEmptyInstructions")}
            />
          )}
        </DataList>
      </PaginatingTableToolbar>
    </Modal>
  );
};
