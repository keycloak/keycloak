import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import type { Path } from "react-router-dom";
import { Link } from "react-router-dom";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import type ProtocolMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/protocolMapperRepresentation";
import type { ProtocolMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";

import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { AddMapperDialog } from "../add/MapperDialog";
import { Action, KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import {
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
} from "@patternfly/react-core";

type MapperListProps = {
  model: ClientScopeRepresentation | ClientRepresentation;
  onAdd: (
    mappers: ProtocolMapperTypeRepresentation | ProtocolMapperRepresentation[],
  ) => void;
  onDelete: (mapper: ProtocolMapperRepresentation) => Promise<boolean>;
  detailLink: (id: string) => Partial<Path>;
};

type Row = ProtocolMapperRepresentation & {
  category: string;
  type: string;
  priority: number;
};

type MapperLinkProps = Row & {
  detailLink: (id: string) => Partial<Path>;
};

const MapperLink = ({ id, name, detailLink }: MapperLinkProps) => (
  <Link to={detailLink(id!)}>{name}</Link>
);

export const MapperList = ({
  model,
  onAdd,
  onDelete,
  detailLink,
}: MapperListProps) => {
  const { t } = useTranslation();

  const [mapperAction, setMapperAction] = useState(false);
  const mapperList = model.protocolMappers;
  const mapperTypes = useServerInfo().protocolMapperTypes![model.protocol!];
  const [deletingMapperId, setDeletingMapperId] = useState<string>();

  const rows = useMemo<Row[]>(() => {
    if (!mapperList) {
      return [];
    }

    const list = mapperList.reduce<Row[]>((rows, mapper) => {
      const mapperType = mapperTypes.find(
        ({ id }) => id === mapper.protocolMapper,
      );

      if (!mapperType) {
        return rows;
      }

      return rows.concat({
        ...mapper,
        category: mapperType.category,
        type: mapperType.name,
        priority: mapperType.priority,
      });
    }, []);

    return list.sort((a, b) => a.priority - b.priority);
  }, [mapperList, mapperTypes, deletingMapperId]);

  const [addMapperDialogOpen, setAddMapperDialogOpen] = useState(false);
  const [filter, setFilter] = useState(model.protocolMappers);
  const toggleAddMapperDialog = (buildIn: boolean) => {
    if (buildIn) {
      setFilter(mapperList || []);
    } else {
      setFilter(undefined);
    }
    setAddMapperDialogOpen(!addMapperDialogOpen);
  };

  const onDeleteMapper = async (mapper: Row) => {
    if (!mapper.id || deletingMapperId) {
      return false;
    }

    setDeletingMapperId(mapper.id);
    try {
      return await onDelete(mapper);
    } finally {
      setDeletingMapperId(undefined);
    }
  };

  return (
    <>
      <AddMapperDialog
        protocol={model.protocol!}
        filter={filter}
        onConfirm={onAdd}
        open={addMapperDialogOpen}
        toggleDialog={() => setAddMapperDialogOpen(!addMapperDialogOpen)}
      />

      <KeycloakDataTable
        loader={rows}
        ariaLabelKey="clientScopeList"
        searchPlaceholderKey="searchForMapper"
        toolbarItem={
          <Dropdown
            onSelect={() => setMapperAction(false)}
            onOpenChange={(isOpen) => setMapperAction(isOpen)}
            toggle={(ref) => (
              <MenuToggle
                ref={ref}
                variant="primary"
                id="mapperAction"
                onClick={() => setMapperAction(!mapperAction)}
                isDisabled={!!deletingMapperId}
              >
                {t("addMapper")}
              </MenuToggle>
            )}
            isOpen={mapperAction}
          >
            <DropdownList>
              <DropdownItem onClick={() => toggleAddMapperDialog(true)}>
                {t("fromPredefinedMapper")}
              </DropdownItem>
              <DropdownItem onClick={() => toggleAddMapperDialog(false)}>
                {t("byConfiguration")}
              </DropdownItem>
            </DropdownList>
          </Dropdown>
        }
        actions={[
          {
            title: t("delete"),
            onRowClick: onDeleteMapper,
          } as Action<Row>,
        ]}
        isRowDisabled={(row) => row.id === deletingMapperId}
        columns={[
          {
            name: "name",
            cellRenderer: (row) => (
              <MapperLink {...row} detailLink={detailLink} />
            ),
          },
          { name: "category" },
          {
            name: "type",
          },
          {
            name: "priority",
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("emptyMappers")}
            instructions={t("emptyMappersInstructions")}
            secondaryActions={[
              {
                text: t("emptyPrimaryAction"),
                onClick: () => toggleAddMapperDialog(true),
              },
              {
                text: t("emptySecondaryAction"),
                onClick: () => toggleAddMapperDialog(false),
              },
            ]}
          />
        }
      />
    </>
  );
};
