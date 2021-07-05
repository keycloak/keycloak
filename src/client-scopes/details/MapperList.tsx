import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useHistory } from "react-router-dom";
import {
  AlertVariant,
  ButtonVariant,
  Dropdown,
  DropdownItem,
  DropdownToggle,
} from "@patternfly/react-core";
import { CaretDownIcon } from "@patternfly/react-icons";

import type ClientScopeRepresentation from "keycloak-admin/lib/defs/clientScopeRepresentation";
import type ProtocolMapperRepresentation from "keycloak-admin/lib/defs/protocolMapperRepresentation";
import type { ProtocolMapperTypeRepresentation } from "keycloak-admin/lib/defs/serverInfoRepesentation";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";

import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { useAlerts } from "../../components/alert/Alerts";
import { AddMapperDialog } from "../add/MapperDialog";
import { useAdminClient } from "../../context/auth/AdminClient";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import { useRealm } from "../../context/realm-context/RealmContext";

type MapperListProps = {
  clientScope: ClientScopeRepresentation;
  refresh: () => void;
};

type Row = ProtocolMapperRepresentation & {
  category: string;
  type: string;
  priority: number;
};

export const MapperList = ({ clientScope, refresh }: MapperListProps) => {
  const { t } = useTranslation("client-scopes");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

  const history = useHistory();
  const { realm } = useRealm();
  const url = `/${realm}/client-scopes/${clientScope.id}/mappers`;

  const [mapperAction, setMapperAction] = useState(false);
  const mapperList = clientScope.protocolMappers!;
  const mapperTypes =
    useServerInfo().protocolMapperTypes![clientScope.protocol!];

  const [key, setKey] = useState(0);
  useEffect(() => setKey(new Date().getTime()), [mapperList]);

  const [addMapperDialogOpen, setAddMapperDialogOpen] = useState(false);
  const [filter, setFilter] = useState(clientScope.protocolMappers);
  const toggleAddMapperDialog = (buildIn: boolean) => {
    if (buildIn) {
      setFilter(mapperList || []);
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
        addAlert(t("common:mappingCreatedSuccess"), AlertVariant.success);
      } catch (error) {
        addAlert(
          t("common:mappingCreatedError", { error }),
          AlertVariant.danger
        );
      }
    }
  };

  const loader = async () =>
    Promise.resolve(
      (mapperList || [])
        .map((mapper) => {
          const mapperType = mapperTypes.filter(
            (type) => type.id === mapper.protocolMapper
          )[0];
          return {
            ...mapper,
            category: mapperType.category,
            type: mapperType.name,
            priority: mapperType.priority,
          } as Row;
        })
        .sort((a, b) => a.priority - b.priority)
    );

  const MapperLink = (mapper: Row) => (
    <>
      <Link to={`${url}/${mapper.id}`}>{mapper.name}</Link>
    </>
  );

  return (
    <>
      <AddMapperDialog
        protocol={clientScope.protocol!}
        filter={filter}
        onConfirm={addMappers}
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
            onRowClick: async (mapper) => {
              try {
                await adminClient.clientScopes.delProtocolMapper({
                  id: clientScope.id!,
                  mapperId: mapper.id!,
                });
                addAlert(
                  t("common:mappingDeletedSuccess"),
                  AlertVariant.success
                );
                refresh();
              } catch (error) {
                addAlert(
                  t("common:mappingDeletedError", { error }),
                  AlertVariant.danger
                );
              }
              return true;
            },
          },
        ]}
        columns={[
          {
            name: "name",
            cellRenderer: MapperLink,
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
