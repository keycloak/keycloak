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
import ProtocolMapperRepresentation from "keycloak-admin/lib/defs/protocolMapperRepresentation";
import { ProtocolMapperTypeRepresentation } from "keycloak-admin/lib/defs/serverInfoRepesentation";

import { useServerInfo } from "../../../context/server-info/ServerInfoProvider";
import { ListEmptyState } from "../../../components/list-empty-state/ListEmptyState";

// export type AddLdapMapperDialogModalProps = {
//   protocol: string;
//   filter?: ProtocolMapperRepresentation[];
//   onConfirm: (
//     value: ProtocolMapperTypeRepresentation | ProtocolMapperRepresentation[]
//   ) => void;
// };

export type AddLdapMapperDialogProps = {
  open: boolean;
  toggleDialog: () => void;
};

export const AddLdapMapperDialog = (props: AddLdapMapperDialogProps) => {
  const { t } = useTranslation("client-scopes");

  const serverInfo = useServerInfo();
  // const protocol = props.protocol;
  // const protocolMappers = serverInfo.protocolMapperTypes![protocol];
  // const builtInMappers = serverInfo.builtinProtocolMappers![protocol];
  // const [filter, setFilter] = useState<ProtocolMapperRepresentation[]>([]);

  // const allRows = builtInMappers.map((mapper) => {
  //   const mapperType = protocolMappers.filter(
  //     (type) => type.id === mapper.protocolMapper
  //   )[0];
  //   return {
  //     item: mapper,
  //     selected: false,
  //     cells: [mapper.name, mapperType.helpText],
  //   };
  // });
  const [rows, setRows] = useState(allRows);

  // if (props.filter && props.filter.length !== filter.length) {
  //   setFilter(props.filter);
  //   const nameFilter = props.filter.map((f) => f.name);
  //   setRows([...allRows.filter((row) => !nameFilter.includes(row.item.name))]);
  // }

  // const selectedRows = rows
  //   .filter((row) => row.selected)
  //   .map((row) => row.item);
  
  //const isBuiltIn = !!props.filter;
  const isBuiltIn = true;

  const mapperList = [
    {
    "id":"699b72e5-b936-41b9-98fc-5d5b3ec5ea6f",
    "name":"username",
    "providerId":"user-attribute-ldap-mapper",
    "providerType":"org.keycloak.storage.ldap.mappers.LDAPStorageMapper",
    "parentId":"f1da61f9-08f7-4dc5-83dd-d774fba518d4",
    "config":
        {
        "ldap.attribute":["cn"],
        "is.mandatory.in.ldap":["true"],
        "always.read.value.from.ldap":["false"],
        "read.only":["true"],
        "user.model.attribute":["username"]
        }
    },
    {
    "id":"c11788d2-62be-442f-813f-4b00382a1e10",
    "name":"last name",
    "providerId":"user-attribute-ldap-mapper",
    "providerType":"org.keycloak.storage.ldap.mappers.LDAPStorageMapper",
    "parentId":"f1da61f9-08f7-4dc5-83dd-d774fba518d4",
    "config":
        {
        "ldap.attribute":["sn"],
        "is.mandatory.in.ldap":["true"],
        "always.read.value.from.ldap":["true"],
        "read.only":["true"],
        "user.model.attribute":["lastName"]
        }
    }
  ]

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
                key="confirm"
                // isDisabled={rows.length === 0 || selectedRows.length === 0}
                onClick={() => {
                  // props.onConfirm(selectedRows);
                  props.toggleDialog();
                }}
              >
                {t("common:add")}
              </Button>,
              <Button
                id="modal-cancel"
                key="cancel"
                variant={ButtonVariant.secondary}
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
            const mapper = mapperList.find((mapper) => mapper.id === id);
            // props.onConfirm(mapper!);
            props.toggleDialog();
          }}
          aria-label={t("chooseAMapperType")}
          isCompact
        >
          {(
            <DataListItem
              aria-labelledby={mapperList[0].name}
              key={mapperList[0].id}
              id={mapperList[0].id}
            >
              <DataListItemRow>
                <DataListItemCells
                  dataListCells={[
                    <DataListCell key={`name-${mapperList[0].id}`}>
                      <>{mapperList[0].name}</>
                    </DataListCell>,
                    <DataListCell key={`helpText-${mapperList[0].id}`}>
                      <>{mapperList[0].helpText}</>
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
          message={t("emptyMappers")}
          instructions={t("emptyBuiltInMappersInstructions")}
        />
      )}
    </Modal>
  );
};
