import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import type { Path } from "react-router-dom";
import { Link } from "react-router-dom";
import { Dropdown, DropdownItem, DropdownToggle } from "@patternfly/react-core";
import { CaretDownIcon } from "@patternfly/react-icons";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import type ProtocolMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/protocolMapperRepresentation";
import type { ProtocolMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";

import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { AddMapperDialog } from "../add/MapperDialog";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";

type MapperListProps = {
  model: ClientScopeRepresentation | ClientRepresentation;
  onAdd: (
    mappers: ProtocolMapperTypeRepresentation | ProtocolMapperRepresentation[]
  ) => void;
  onDelete: (mapper: ProtocolMapperRepresentation) => void;
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
  const { t } = useTranslation("client-scopes");

  const [mapperAction, setMapperAction] = useState(false);
  const mapperList = model.protocolMappers;
  const mapperTypes = useServerInfo().protocolMapperTypes![model.protocol!];

  const [key, setKey] = useState(0);
  useEffect(() => setKey(key + 1), [mapperList]);

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

  const loader = async () => {
    if (!mapperList) {
      return [];
    }

    const list = mapperList.reduce<Row[]>((rows, mapper) => {
      const mapperType = mapperTypes.find(
        ({ id }) => id === mapper.protocolMapper
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
        key={key}
        loader={loader}
        ariaLabelKey="client-scopes:clientScopeList"
        searchPlaceholderKey="common:searchForMapper"
        toolbarItem={
          <Dropdown
            onSelect={() => setMapperAction(false)}
            toggle={
              <DropdownToggle
                isPrimary
                id="mapperAction"
                onToggle={() => setMapperAction(!mapperAction)}
                toggleIndicator={CaretDownIcon}
              >
                {t("common:addMapper")}
              </DropdownToggle>
            }
            isOpen={mapperAction}
            dropdownItems={[
              <DropdownItem
                key="predefined"
                onClick={() => toggleAddMapperDialog(true)}
              >
                {t("fromPredefinedMapper")}
              </DropdownItem>,
              <DropdownItem
                key="byConfiguration"
                onClick={() => toggleAddMapperDialog(false)}
              >
                {t("byConfiguration")}
              </DropdownItem>,
            ]}
          />
        }
        actions={[
          {
            title: t("common:delete"),
            onRowClick: onDelete,
          },
        ]}
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
            message={t("common:emptyMappers")}
            instructions={t("common:emptyMappersInstructions")}
            secondaryActions={[
              {
                text: t("common:emptyPrimaryAction"),
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
