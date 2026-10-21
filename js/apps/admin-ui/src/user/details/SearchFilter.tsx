import { KeycloakSelect } from "@keycloak/keycloak-ui-shared";
import {
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  SelectOption,
  ToolbarItem,
} from "@patternfly/react-core";
import { FilterIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";

export type SearchType = "default" | "attribute";

type SearchToolbarProps = SearchDropdownProps;

type SearchDropdownProps = {
  searchType: SearchType;
  onSelect: (value: SearchType) => void;
};

export const SearchDropdown = ({
  searchType,
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
      {t(`searchType.${searchType}`)}
    </DropdownItem>
  );
  const options = [createDropdown("default"), createDropdown("attribute")];

  return (
    <Dropdown
      className="keycloak__users__searchtype"
      onOpenChange={(isOpen) => setSearchToggle(isOpen)}
      toggle={(ref) => (
        <MenuToggle
          data-testid="user-search-toggle"
          ref={ref}
          id="toggle-id"
          onClick={() => setSearchToggle(!searchToggle)}
          icon={<FilterIcon />}
        >
          {t(`searchType.${searchType}`)}
        </MenuToggle>
      )}
      isOpen={searchToggle}
    >
      <DropdownList>{options}</DropdownList>
    </Dropdown>
  );
};

export const SearchToolbar = ({ searchType, onSelect }: SearchToolbarProps) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);

  return (
    <>
      <ToolbarItem>
        <SearchDropdown searchType={searchType} onSelect={onSelect} />
      </ToolbarItem>
      <ToolbarItem>
        <KeycloakSelect
          className="keycloak__users__searchtype"
          onToggle={(val) => setOpen(val)}
          isOpen={open}
          selections={[t("default"), t("attribute")]}
          onSelect={() => setOpen(false)}
        >
          <SelectOption value={"default"}>{t("default")}</SelectOption>
          <SelectOption value={"attribute"}>{t("attribute")}</SelectOption>
        </KeycloakSelect>
      </ToolbarItem>
    </>
  );
};
