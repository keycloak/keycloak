import React, { useContext, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
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

import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import {
  ClientScopeRepresentation,
  ProtocolMapperRepresentation,
} from "../models/client-scope";
import { TableToolbar } from "../../components/table-toolbar/TableToolbar";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { HttpClientContext } from "../../context/http-service/HttpClientContext";
import { RealmContext } from "../../context/realm-context/RealmContext";
import { useAlerts } from "../../components/alert/Alerts";

type MapperListProps = {
  clientScope: ClientScopeRepresentation;
};

type Row = {
  name: string;
  category: string;
  type: string;
  priority: number;
};

export const MapperList = ({ clientScope }: MapperListProps) => {
  const { t } = useTranslation("client-scopes");
  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);
  const { addAlert } = useAlerts();

  const [filteredData, setFilteredData] = useState<
    { mapper: ProtocolMapperRepresentation; cells: Row }[]
  >();
  const [mapperAction, setMapperAction] = useState(false);
  const mapperList = clientScope.protocolMappers!;
  const mapperTypes = useServerInfo().protocolMapperTypes[
    clientScope.protocol!
  ];

  if (!mapperList) {
    return (
      <ListEmptyState
        message={t("emptyMappers")}
        instructions={t("emptyMappersInstructions")}
        primaryActionText={t("emptyPrimaryAction")}
        onPrimaryAction={() => {}}
      />
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
          name: mapper.name,
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
        column.cells.name.toLowerCase().includes(search.toLowerCase())
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
            <DropdownItem key="predefined">
              {t("fromPredefinedMapper")}
            </DropdownItem>,
            <DropdownItem key="byConfiguration">
              {t("byConfiguration")}
            </DropdownItem>,
          ]}
        />
      }
    >
      <Table
        variant={TableVariant.compact}
        cells={[t("name"), t("category"), t("type"), t("priority")]}
        rows={(filteredData || data).map((cell) => {
          return { cells: Object.values(cell.cells), mapper: cell.mapper };
        })}
        aria-label={t("clientScopeList")}
        actions={[
          {
            title: t("common:delete"),
            onClick: async (_, rowId) => {
              try {
                await httpClient.doDelete(
                  `/admin/realms/${realm}/client-scopes/${clientScope.id}/protocol-mappers/models/${data[rowId].mapper.id}`
                );
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
