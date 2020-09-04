import React, { useState, useContext, useEffect } from "react";
import { useHistory } from "react-router-dom";
import { Realm } from "../../models/Realm";

import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  Button,
  Divider,
} from "@patternfly/react-core";

import style from "./realm-selector.module.css";
import { HttpClientContext } from "../../http-service/HttpClientContext";

export const RealmSelector = () => {
  const [open, setOpen] = useState(false);
  const history = useHistory();
  const httpClient = useContext(HttpClientContext);
  const [realms, setRealms] = useState([] as Realm[]);
  const [currentRealm, setCurrentRealm] = useState("Master");

  const getRealms = async () => {
    return await httpClient
      ?.doGet("/admin/realms")
      .then((r) => r.data as Realm[]);
  };

  useEffect(() => {
    getRealms().then((result) => {
      setRealms(result) !== undefined ? result : [];
    });
  }, []);

  const dropdownItems = realms.map((r) => (
    <DropdownItem
      component="a"
      href={"/#/realms/" + r.id}
      key={r.id}
      onClick={() => setCurrentRealm(r.realm)}
    >
      {r.realm.charAt(0).toUpperCase() + r.realm.slice(1)}
    </DropdownItem>
  ));

  return (
    <Dropdown
      id="realm-select"
      className={style.dropdown}
      isOpen={open}
      toggle={
        <DropdownToggle
          id="realm-select-toggle"
          onToggle={() => setOpen(!open)}
          className={style.toggle}
        >
          {currentRealm}
        </DropdownToggle>
      }
      dropdownItems={[
        ...dropdownItems,
        <Divider key={1} />,
        <DropdownItem component="div" key="add">
          <Button
            className="realmSelectButton"
            onClick={() => history.push("/add-realm")}
          >
            Create Realm
          </Button>
        </DropdownItem>,
      ]}
    />
  );
};
