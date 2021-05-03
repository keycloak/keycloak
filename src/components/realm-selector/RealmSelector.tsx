import React, { useState, useContext, useEffect, ReactElement } from "react";
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
  Label,
} from "@patternfly/react-core";
import { CheckIcon } from "@patternfly/react-icons";

import RealmRepresentation from "keycloak-admin/lib/defs/realmRepresentation";
import { toUpperCase } from "../../util";
import { useRealm } from "../../context/realm-context/RealmContext";
import { WhoAmIContext } from "../../context/whoami/WhoAmI";
import { RecentUsed } from "./recent-used";

import "./realm-selector.css";

export const RealmSelector = () => {
  const { realm, setRealm, realms } = useRealm();
  const { whoAmI } = useContext(WhoAmIContext);
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [filteredItems, setFilteredItems] = useState<RealmRepresentation[]>();
  const history = useHistory();
  const { t } = useTranslation("common");
  const recentUsed = new RecentUsed();

  const RealmText = ({ value }: { value: string }) => (
    <Split className="keycloak__realm_selector__list-item-split">
      <SplitItem isFilled>{toUpperCase(value)}</SplitItem>
      <SplitItem>{value === realm && <CheckIcon />}</SplitItem>
    </Split>
  );

  const AddRealm = () => (
    <Button
      data-testid="add-realm"
      component="div"
      isBlock
      onClick={() => {
        history.push(`/${realm}/add-realm`);
        setOpen(!open);
      }}
    >
      {t("createRealm")}
    </Button>
  );

  const onFilter = () => {
    if (search === "") {
      setFilteredItems(undefined);
    } else {
      const filtered = realms.filter(
        (r) => r.realm!.toLowerCase().indexOf(search.toLowerCase()) !== -1
      );
      setFilteredItems(filtered);
    }
  };

  const selectRealm = (realm: string) => {
    setRealm(realm);
    setOpen(!open);
    history.push(`/${realm}/`);
  };

  useEffect(() => {
    onFilter();
  }, [search]);

  const dropdownItems = realms.map((r) => (
    <DropdownItem
      key={`realm-dropdown-item-${r.realm}`}
      onClick={() => {
        selectRealm(r.realm!);
      }}
    >
      <RealmText value={r.realm!} />
    </DropdownItem>
  ));

  const addRealmComponent = (
    <React.Fragment key="Add Realm">
      {whoAmI.canCreateRealm() && (
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
      {realms.length > 5 && (
        <ContextSelector
          data-testid="realmSelector"
          toggleText={toUpperCase(realm)}
          isOpen={open}
          screenReaderLabel={toUpperCase(realm)}
          onToggle={() => setOpen(!open)}
          onSelect={(_, r) => {
            let element: ReactElement;
            if (Array.isArray(r)) {
              element = (r as ReactElement[])[0];
            } else {
              element = r as ReactElement;
            }
            const value = element.props.value;
            if (value) {
              selectRealm(value);
            }
          }}
          searchInputValue={search}
          onSearchInputChange={(value) => setSearch(value)}
          onSearchButtonClick={() => onFilter()}
          className="keycloak__realm_selector__context_selector"
        >
          {recentUsed.used.map((realm) => (
            <ContextSelectorItem key={realm}>
              <RealmText value={realm} /> <Label>{t("recent")}</Label>
            </ContextSelectorItem>
          ))}
          {(filteredItems || realms)
            .filter((r) => !recentUsed.used.includes(r.realm!))
            .map((item) => (
              <ContextSelectorItem key={item.id}>
                <RealmText value={item.realm!} />
              </ContextSelectorItem>
            ))}
          <ContextSelectorItem key="add">
            <AddRealm />
          </ContextSelectorItem>
        </ContextSelector>
      )}
      {realms.length <= 5 && (
        <Dropdown
          id="realm-select"
          data-testid="realmSelector"
          className="keycloak__realm_selector__dropdown"
          isOpen={open}
          toggle={
            <DropdownToggle
              data-testid="realmSelectorToggle"
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
