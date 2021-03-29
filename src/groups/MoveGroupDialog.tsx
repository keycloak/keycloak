import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useErrorHandler } from "react-error-boundary";
import {
  Breadcrumb,
  BreadcrumbItem,
  Button,
  ButtonVariant,
  DataList,
  DataListAction,
  DataListCell,
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
import { AngleRightIcon, SearchIcon } from "@patternfly/react-icons";

import GroupRepresentation from "keycloak-admin/lib/defs/groupRepresentation";
import { asyncStateFetch, useAdminClient } from "../context/auth/AdminClient";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";

type MoveGroupDialogProps = {
  group: GroupRepresentation;
  onClose: () => void;
  onMove: (groupId: string) => void;
};

export const MoveGroupDialog = ({
  group,
  onClose,
  onMove,
}: MoveGroupDialogProps) => {
  const { t } = useTranslation("groups");

  const adminClient = useAdminClient();
  const errorHandler = useErrorHandler();

  const [navigation, setNavigation] = useState<GroupRepresentation[]>([]);
  const [groups, setGroups] = useState<GroupRepresentation[]>([]);
  const [filtered, setFiltered] = useState<GroupRepresentation[]>();
  const [filter, setFilter] = useState("");

  const [id, setId] = useState<string>();
  const currentGroup = () => navigation[navigation.length - 1];

  useEffect(
    () =>
      asyncStateFetch(
        async () => {
          if (id) {
            const group = await adminClient.groups.findOne({ id });
            return { group, groups: group.subGroups! };
          } else {
            return { groups: await adminClient.groups.find() };
          }
        },
        ({ group: selectedGroup, groups }) => {
          if (selectedGroup) setNavigation([...navigation, selectedGroup]);
          setGroups(groups.filter((g) => g.id !== group.id));
        },
        errorHandler
      ),
    [id]
  );

  return (
    <Modal
      variant={ModalVariant.large}
      title={
        currentGroup()
          ? t("moveToGroup", {
              group1: group.name,
              group2: currentGroup().name,
            })
          : t("moveTo")
      }
      isOpen={true}
      onClose={onClose}
      actions={[
        <Button
          data-testid="moveGroup"
          key="confirm"
          variant="primary"
          form="group-form"
          onClick={() => onMove(currentGroup().id!)}
        >
          {t("moveHere")}
        </Button>,
        <Button
          data-testid="moveCancel"
          key="cancel"
          variant="secondary"
          onClick={onClose}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Breadcrumb>
        <BreadcrumbItem key="home">
          <Button
            variant="link"
            onClick={() => {
              setId(undefined);
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
                  setId(group.id);
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
                placeholder={t("searchForGroups")}
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
        onSelectDataListItem={(value) => setId(value)}
        aria-label={t("groups")}
        isCompact
      >
        {(filtered || groups).map((group) => (
          <DataListItem
            aria-labelledby={group.name}
            key={group.id}
            id={group.id}
          >
            <DataListItemRow data-testid={group.name}>
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
        {(filtered || groups).length === 0 && (
          <ListEmptyState
            hasIcon={false}
            message={t("moveGroupEmpty")}
            instructions={t("moveGroupEmptyInstructions")}
          />
        )}
      </DataList>
    </Modal>
  );
};
