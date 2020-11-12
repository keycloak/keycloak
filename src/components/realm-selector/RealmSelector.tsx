import React, { useState, useContext, useEffect } from "react";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";

import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  Button,
  Divider,
  SplitItem,
  Split,
  ContextSelector,
  ContextSelectorItem,
} from "@patternfly/react-core";
import { CheckIcon } from "@patternfly/react-icons";

import RealmRepresentation from "keycloak-admin/lib/defs/realmRepresentation";
import { RealmContext } from "../../context/realm-context/RealmContext";
import { WhoAmIContext } from "../../context/whoami/WhoAmI";

import "./realm-selector.css";

type RealmSelectorProps = {
  realmList: RealmRepresentation[];
};

export const RealmSelector = ({ realmList }: RealmSelectorProps) => {
  const { realm, setRealm } = useContext(RealmContext);
  const whoami = useContext(WhoAmIContext);
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [filteredItems, setFilteredItems] = useState(realmList);
  const history = useHistory();
  const { t } = useTranslation("common");

  const toUpperCase = (realmName: string) =>
    realmName.charAt(0).toUpperCase() + realmName.slice(1);

  const RealmText = ({ value }: { value: string }) => (
    <Split className="keycloak__realm_selector__list-item-split">
      <SplitItem isFilled>{toUpperCase(value)}</SplitItem>
      <SplitItem>{value === realm && <CheckIcon />}</SplitItem>
    </Split>
  );

  const AddRealm = ({ className }: { className?: string }) => (
    <Button
      component="div"
      isBlock
      onClick={() => history.push("/add-realm")}
      className={className}
    >
      {t("createRealm")}
    </Button>
  );

  const onFilter = () => {
    const filtered =
      search === ""
        ? realmList
        : realmList.filter(
            (r) => r.realm!.toLowerCase().indexOf(search.toLowerCase()) !== -1
          );
    setFilteredItems(filtered || []);
  };

  useEffect(() => {
    onFilter();
  }, [search]);

  const dropdownItems = realmList.map((r) => (
    <DropdownItem
      key={`realm-dropdown-item-${r.realm}`}
      onClick={() => {
        setRealm(r.realm!);
        setOpen(!open);
      }}
    >
      <RealmText value={r.realm!} />
    </DropdownItem>
  ));

  const addRealmComponent = (
    <React.Fragment key="Add Realm">
      {whoami.canCreateRealm() && (
        <>
          <Divider key="divider" />
          <DropdownItem key="add">
            <AddRealm />
          </DropdownItem>
        </>
      )}
    </React.Fragment>
  );

  return (
    <>
      {realmList.length > 5 && (
        <ContextSelector
          toggleText={toUpperCase(realm)}
          isOpen={open}
          screenReaderLabel={toUpperCase(realm)}
          onToggle={() => setOpen(!open)}
          onSelect={(_, r) => {
            const value = ((r as unknown) as any).props.value;
            setRealm(value || "master");
            setOpen(!open);
          }}
          searchInputValue={search}
          onSearchInputChange={(value) => setSearch(value)}
          onSearchButtonClick={() => onFilter()}
          className="keycloak__realm_selector__context_selector"
        >
          {filteredItems.map((item) => (
            <ContextSelectorItem key={item.id}>
              <RealmText value={item.realm!} />
            </ContextSelectorItem>
          ))}
          <ContextSelectorItem key="add">
            <AddRealm />
          </ContextSelectorItem>
        </ContextSelector>
      )}
      {realmList.length <= 5 && (
        <Dropdown
          id="realm-select"
          className="keycloak__realm_selector__dropdown"
          isOpen={open}
          toggle={
            <DropdownToggle
              id="realm-select-toggle"
              onToggle={() => setOpen(!open)}
              className="keycloak__realm_selector_dropdown__toggle"
            >
              {toUpperCase(realm)}
            </DropdownToggle>
          }
          dropdownItems={[...dropdownItems, addRealmComponent]}
        />
      )}
    </>
  );
};
