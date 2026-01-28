import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  Select,
  SelectList,
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
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";
import { useMemo } from "react";

export type SearchType = "name" | "type" | "protocol";
export const PROTOCOLS = ["all", "saml", "openid-connect"] as const;
export type ProtocolType = (typeof PROTOCOLS)[number] | "oid4vc";

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
  const { t } = useTranslation();
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
      onOpenChange={(isOpen) => setSearchToggle(isOpen)}
      toggle={(ref) => (
        <MenuToggle
          data-testid="clientScopeSearch"
          ref={ref}
          id="toggle-id"
          onClick={() => setSearchToggle(!searchToggle)}
        >
          <FilterIcon /> {t(`clientScopeSearch.${searchType}`)}
        </MenuToggle>
      )}
      isOpen={searchToggle}
    >
      <DropdownList>{options}</DropdownList>
    </Dropdown>
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
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const isFeatureEnabled = useIsFeatureEnabled();
  const protocols = useMemo<readonly ProtocolType[]>(
    () =>
      isFeatureEnabled(Feature.OpenId4VCI)
        ? ([...PROTOCOLS, "oid4vc"] as const)
        : PROTOCOLS,
    [isFeatureEnabled],
  );

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
              toggle={(ref) => (
                <MenuToggle
                  data-testid="clientScopeSearchType"
                  ref={ref}
                  isExpanded={open}
                  onClick={() => setOpen(!open)}
                >
                  {type === AllClientScopes.none
                    ? t("allTypes")
                    : t(`clientScopeTypes.${type}`)}
                </MenuToggle>
              )}
              onOpenChange={(val) => setOpen(val)}
              isOpen={open}
              selected={
                type === AllClientScopes.none
                  ? t("allTypes")
                  : t(`clientScopeTypes.${type}`)
              }
              onSelect={(_, value) => {
                onType(value as AllClientScopes);
                setOpen(false);
              }}
            >
              <SelectList>
                <SelectOption value={AllClientScopes.none}>
                  {t("allTypes")}
                </SelectOption>
                {clientScopeTypesSelectOptions(t)}
              </SelectList>
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
              toggle={(ref) => (
                <MenuToggle
                  data-testid="clientScopeSearchProtocol"
                  ref={ref}
                  isExpanded={open}
                  onClick={() => setOpen(!open)}
                >
                  {t(`protocolTypes.${protocol}`)}
                </MenuToggle>
              )}
              onOpenChange={(val) => setOpen(val)}
              isOpen={open}
              selected={t(`protocolTypes.${protocol}`)}
              onSelect={(_, value) => {
                onProtocol?.(value as ProtocolType);
                setOpen(false);
              }}
            >
              <SelectList>
                {protocols.map((type) => (
                  <SelectOption key={type} value={type}>
                    {t(`protocolTypes.${type}`)}
                  </SelectOption>
                ))}
              </SelectList>
            </Select>
          </ToolbarItem>
        </>
      )}
    </>
  );
};
