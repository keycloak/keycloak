import React, { useEffect, useState } from "react";
import {
  Breadcrumb,
  BreadcrumbItem,
  Button,
  ButtonVariant,
  DataList,
  DataListAction,
  DataListCell,
  DataListCheck,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  InputGroup,
  Modal,
  ModalVariant,
  TextInput,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { asyncStateFetch, useAdminClient } from "../context/auth/AdminClient";
import { AngleRightIcon, SearchIcon } from "@patternfly/react-icons";
import GroupRepresentation from "keycloak-admin/lib/defs/groupRepresentation";
import { useErrorHandler } from "react-error-boundary";
import _ from "lodash";
import { useParams } from "react-router-dom";

export type JoinGroupDialogProps = {
  open: boolean;
  toggleDialog: () => void;
  onClose: () => void;
  username: string;
  onConfirm: (newGroups: Group[]) => void;
};

type Group = GroupRepresentation & {
  checked?: boolean;
};

export const JoinGroupDialog = ({
  onClose,
  open,
  toggleDialog,
  onConfirm,
  username,
}: JoinGroupDialogProps) => {
  const { t } = useTranslation("roles");
  const adminClient = useAdminClient();
  const [selectedRows, setSelectedRows] = useState<Group[]>([]);

  const errorHandler = useErrorHandler();

  const [navigation, setNavigation] = useState<Group[]>([]);
  const [groups, setGroups] = useState<Group[]>([]);
  const [filtered, setFiltered] = useState<GroupRepresentation[]>();
  const [filter, setFilter] = useState("");

  const [groupId, setGroupId] = useState<string>();

  const { id } = useParams<{ id: string }>();

  useEffect(
    () =>
      asyncStateFetch(
        async () => {
          const existingUserGroups = await adminClient.users.listGroups({ id });
          const allGroups = await adminClient.groups.find();

          if (groupId) {
            const group = await adminClient.groups.findOne({ id: groupId });
            return { group, groups: group.subGroups! };
          } else {
            return {
              groups: _.differenceBy(allGroups, existingUserGroups, "id"),
            };
          }
        },
        async ({ group: selectedGroup, groups }) => {
          if (selectedGroup) {
            setNavigation([...navigation, selectedGroup]);
          }

          groups.forEach((group: Group) => {
            group.checked = !!selectedRows.find((r) => r.id === group.id);
          });
          setGroups(groups);
        },
        errorHandler
      ),
    [groupId]
  );

  return (
    <Modal
      variant={ModalVariant.small}
      title={`Join groups for user ${username}`}
      isOpen={open}
      onClose={onClose}
      actions={[
        <Button
          data-testid="joinGroup"
          key="confirm"
          variant="primary"
          form="group-form"
          onClick={() => {
            toggleDialog();
            onConfirm(selectedRows);
          }}
          isDisabled={selectedRows.length === 0}
        >
          {t("users:Join")}
        </Button>,
      ]}
    >
      <Breadcrumb>
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

      <Toolbar>
        <ToolbarContent>
          <ToolbarItem>
            <InputGroup>
              <TextInput
                type="search"
                aria-label={t("common:search")}
                placeholder={t("users:searchForGroups")}
                onChange={(value) => {
                  if (value === "") {
                    setFiltered(undefined);
                  }
                  setFilter(value);
                }}
              />
              <Button
                variant={ButtonVariant.control}
                aria-label={t("common:search")}
                onClick={() =>
                  setFiltered(
                    groups.filter((group) =>
                      group.name?.toLowerCase().includes(filter.toLowerCase())
                    )
                  )
                }
              >
                <SearchIcon />
              </Button>
            </InputGroup>
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>
      <DataList
        onSelectDataListItem={(value) => {
          setGroupId(value);
        }}
        aria-label={t("groups")}
        isCompact
      >
        {(filtered || groups).map((group: Group) => (
          <DataListItem
            aria-labelledby={group.name}
            key={group.id}
            id={group.id}
            onClick={(e) => {
              if ((e.target as HTMLInputElement).type !== "checkbox") {
                setGroupId(group.id);
              }
            }}
          >
            <DataListItemRow data-testid={group.name}>
              <DataListCheck
                data-testid={`${group.name}-check`}
                isChecked={group.checked}
                onChange={(checked, e) => {
                  group.checked = (e.target as HTMLInputElement).checked;
                  let newSelectedRows: Group[];
                  if (!group.checked) {
                    newSelectedRows = selectedRows.filter(
                      (r) => r.id !== group.id
                    );
                  } else if (group.checked) {
                    newSelectedRows = [...selectedRows, group];
                  }

                  setSelectedRows(newSelectedRows!);
                }}
                aria-labelledby="data-list-check"
              />

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
                <Button isDisabled variant="link">
                  <AngleRightIcon />
                </Button>
              </DataListAction>
            </DataListItemRow>
          </DataListItem>
        ))}
      </DataList>
    </Modal>
  );
};
