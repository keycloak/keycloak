import React from "react";
import { TFunction } from "i18next";
import { DropdownItem, SelectOption } from "@patternfly/react-core";

export enum ClientScope {
  default = "default",
  optional = "optional",
}
export type ClientScopeType = ClientScope.default | ClientScope.optional;
const clientScopeTypes = Object.keys(ClientScope);

export const clientScopeTypesSelectOptions = (t: TFunction) =>
  clientScopeTypes.map((type) => (
    <SelectOption key={type} value={type}>
      {t(`clientScope.${type}`)}
    </SelectOption>
  ));

export const clientScopeTypesDropdown = (
  t: TFunction,
  onClick: (scope: ClientScopeType) => void
) =>
  clientScopeTypes.map((type) => (
    <DropdownItem key={type} onClick={() => onClick(type as ClientScopeType)}>
      {t(`clientScope.${type}`)}
    </DropdownItem>
  ));
