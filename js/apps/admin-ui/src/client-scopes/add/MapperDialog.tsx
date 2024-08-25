import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
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

import type ProtocolMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/protocolMapperRepresentation";
import type { ProtocolMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";

import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import useLocaleSort, { mapByKey } from "../../utils/useLocaleSort";

type Row = {
  id: string;
  description: string;
  item: ProtocolMapperRepresentation;
};

export type AddMapperDialogModalProps = {
  protocol: string;
  filter?: ProtocolMapperRepresentation[];
  onConfirm: (
    value: ProtocolMapperTypeRepresentation | ProtocolMapperRepresentation[],
  ) => void;
};

export type AddMapperDialogProps = AddMapperDialogModalProps & {
  open: boolean;
  toggleDialog: () => void;
};

export const AddMapperDialog = (props: AddMapperDialogProps) => {
  const { t } = useTranslation();

  const serverInfo = useServerInfo();
  const protocol = props.protocol;
  const protocolMappers = serverInfo.protocolMapperTypes![protocol];
  const builtInMappers = serverInfo.builtinProtocolMappers![protocol];
  const [filter, setFilter] = useState<ProtocolMapperRepresentation[]>([]);
  const [selectedRows, setSelectedRows] = useState<Row[]>([]);
  const localeSort = useLocaleSort();

  const allRows = useMemo(
    () =>
      localeSort(builtInMappers, mapByKey("name")).map((mapper) => {
        const mapperType = protocolMappers.find(
          (type) => type.id === mapper.protocolMapper,
        )!;
        return {
          item: mapper,
          id: mapper.name!,
          description: mapperType.helpText,
        };
      }),
    [builtInMappers, protocolMappers],
  );
  const [rows, setRows] = useState(allRows);

  if (props.filter && props.filter.length !== filter.length) {
    setFilter(props.filter);
    const nameFilter = props.filter.map((f) => f.name);
    setRows([...allRows.filter((row) => !nameFilter.includes(row.item.name))]);
  }

  const sortedProtocolMappers = useMemo(
    () => localeSort(protocolMappers, mapByKey("name")),
    [protocolMappers],
  );

  const isBuiltIn = !!props.filter;

  const header = [t("name"), t("description")];

  return (
    <Modal
      aria-label={
        isBuiltIn ? t("addPredefinedMappers") : t("emptySecondaryAction")
      }
      variant={ModalVariant.medium}
      header={
        <TextContent
          role="dialog"
          aria-label={
            isBuiltIn ? t("addPredefinedMappers") : t("emptySecondaryAction")
          }
        >
          <Text component={TextVariants.h1}>
            {isBuiltIn ? t("addPredefinedMappers") : t("emptySecondaryAction")}
          </Text>
          <Text>
            {isBuiltIn
              ? t("predefinedMappingDescription")
              : t("configureMappingDescription")}
          </Text>
        </TextContent>
      }
      isOpen={props.open}
      onClose={props.toggleDialog}
      actions={
        isBuiltIn
          ? [
              <Button
                id="modal-confirm"
                data-testid="confirm"
                key="confirm"
                isDisabled={rows.length === 0 || selectedRows.length === 0}
                onClick={() => {
                  props.onConfirm(selectedRows.map(({ item }) => item));
                  props.toggleDialog();
                }}
              >
                {t("add")}
              </Button>,
              <Button
                id="modal-cancel"
                data-testid="cancel"
                key="cancel"
                variant={ButtonVariant.link}
                onClick={() => {
                  props.toggleDialog();
                }}
              >
                {t("cancel")}
              </Button>,
            ]
          : []
      }
    >
      {!isBuiltIn && (
        <DataList
          onSelectDataListItem={(_event, id) => {
            const mapper = protocolMappers.find((mapper) => mapper.id === id);
            props.onConfirm(mapper!);
            props.toggleDialog();
          }}
          aria-label={t("addPredefinedMappers")}
          isCompact
        >
          <DataListItem aria-label={t("headerName")} id="header">
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
      {isBuiltIn && (
        <KeycloakDataTable
          loader={rows}
          onSelect={setSelectedRows}
          canSelectAll
          ariaLabelKey="addPredefinedMappers"
          searchPlaceholderKey="searchForMapper"
          columns={[
            {
              name: "id",
              displayKey: "name",
            },
            {
              name: "description",
              displayKey: "description",
            },
          ]}
          emptyState={
            <ListEmptyState
              message={t("emptyMappers")}
              instructions={t("emptyBuiltInMappersInstructions")}
            />
          }
        />
      )}
    </Modal>
  );
};
