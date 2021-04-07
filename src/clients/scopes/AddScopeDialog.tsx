import React, { useState } from "react";
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
import ClientScopeRepresentation from "keycloak-admin/lib/defs/clientScopeRepresentation";

import {
  ClientScopeType,
  clientScopeTypesDropdown,
} from "../../components/client-scope/ClientScopeTypes";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";

export type AddScopeDialogProps = {
  clientScopes: ClientScopeRepresentation[];
  open: boolean;
  toggleDialog: () => void;
  onAdd: (
    scopes: { scope: ClientScopeRepresentation; type: ClientScopeType }[]
  ) => void;
};

import "./client-scopes.css";

export const AddScopeDialog = ({
  clientScopes,
  open,
  toggleDialog,
  onAdd,
}: AddScopeDialogProps) => {
  const { t } = useTranslation("clients");
  const [addToggle, setAddToggle] = useState(false);
  const [rows, setRows] = useState<ClientScopeRepresentation[]>([]);

  const loader = () => Promise.resolve(clientScopes);

  const action = (scope: ClientScopeType) => {
    const scopes = rows.map((row) => {
      return { scope: row, type: scope };
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
          className="keycloak__client-scopes-add__add-dropdown"
          id="add-dropdown"
          key="add-dropdown"
          direction={DropdownDirection.up}
          isOpen={addToggle}
          toggle={
            <DropdownToggle
              isDisabled={rows.length === 0}
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
            setRows([]);
            toggleDialog();
          }}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <KeycloakDataTable
        loader={loader}
        ariaLabelKey="client-scopes:chooseAMapperType"
        searchPlaceholderKey="client-scopes:searchFor"
        canSelectAll
        onSelect={(rows) => setRows(rows)}
        columns={[
          {
            name: "name",
          },
          {
            name: "description",
          },
        ]}
      />
    </Modal>
  );
};
