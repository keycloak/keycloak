import React, { useState } from "react";
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
  const protocol = props.protocol;
  const protocolMappers = serverInfo.protocolMapperTypes![protocol];
  const builtInMappers = serverInfo.builtinProtocolMappers![protocol];
  const [filter, setFilter] = useState<ProtocolMapperRepresentation[]>([]);

  const allRows = builtInMappers.map((mapper) => {
    const mapperType = protocolMappers.filter(
      (type) => type.id === mapper.protocolMapper
    )[0];
    return {
      item: mapper,
      selected: false,
      cells: [mapper.name, mapperType.helpText],
    };
  });
  const [rows, setRows] = useState(allRows);

  if (props.filter && props.filter.length !== filter.length) {
    setFilter(props.filter);
    const nameFilter = props.filter.map((f) => f.name);
    setRows([...allRows.filter((row) => !nameFilter.includes(row.item.name))]);
  }

  const selectedRows = rows
    .filter((row) => row.selected)
    .map((row) => row.item);
  const isBuiltIn = !!props.filter;

  return (
    <Modal
      variant={ModalVariant.medium}
      title={t("chooseAMapperType")}
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
      <TextContent>
        <Text>{t("predefinedMappingDescription")}</Text>
      </TextContent>
      {!isBuiltIn && (
        <DataList
          onSelectDataListItem={(id) => {
            const mapper = protocolMappers.find((mapper) => mapper.id === id);
            props.onConfirm(mapper!);
            props.toggleDialog();
          }}
          aria-label={t("chooseAMapperType")}
          isCompact
        >
          {protocolMappers.map((mapper) => (
            <DataListItem
              aria-label={mapper.name}
              key={mapper.id}
              id={mapper.id}
            >
              <DataListItemRow>
                <DataListItemCells
                  dataListCells={[
                    <DataListCell key={`name-${mapper.id}`}>
                      <>{mapper.name}</>
                    </DataListCell>,
                    <DataListCell key={`helpText-${mapper.id}`}>
                      <>{mapper.helpText}</>
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
          cells={[t("common:name"), t("common:description")]}
          onSelect={(_, isSelected, rowIndex) => {
            rows[rowIndex].selected = isSelected;
            setRows([...rows]);
          }}
          canSelectAll={false}
          rows={rows}
          aria-label={t("chooseAMapperType")}
        >
          <TableHeader />
          <TableBody />
        </Table>
      )}
      {isBuiltIn && rows.length === 0 && (
        <ListEmptyState
          message={t("common:emptyMappers")}
          instructions={t("common:emptyBuiltInMappersInstructions")}
        />
      )}
    </Modal>
  );
};
