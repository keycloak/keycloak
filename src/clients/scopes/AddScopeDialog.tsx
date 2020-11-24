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
import {
  Table,
  TableBody,
  TableHeader,
  TableVariant,
} from "@patternfly/react-table";
import ClientScopeRepresentation from "keycloak-admin/lib/defs/clientScopeRepresentation";

import { clientScopeTypesDropdown } from "./ClientScopeTypes";

export type AddScopeDialogProps = {
  clientScopes: ClientScopeRepresentation[];
  open: boolean;
  toggleDialog: () => void;
};

export const AddScopeDialog = ({
  clientScopes,
  open,
  toggleDialog,
}: AddScopeDialogProps) => {
  const { t } = useTranslation("clients");
  const [addToggle, setAddToggle] = useState(false);

  const data = clientScopes.map((scope) => {
    return { cells: [scope.name, scope.description] };
  });

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
          dropdownItems={clientScopeTypesDropdown(t)}
        />,
        <Button
          id="modal-cancel"
          key="cancel"
          variant={ButtonVariant.secondary}
          onClick={toggleDialog}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Table
        variant={TableVariant.compact}
        cells={[t("name"), t("description")]}
        onSelect={(_, isSelected, rowIndex) => {}}
        rows={data}
        aria-label={t("chooseAMapperType")}
      >
        <TableHeader />
        <TableBody />
      </Table>
    </Modal>
  );
};
