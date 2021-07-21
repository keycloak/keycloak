import {
  Button,
  ContextSelector,
  ContextSelectorItem,
  Divider,
  Dropdown,
  DropdownItem,
  DropdownToggle,
  Label,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import { CheckIcon } from "@patternfly/react-icons";
import React, { ReactElement, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";

import { useRealm } from "../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { toUpperCase } from "../../util";
import { RecentUsed } from "./recent-used";

import "./realm-selector.css";

export const RealmSelector = () => {
  const { realm, setRealm, realms } = useRealm();
  const { whoAmI } = useWhoAmI();
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const history = useHistory();
  const { t } = useTranslation("common");
  const recentUsed = new RecentUsed();
  const all = recentUsed.used
    .filter((r) => r !== realm)
    .map((name) => {
      return { name, used: true };
    })
    .concat(
      realms
        .filter((r) => !recentUsed.used.includes(r.realm!))
        .map((r) => {
          return { name: r.realm!, used: false };
        })
    );

  const filteredItems = useMemo(() => {
    if (search === "") {
      return undefined;
    }

    return all.filter((r) =>
      r.name.toLowerCase().includes(search.toLowerCase())
    );
  }, [search, all]);

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

  const selectRealm = (realm: string) => {
    setRealm(realm);
    setOpen(!open);
    history.push(`/${realm}/`);
  };

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
          className="keycloak__realm_selector__context_selector"
          footer={
            <ContextSelectorItem key="add">
              <AddRealm />
            </ContextSelectorItem>
          }
        >
          {(filteredItems || all).map((item) => (
            <ContextSelectorItem key={item.name}>
              <RealmText value={item.name} />{" "}
              {item.used && <Label>{t("recent")}</Label>}
            </ContextSelectorItem>
          ))}
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
