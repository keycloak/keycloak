import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";

import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import type { TFunction } from "i18next";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { toUpperCase } from "../../util";
import {
  DropdownItem,
  MenuToggle,
  Select,
  SelectOption,
  SelectProps,
} from "@patternfly/react-core";

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

type CellDropdownProps = Omit<SelectProps, "toggle"> & {
  clientScope: ClientScopeRepresentation;
  type: ClientScopeType | AllClientScopeType;
  all?: boolean;
  onSelect: (value: ClientScopeType | AllClientScopeType) => void;
  isDisabled?: boolean;
};

export const CellDropdown = ({
  clientScope,
  type,
  onSelect,
  all = false,
  isDisabled,
  ...props
}: CellDropdownProps) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);

  return (
    <Select
      key={clientScope.id}
      toggle={(ref) => (
        <MenuToggle
          data-testid="cell-dropdown"
          className={`keycloak__client-scope__${type}`}
          ref={ref}
          onClick={() => setOpen(!open)}
          isExpanded={open}
          isDisabled={isDisabled}
        >
          {t(`clientScopeType.${type}`)}
        </MenuToggle>
      )}
      isOpen={open}
      onOpenChange={(isOpen) => setOpen(isOpen)}
      selected={[type]}
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
  adminClient: KeycloakAdminClient,
  clientScope: ClientScopeDefaultOptionalType,
  changeTo: AllClientScopeType,
) => {
  await removeScope(adminClient, clientScope);
  await addScope(adminClient, clientScope, changeTo);
};

const castAdminClient = (adminClient: KeycloakAdminClient) =>
  adminClient.clientScopes as unknown as {
    [index: string]: (params: { id: string }) => Promise<void>;
  };

export const removeScope = async (
  adminClient: KeycloakAdminClient,
  clientScope: ClientScopeDefaultOptionalType,
) => {
  if (clientScope.type !== AllClientScopes.none)
    await castAdminClient(adminClient)[
      `delDefault${
        clientScope.type === ClientScope.optional ? "Optional" : ""
      }ClientScope`
    ]({
      id: clientScope.id!,
    });
};

const addScope = async (
  adminClient: KeycloakAdminClient,
  clientScope: ClientScopeDefaultOptionalType,
  type: AllClientScopeType,
) => {
  if (type !== AllClientScopes.none)
    await castAdminClient(adminClient)[
      `addDefault${type === ClientScope.optional ? "Optional" : ""}ClientScope`
    ]({
      id: clientScope.id!,
    });
};

export const changeClientScope = async (
  adminClient: KeycloakAdminClient,
  clientId: string,
  clientScope: ClientScopeRepresentation,
  type: AllClientScopeType,
  changeTo: ClientScopeType,
) => {
  if (type !== "none") {
    await removeClientScope(adminClient, clientId, clientScope, type);
  }
  await addClientScope(adminClient, clientId, clientScope, changeTo);
};

export const removeClientScope = async (
  adminClient: KeycloakAdminClient,
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
  adminClient: KeycloakAdminClient,
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
