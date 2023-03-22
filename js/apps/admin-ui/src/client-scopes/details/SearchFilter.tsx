import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Dropdown,
  DropdownItem,
  DropdownToggle,
  Select,
  SelectOption,
  ToolbarItem,
} from "@patternfly/react-core";
import { FilterIcon } from "@patternfly/react-icons";

import {
  AllClientScopes,
  AllClientScopeType,
  clientScopeTypesSelectOptions,
} from "../../components/client-scope/ClientScopeTypes";
import type { Row } from "../../clients/scopes/ClientScopes";

export type SearchType = "name" | "type" | "protocol";
export const PROTOCOLS = ["all", "saml", "openid-connect"] as const;
export type ProtocolType = (typeof PROTOCOLS)[number];

export const nameFilter =
  (search = "") =>
  (scope: Row) =>
    scope.name?.includes(search);
export const typeFilter = (type: AllClientScopeType) => (scope: Row) =>
  type === AllClientScopes.none || scope.type === type;

export const protocolFilter = (protocol: ProtocolType) => (scope: Row) =>
  protocol === "all" || scope.protocol === protocol;

type SearchToolbarProps = Omit<SearchDropdownProps, "withProtocol"> & {
  type: AllClientScopeType;
  onType: (value: AllClientScopes) => void;
  protocol?: ProtocolType;
  onProtocol?: (value: ProtocolType) => void;
};

type SearchDropdownProps = {
  searchType: SearchType;
  onSelect: (value: SearchType) => void;
  withProtocol?: boolean;
};

export const SearchDropdown = ({
  searchType,
  withProtocol = false,
  onSelect,
}: SearchDropdownProps) => {
  const { t } = useTranslation("clients");
  const [searchToggle, setSearchToggle] = useState(false);

  const createDropdown = (searchType: SearchType) => (
    <DropdownItem
      key={searchType}
      onClick={() => {
        onSelect(searchType);
        setSearchToggle(false);
      }}
    >
      {t(`clientScopeSearch.${searchType}`)}
    </DropdownItem>
  );
  const options = [createDropdown("name"), createDropdown("type")];
  if (withProtocol) {
    options.push(createDropdown("protocol"));
  }

  return (
    <Dropdown
      className="keycloak__client-scopes__searchtype"
      toggle={
        <DropdownToggle id="toggle-id" onToggle={setSearchToggle}>
          <FilterIcon /> {t(`clientScopeSearch.${searchType}`)}
        </DropdownToggle>
      }
      isOpen={searchToggle}
      dropdownItems={options}
    />
  );
};

export const SearchToolbar = ({
  searchType,
  onSelect,
  type,
  onType,
  protocol,
  onProtocol,
}: SearchToolbarProps) => {
  const { t } = useTranslation("client-scopes");
  const [open, setOpen] = useState(false);

  return (
    <>
      {searchType === "type" && (
        <>
          <ToolbarItem>
            <SearchDropdown
              searchType={searchType}
              onSelect={onSelect}
              withProtocol={!!protocol}
            />
          </ToolbarItem>
          <ToolbarItem>
            <Select
              className="keycloak__client-scopes__searchtype"
              onToggle={setOpen}
              isOpen={open}
              selections={[
                type === AllClientScopes.none
                  ? t("common:allTypes")
                  : t(`common:clientScope.${type}`),
              ]}
              onSelect={(_, value) => {
                onType(value as AllClientScopes);
                setOpen(false);
              }}
            >
              <SelectOption value={AllClientScopes.none}>
                {t("common:allTypes")}
              </SelectOption>
              <>{clientScopeTypesSelectOptions(t)}</>
            </Select>
          </ToolbarItem>
        </>
      )}
      {searchType === "protocol" && !!protocol && (
        <>
          <ToolbarItem>
            <SearchDropdown
              searchType={searchType}
              onSelect={onSelect}
              withProtocol
            />
          </ToolbarItem>
          <ToolbarItem>
            <Select
              className="keycloak__client-scopes__searchtype"
              onToggle={setOpen}
              isOpen={open}
              selections={[t(`protocolTypes.${protocol}`)]}
              onSelect={(_, value) => {
                onProtocol?.(value as ProtocolType);
                setOpen(false);
              }}
            >
              {PROTOCOLS.map((type) => (
                <SelectOption key={type} value={type}>
                  {t(`protocolTypes.${type}`)}
                </SelectOption>
              ))}
            </Select>
          </ToolbarItem>
        </>
      )}
    </>
  );
};
