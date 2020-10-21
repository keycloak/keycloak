import React, { ReactElement, useState } from "react";
import {
  Button,
  ButtonVariant,
  DataList,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Modal,
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

import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import {
  ProtocolMapperRepresentation,
  ProtocolMapperTypeRepresentation,
} from "../../context/server-info/server-info";

export type AddMapperDialogProps = {
  protocol: string;
  buildIn: boolean;
  onConfirm: (
    value: ProtocolMapperTypeRepresentation | ProtocolMapperRepresentation[]
  ) => {};
};

type AddMapperDialogModalProps = AddMapperDialogProps & {
  open: boolean;
  toggleDialog: () => void;
};

export const useAddMapperDialog = (
  props: AddMapperDialogProps
): [() => void, () => ReactElement] => {
  const [show, setShow] = useState(false);

  function toggleDialog() {
    setShow((show) => !show);
  }

  const Dialog = () => (
    <AddMapperDialog {...props} open={show} toggleDialog={toggleDialog} />
  );
  return [toggleDialog, Dialog];
};

export const AddMapperDialog = ({
  protocol,
  buildIn,
  open,
  toggleDialog,
  onConfirm,
}: AddMapperDialogModalProps) => {
  const serverInfo = useServerInfo();
  const protocolMappers = serverInfo.protocolMapperTypes[protocol];
  const buildInMappers = serverInfo.builtinProtocolMappers[protocol];
  const { t } = useTranslation("client-scopes");
  const [rows, setRows] = useState(
    buildInMappers.map((mapper) => {
      const mapperType = protocolMappers.filter(
        (type) => type.id === mapper.protocolMapper
      )[0];
      return {
        item: mapper,
        selected: false,
        cells: [mapper.name, mapperType.helpText],
      };
    })
  );

  return (
    <Modal
      title={t("chooseAMapperType")}
      isOpen={open}
      onClose={toggleDialog}
      actions={
        buildIn
          ? [
              <Button
                id="modal-confirm"
                key="confirm"
                onClick={() => {
                  onConfirm(
                    rows.filter((row) => row.selected).map((row) => row.item)
                  );
                  toggleDialog();
                }}
              >
                {t("common:add")}
              </Button>,
              <Button
                id="modal-cancel"
                key="cancel"
                variant={ButtonVariant.secondary}
                onClick={() => {
                  toggleDialog();
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
      {!buildIn && (
        <DataList
          onSelectDataListItem={(id) => {
            const mapper = protocolMappers.find((mapper) => mapper.id === id);
            onConfirm(mapper!);
            toggleDialog();
          }}
          aria-label={t("chooseAMapperType")}
          isCompact
        >
          {protocolMappers.map((mapper) => (
            <DataListItem
              aria-labelledby={mapper.name}
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
      {buildIn && (
        <Table
          variant={TableVariant.compact}
          cells={[t("name"), t("description")]}
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
    </Modal>
  );
};
