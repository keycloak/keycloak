import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  ButtonVariant,
  Dropdown,
  DropdownToggle,
  Modal,
  ModalVariant,
  DropdownDirection,
} from "@patternfly/react-core";
import { CaretUpIcon } from "@patternfly/react-icons";
import {
  Table,
  TableBody,
  TableHeader,
  TableVariant,
} from "@patternfly/react-table";
import ClientScopeRepresentation from "keycloak-admin/lib/defs/clientScopeRepresentation";

import { ClientScopeType, clientScopeTypesDropdown } from "./ClientScopeTypes";

export type AddScopeDialogProps = {
  clientScopes: ClientScopeRepresentation[];
  open: boolean;
  toggleDialog: () => void;
  onAdd: (
    scopes: { scope: ClientScopeRepresentation; type: ClientScopeType }[]
  ) => void;
};

type Row = {
  selected: boolean;
  scope: ClientScopeRepresentation;
  cells: (string | undefined)[];
};

export const AddScopeDialog = ({
  clientScopes,
  open,
  toggleDialog,
  onAdd,
}: AddScopeDialogProps) => {
  const { t } = useTranslation("clients");
  const [addToggle, setAddToggle] = useState(false);
  const [rows, setRows] = useState<Row[]>([]);

  useEffect(() => {
    setRows(
      clientScopes.map((scope) => {
        return {
          selected: false,
          scope,
          cells: [scope.name, scope.description],
        };
      })
    );
  }, [clientScopes]);

  const action = (scope: ClientScopeType) => {
    const scopes = rows
      .filter((row) => row.selected)
      .map((row) => {
        return { scope: row.scope, type: scope };
      });
    onAdd(scopes);
    setAddToggle(false);
    toggleDialog();
  };

  return (
    <Modal
      variant={ModalVariant.medium}
      title={t("addClientScopesTo", { clientId: "test" })}
      isOpen={open}
      onClose={toggleDialog}
      actions={[
        <Dropdown
          id="add-dropdown"
          key="add-dropdown"
          direction={DropdownDirection.up}
          isOpen={addToggle}
          toggle={
            <DropdownToggle
              onToggle={() => setAddToggle(!addToggle)}
              isPrimary
              toggleIndicator={CaretUpIcon}
              id="add-scope-toggle"
            >
              {t("common:add")}
            </DropdownToggle>
          }
          dropdownItems={clientScopeTypesDropdown(t, action)}
        />,
        <Button
          id="modal-cancel"
          key="cancel"
          variant={ButtonVariant.secondary}
          onClick={() => {
            setRows(
              rows.map((row) => {
                row.selected = false;
                return row;
              })
            );
            toggleDialog();
          }}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Table
        variant={TableVariant.compact}
        cells={[t("common:name"), t("common:description")]}
        onSelect={(_, isSelected, rowIndex) => {
          if (rowIndex === -1) {
            setRows(
              rows.map((row) => {
                row.selected = isSelected;
                return row;
              })
            );
          } else {
            rows[rowIndex].selected = isSelected;
            setRows([...rows]);
          }
        }}
        rows={rows}
        aria-label={t("chooseAMapperType")}
      >
        <TableHeader />
        <TableBody />
      </Table>
    </Modal>
  );
};
