import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type { UserProfileConfig } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import {
  Button,
  ButtonVariant,
  Dropdown,
  DropdownItem,
  InputGroup,
  KebabToggle,
  SearchInput,
  ToolbarItem,
} from "@patternfly/react-core";
import { ArrowRightIcon } from "@patternfly/react-icons";
import { ReactNode, useState } from "react";
import { useTranslation } from "react-i18next";

import { useAccess } from "../../context/access/Access";
import { SearchDropdown, SearchType } from "../../user/details/SearchFilter";
import { UserAttribute } from "./UserDataTable";
import { UserDataTableAttributeSearchForm } from "./UserDataTableAttributeSearchForm";
import DropdownPanel from "../dropdown-panel/DropdownPanel";

type UserDataTableToolbarItemsProps = {
  searchDropdownOpen: boolean;
  setSearchDropdownOpen: (open: boolean) => void;
  realm: RealmRepresentation;
  hasSelectedRows: boolean;
  toggleDeleteDialog: () => void;
  toggleUnlockUsersDialog: () => void;
  goToCreate: () => void;
  searchType: SearchType;
  setSearchType: (searchType: SearchType) => void;
  searchUser: string;
  setSearchUser: (searchUser: string) => void;
  activeFilters: UserAttribute[];
  setActiveFilters: (activeFilters: UserAttribute[]) => void;
  refresh: () => void;
  profile: UserProfileConfig;
  clearAllFilters: () => void;
  createAttributeSearchChips: () => ReactNode;
  searchUserWithAttributes: () => void;
};

export function UserDataTableToolbarItems({
  searchDropdownOpen,
  setSearchDropdownOpen,
  realm,
  hasSelectedRows,
  toggleDeleteDialog,
  toggleUnlockUsersDialog,
  goToCreate,
  searchType,
  setSearchType,
  searchUser,
  setSearchUser,
  activeFilters,
  setActiveFilters,
  refresh,
  profile,
  clearAllFilters,
  createAttributeSearchChips,
  searchUserWithAttributes,
}: UserDataTableToolbarItemsProps) {
  const { t } = useTranslation();
  const [kebabOpen, setKebabOpen] = useState(false);

  const { hasAccess } = useAccess();

  // Only needs query-users access to attempt add/delete of users.
  // This is because the user could have fine-grained access to users
  // of a group.  There is no way to know this without searching the
  // permissions of every group.
  const isManager = hasAccess("query-users");

  const searchItem = () => {
    return (
      <ToolbarItem>
        <InputGroup>
          <SearchDropdown
            searchType={searchType}
            onSelect={(searchType) => {
              clearAllFilters();
              setSearchType(searchType);
            }}
          />
          {searchType === "default" && defaultSearchInput()}
          {searchType === "attribute" && attributeSearchInput()}
        </InputGroup>
      </ToolbarItem>
    );
  };

  const defaultSearchInput = () => {
    return (
      <ToolbarItem>
        <SearchInput
          placeholder={t("searchForUser")}
          aria-label={t("search")}
          value={searchUser}
          onChange={(_, value) => {
            setSearchUser(value);
          }}
          onSearch={() => {
            setSearchUser(searchUser);
            refresh();
          }}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              setSearchUser(searchUser);
              refresh();
            }
          }}
          onClear={() => {
            setSearchUser("");
            refresh();
          }}
        />
      </ToolbarItem>
    );
  };

  const attributeSearchInput = () => {
    return (
      <>
        <DropdownPanel
          buttonText={t("selectAttributes")}
          setSearchDropdownOpen={setSearchDropdownOpen}
          searchDropdownOpen={searchDropdownOpen}
          width="15vw"
        >
          <UserDataTableAttributeSearchForm
            activeFilters={activeFilters}
            setActiveFilters={setActiveFilters}
            profile={profile}
            createAttributeSearchChips={createAttributeSearchChips}
            searchUserWithAttributes={() => {
              searchUserWithAttributes();
              setSearchDropdownOpen(false);
            }}
          />
        </DropdownPanel>
        <Button
          icon={<ArrowRightIcon />}
          variant="control"
          onClick={() => {
            searchUserWithAttributes();
            setSearchDropdownOpen(false);
          }}
          aria-label={t("searchAttributes")}
        />
      </>
    );
  };

  const bruteForceProtectionToolbarItem = !realm.bruteForceProtected ? (
    <ToolbarItem>
      <Button
        variant={ButtonVariant.link}
        onClick={toggleDeleteDialog}
        data-testid="delete-user-btn"
        isDisabled={hasSelectedRows}
      >
        {t("deleteUser")}
      </Button>
    </ToolbarItem>
  ) : (
    <ToolbarItem>
      <Dropdown
        toggle={<KebabToggle onToggle={(open) => setKebabOpen(open)} />}
        isOpen={kebabOpen}
        isPlain
        dropdownItems={[
          <DropdownItem
            key="deleteUser"
            component="button"
            isDisabled={hasSelectedRows}
            onClick={() => {
              toggleDeleteDialog();
              setKebabOpen(false);
            }}
          >
            {t("deleteUser")}
          </DropdownItem>,

          <DropdownItem
            key="unlock"
            component="button"
            onClick={() => {
              toggleUnlockUsersDialog();
              setKebabOpen(false);
            }}
          >
            {t("unlockAllUsers")}
          </DropdownItem>,
        ]}
      />
    </ToolbarItem>
  );

  const actionItems = (
    <>
      <ToolbarItem>
        <Button data-testid="add-user" onClick={goToCreate}>
          {t("addUser")}
        </Button>
      </ToolbarItem>
      {bruteForceProtectionToolbarItem}
    </>
  );

  return (
    <>
      {searchItem()}
      {isManager ? actionItems : null}
    </>
  );
}
