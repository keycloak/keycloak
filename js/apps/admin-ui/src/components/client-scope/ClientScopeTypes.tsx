import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";

import {
  DropdownItem,
  Select,
  SelectOption,
  SelectProps,
} from "@patternfly/react-core/deprecated";
import type { TFunction } from "i18next";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import { adminClient } from "../../admin-client";
import { toUpperCase } from "../../util";

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
}) as AllClientScopeType[];

export const clientScopeTypesSelectOptions = (
  t: TFunction,
  scopeTypes: string[] | undefined = clientScopeTypes,
) =>
  scopeTypes.map((type) => (
    <SelectOption key={type} value={type}>
      {t(`clientScopeType.${type}`)}
    </SelectOption>
  ));

export const clientScopeTypesDropdown = (
  t: TFunction,
  onClick: (scope: ClientScopeType) => void,
) =>
  clientScopeTypes.map((type) => (
    <DropdownItem key={type} onClick={() => onClick(type as ClientScopeType)}>
      {t(`clientScopeType.${type}`)}
    </DropdownItem>
  ));

type CellDropdownProps = Omit<SelectProps, "onToggle"> & {
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
  ...props
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
          all ? (value as ClientScopeType) : (value as AllClientScopeType),
        );
        setOpen(false);
      }}
      {...props}
    >
      {clientScopeTypesSelectOptions(
        t,
        all ? allClientScopeTypes : clientScopeTypes,
      )}
    </Select>
  );
};

export type ClientScopeDefaultOptionalType = ClientScopeRepresentation & {
  type: AllClientScopeType;
};

export const changeScope = async (
  clientScope: ClientScopeDefaultOptionalType,
  changeTo: AllClientScopeType,
) => {
  await removeScope(clientScope);
  await addScope(clientScope, changeTo);
};

const castAdminClient = () =>
  adminClient.clientScopes as unknown as {
    [index: string]: Function;
  };

export const removeScope = async (
  clientScope: ClientScopeDefaultOptionalType,
) => {
  if (clientScope.type !== AllClientScopes.none)
    await castAdminClient()[
      `delDefault${
        clientScope.type === ClientScope.optional ? "Optional" : ""
      }ClientScope`
    ]({
      id: clientScope.id!,
    });
};

const addScope = async (
  clientScope: ClientScopeDefaultOptionalType,
  type: AllClientScopeType,
) => {
  if (type !== AllClientScopes.none)
    await castAdminClient()[
      `addDefault${type === ClientScope.optional ? "Optional" : ""}ClientScope`
    ]({
      id: clientScope.id!,
    });
};

export const changeClientScope = async (
  clientId: string,
  clientScope: ClientScopeRepresentation,
  type: AllClientScopeType,
  changeTo: ClientScopeType,
) => {
  if (type !== "none") {
    await removeClientScope(clientId, clientScope, type);
  }
  await addClientScope(clientId, clientScope, changeTo);
};

export const removeClientScope = async (
  clientId: string,
  clientScope: ClientScopeRepresentation,
  type: ClientScope,
) => {
  const methodName = `del${toUpperCase(type)}ClientScope` as const;

  await adminClient.clients[methodName]({
    id: clientId,
    clientScopeId: clientScope.id!,
  });
};

export const addClientScope = async (
  clientId: string,
  clientScope: ClientScopeRepresentation,
  type: ClientScopeType,
) => {
  const methodName = `add${toUpperCase(type)}ClientScope` as const;

  await adminClient.clients[methodName]({
    id: clientId,
    clientScopeId: clientScope.id!,
  });
};
