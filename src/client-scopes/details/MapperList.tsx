import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useHistory, useRouteMatch } from "react-router-dom";
import {
  AlertVariant,
  ButtonVariant,
  Dropdown,
  DropdownItem,
  DropdownToggle,
} from "@patternfly/react-core";
import {
  Table,
  TableBody,
  TableHeader,
  TableVariant,
} from "@patternfly/react-table";
import { CaretDownIcon } from "@patternfly/react-icons";

import ClientScopeRepresentation from "keycloak-admin/lib/defs/clientScopeRepresentation";
import ProtocolMapperRepresentation from "keycloak-admin/lib/defs/protocolMapperRepresentation";
import { ProtocolMapperTypeRepresentation } from "keycloak-admin/lib/defs/serverInfoRepesentation";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";

import { TableToolbar } from "../../components/table-toolbar/TableToolbar";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { useAlerts } from "../../components/alert/Alerts";
import { AddMapperDialog } from "../add/MapperDialog";
import { useAdminClient } from "../../context/auth/AdminClient";

type MapperListProps = {
  clientScope: ClientScopeRepresentation;
  refresh: () => void;
};

type Row = {
  name: JSX.Element;
  category: string;
  type: string;
  priority: number;
};

export const MapperList = ({ clientScope, refresh }: MapperListProps) => {
  const { t } = useTranslation("client-scopes");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();
  const history = useHistory();
  const { url } = useRouteMatch();

  const [filteredData, setFilteredData] = useState<
    { mapper: ProtocolMapperRepresentation; cells: Row }[]
  >();
  const [mapperAction, setMapperAction] = useState(false);
  const mapperList = clientScope.protocolMappers!;
  const mapperTypes = useServerInfo().protocolMapperTypes![
    clientScope.protocol!
  ];

  const [addMapperDialogOpen, setAddMapperDialogOpen] = useState(false);
  const [filter, setFilter] = useState(clientScope.protocolMappers);
  const toggleAddMapperDialog = (buildIn: boolean) => {
    if (buildIn) {
      setFilter(mapperList);
    } else {
      setFilter(undefined);
    }
    setAddMapperDialogOpen(!addMapperDialogOpen);
  };

  const addMappers = async (
    mappers: ProtocolMapperTypeRepresentation | ProtocolMapperRepresentation[]
  ): Promise<void> => {
    if (filter === undefined) {
      const mapper = mappers as ProtocolMapperTypeRepresentation;
      history.push(`${url}/${mapper.id}`);
    } else {
      try {
        await adminClient.clientScopes.addMultipleProtocolMappers(
          { id: clientScope.id! },
          mappers as ProtocolMapperRepresentation[]
        );
        refresh();
        addAlert(t("mappingCreatedSuccess"), AlertVariant.success);
      } catch (error) {
        addAlert(t("mappingCreatedError", { error }), AlertVariant.danger);
      }
    }
  };

  if (!mapperList) {
    return (
      <>
        <AddMapperDialog
          protocol={clientScope.protocol!}
          filter={filter}
          onConfirm={addMappers}
          open={addMapperDialogOpen}
          toggleDialog={() => setAddMapperDialogOpen(!addMapperDialogOpen)}
        />
        <ListEmptyState
          message={t("emptyMappers")}
          instructions={t("emptyMappersInstructions")}
          primaryActionText={t("emptyPrimaryAction")}
          onPrimaryAction={() => toggleAddMapperDialog(true)}
          secondaryActions={[
            {
              text: t("emptySecondaryAction"),
              onClick: () => toggleAddMapperDialog(false),
              type: ButtonVariant.secondary,
            },
          ]}
        />
      </>
    );
  }

  const data = mapperList
    .map((mapper) => {
      const mapperType = mapperTypes.filter(
        (type) => type.id === mapper.protocolMapper
      )[0];
      return {
        mapper,
        cells: {
          name: (
            <>
              <Link to={`${url}/${clientScope.id}/${mapper.id}`}>
                {mapper.name}
              </Link>
            </>
          ),
          category: mapperType.category,
          type: mapperType.name,
          priority: mapperType.priority,
        } as Row,
      };
    })
    .sort((a, b) => a.cells.priority - b.cells.priority);

  const filterData = (search: string) => {
    setFilteredData(
      data.filter((column) =>
        column.mapper.name!.toLowerCase().includes(search.toLowerCase())
      )
    );
  };

  return (
    <TableToolbar
      inputGroupName="clientsScopeToolbarTextInput"
      inputGroupPlaceholder={t("mappersSearchFor")}
      inputGroupOnChange={filterData}
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
              {t("addMapper")}
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
    >
      <AddMapperDialog
        protocol={clientScope.protocol!}
        filter={filter}
        onConfirm={addMappers}
        open={addMapperDialogOpen}
        toggleDialog={() => setAddMapperDialogOpen(!addMapperDialogOpen)}
      />
      <Table
        variant={TableVariant.compact}
        cells={[
          t("common:name"),
          t("common:category"),
          t("common:type"),
          t("common:priority"),
        ]}
        rows={(filteredData || data).map((cell) => {
          return { cells: Object.values(cell.cells), mapper: cell.mapper };
        })}
        aria-label={t("clientScopeList")}
        actions={[
          {
            title: t("common:delete"),
            onClick: async (_, rowId) => {
              try {
                await adminClient.clientScopes.delProtocolMapper({
                  id: clientScope.id!,
                  mapperId: data[rowId].mapper.id!,
                });
                refresh();
                addAlert(t("mappingDeletedSuccess"), AlertVariant.success);
              } catch (error) {
                addAlert(
                  t("mappingDeletedError", { error }),
                  AlertVariant.danger
                );
              }
            },
          },
        ]}
      >
        <TableHeader />
        <TableBody />
      </Table>
    </TableToolbar>
  );
};
