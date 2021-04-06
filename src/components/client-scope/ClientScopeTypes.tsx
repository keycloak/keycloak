import React, { useState } from "react";

import { TFunction } from "i18next";
import { useTranslation } from "react-i18next";
import { DropdownItem, Select, SelectOption } from "@patternfly/react-core";

import ClientScopeRepresentation from "keycloak-admin/lib/defs/clientScopeRepresentation";

export enum ClientScope {
  default = "default",
  optional = "optional",
}

export enum AllClientScopes {
  none = "none",
}

export type ClientScopeType = ClientScope;
export type AllClientScopeType = ClientScope | AllClientScopes;

const clientScopeTypes = Object.keys(ClientScope);
export const allClientScopeTypes = Object.keys({
  ...AllClientScopes,
  ...ClientScope,
});

export const clientScopeTypesSelectOptions = (
  t: TFunction,
  scopeTypes: string[] | undefined = clientScopeTypes
) =>
  scopeTypes.map((type) => (
    <SelectOption key={type} value={type}>
      {t(`common:clientScope.${type}`)}
    </SelectOption>
  ));

export const clientScopeTypesDropdown = (
  t: TFunction,
  onClick: (scope: ClientScopeType) => void
) =>
  clientScopeTypes.map((type) => (
    <DropdownItem key={type} onClick={() => onClick(type as ClientScopeType)}>
      {t(`common:clientScope.${type}`)}
    </DropdownItem>
  ));

type CellDropdownProps = {
  clientScope: ClientScopeRepresentation;
  type: ClientScopeType | AllClientScopeType;
  all?: boolean;
  onSelect: (value: ClientScopeType | AllClientScopeType) => void;
};

export const CellDropdown = ({
  clientScope,
  type,
  onSelect,
  all = false,
}: CellDropdownProps) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);

  return (
    <Select
      className={`keycloak__client-scope__${type}`}
      key={clientScope.id}
      onToggle={() => setOpen(!open)}
      isOpen={open}
      selections={[type]}
      onSelect={(_, value) => {
        onSelect(
          all ? (value as ClientScopeType) : (value as AllClientScopeType)
        );
        setOpen(false);
      }}
    >
      {clientScopeTypesSelectOptions(
        t,
        all ? allClientScopeTypes : clientScopeTypes
      )}
    </Select>
  );
};
