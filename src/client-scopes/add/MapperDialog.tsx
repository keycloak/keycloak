import React, { useMemo, useState } from "react";
import {
  Button,
  ButtonVariant,
  DataList,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Modal,
  ModalVariant,
  Text,
  TextContent,
  TextVariants,
} from "@patternfly/react-core";
import {
  Table,
  TableBody,
  TableHeader,
  TableVariant,
} from "@patternfly/react-table";
import { useTranslation } from "react-i18next";
import type ProtocolMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/protocolMapperRepresentation";
import type { ProtocolMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";

import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";

export type AddMapperDialogModalProps = {
  protocol: string;
  filter?: ProtocolMapperRepresentation[];
  onConfirm: (
    value: ProtocolMapperTypeRepresentation | ProtocolMapperRepresentation[]
  ) => void;
};

export type AddMapperDialogProps = AddMapperDialogModalProps & {
  open: boolean;
  toggleDialog: () => void;
};

export const AddMapperDialog = (props: AddMapperDialogProps) => {
  const { t } = useTranslation("client-scopes");

  const serverInfo = useServerInfo();
  const { whoAmI } = useWhoAmI();
  const protocol = props.protocol;
  const protocolMappers = serverInfo.protocolMapperTypes![protocol];
  const builtInMappers = serverInfo.builtinProtocolMappers![protocol];
  const [filter, setFilter] = useState<ProtocolMapperRepresentation[]>([]);

  const allRows = useMemo(
    () =>
      builtInMappers
        .sort((a, b) => a.name!.localeCompare(b.name!, whoAmI.getLocale()))
        .map((mapper) => {
          const mapperType = protocolMappers.filter(
            (type) => type.id === mapper.protocolMapper
          )[0];
          return {
            item: mapper,
            selected: false,
            cells: [mapper.name, mapperType.helpText],
          };
        }),
    []
  );
  const [rows, setRows] = useState(allRows);

  if (props.filter && props.filter.length !== filter.length) {
    setFilter(props.filter);
    const nameFilter = props.filter.map((f) => f.name);
    setRows([...allRows.filter((row) => !nameFilter.includes(row.item.name))]);
  }

  const selectedRows = rows
    .filter((row) => row.selected)
    .map((row) => row.item);

  const sortedProtocolMappers = useMemo(
    () =>
      protocolMappers.sort((a, b) =>
        a.name!.localeCompare(b.name!, whoAmI.getLocale())
      ),
    [protocolMappers]
  );

  const isBuiltIn = !!props.filter;

  const header = [t("common:name"), t("common:description")];

  return (
    <Modal
      aria-labelledby={t("addPredefinedMappers")}
      variant={ModalVariant.medium}
      header={
        <TextContent>
          <Text component={TextVariants.h1}>{t("addPredefinedMappers")}</Text>
          <Text>{t("predefinedMappingDescription")}</Text>
        </TextContent>
      }
      isOpen={props.open}
      onClose={props.toggleDialog}
      actions={
        isBuiltIn
          ? [
              <Button
                id="modal-confirm"
                data-testid="modalConfirm"
                key="confirm"
                isDisabled={rows.length === 0 || selectedRows.length === 0}
                onClick={() => {
                  props.onConfirm(selectedRows);
                  props.toggleDialog();
                }}
              >
                {t("common:add")}
              </Button>,
              <Button
                id="modal-cancel"
                key="cancel"
                variant={ButtonVariant.link}
                onClick={() => {
                  props.toggleDialog();
                }}
              >
                {t("common:cancel")}
              </Button>,
            ]
          : []
      }
    >
      {!isBuiltIn && (
        <DataList
          onSelectDataListItem={(id) => {
            const mapper = protocolMappers.find((mapper) => mapper.id === id);
            props.onConfirm(mapper!);
            props.toggleDialog();
          }}
          aria-label={t("addPredefinedMappers")}
          isCompact
        >
          <DataListItem aria-labelledby="headerName" id="header">
            <DataListItemRow>
              <DataListItemCells
                dataListCells={header.map((name) => (
                  <DataListCell style={{ fontWeight: 700 }} key={name}>
                    {name}
                  </DataListCell>
                ))}
              />
            </DataListItemRow>
          </DataListItem>
          {sortedProtocolMappers.map((mapper) => (
            <DataListItem
              aria-label={mapper.name}
              key={mapper.id}
              id={mapper.id}
            >
              <DataListItemRow>
                <DataListItemCells
                  dataListCells={[
                    <DataListCell key={`name-${mapper.id}`}>
                      {mapper.name}
                    </DataListCell>,
                    <DataListCell key={`helpText-${mapper.id}`}>
                      {mapper.helpText}
                    </DataListCell>,
                  ]}
                />
              </DataListItemRow>
            </DataListItem>
          ))}
        </DataList>
      )}
      {isBuiltIn && rows.length > 0 && (
        <Table
          variant={TableVariant.compact}
          cells={header}
          onSelect={(_, isSelected, rowIndex) => {
            if (rowIndex === -1) {
              setRows(
                rows.map((row) => ({
                  ...row,
                  selected: isSelected,
                }))
              );
            } else {
              rows[rowIndex].selected = isSelected;
              setRows([...rows]);
            }
          }}
          canSelectAll
          rows={rows}
          aria-label={t("addPredefinedMappers")}
        >
          <TableHeader />
          <TableBody />
        </Table>
      )}
      {isBuiltIn && rows.length === 0 && (
        <ListEmptyState
          message={t("common:emptyMappers")}
          instructions={t("client-scopes:emptyBuiltInMappersInstructions")}
        />
      )}
    </Modal>
  );
};
