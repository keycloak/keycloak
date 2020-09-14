import React, { useState, useContext, useEffect } from "react";
import { useHistory } from "react-router-dom";
import { Realm } from "../../realm/models/Realm";

import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  Button,
  Divider,
} from "@patternfly/react-core";

import "./realm-selector.css";

type RealmSelectorProps = {
  realm: string;
  realmList: Realm[];
};

export const RealmSelector = ({ realm, realmList }: RealmSelectorProps) => {
  const [open, setOpen] = useState(false);
  const history = useHistory();
  const [currentRealm, setCurrentRealm] = useState(realm);

  const dropdownItems = realmList.map((r) => (
    <DropdownItem
      key={r.id}
      onClick={() => {
        setCurrentRealm(r.realm);
        setOpen(!open);
      }}
    >
      {r.realm.charAt(0).toUpperCase() + r.realm.slice(1)}
    </DropdownItem>
  ));

  return (
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
          {currentRealm}
        </DropdownToggle>
      }
      dropdownItems={[
        ...dropdownItems,
        <Divider key={1} />,
        <DropdownItem component="div" key="add">
          <Button isBlock onClick={() => history.push("/add-realm")}>
            Create Realm
          </Button>
        </DropdownItem>,
      ]}
    />
  );
};
