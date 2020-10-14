import React, { useState, useContext } from "react";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Nav,
  NavItem,
  NavGroup,
  NavList,
  PageSidebar,
} from "@patternfly/react-core";
import { RealmSelector } from "./components/realm-selector/RealmSelector";
import { DataLoader } from "./components/data-loader/DataLoader";
import { HttpClientContext } from "./context/http-service/HttpClientContext";
import { RealmRepresentation } from "./realm/models/Realm";

export const PageNav: React.FunctionComponent = () => {
  const { t } = useTranslation("common");
  const httpClient = useContext(HttpClientContext)!;
  const realmLoader = async () => {
    const response = await httpClient.doGet<RealmRepresentation[]>(
      "/admin/realms"
    );
    return response.data;
  };

  const history = useHistory();

  let initialItem = history.location.pathname;
  if (initialItem === "/") initialItem = "/client-list";

  const [activeItem, setActiveItem] = useState(initialItem);

  type SelectedItem = {
    groupId: number | string;
    itemId: number | string;
    to: string;
    event: React.FormEvent<HTMLInputElement>;
  };

  const onSelect = (item: SelectedItem) => {
    setActiveItem(item.to);
    history.push(item.to);
    item.event.preventDefault();
  };

  const makeNavItem = (title: string, path: string) => {
    return (
      <NavItem
        id={"nav-item-" + path}
        to={"/" + path}
        isActive={activeItem === "/" + path}
      >
        {t(title)}
      </NavItem>
    );
  };

  return (
    <DataLoader loader={realmLoader}>
      {(realmList) => (
        <PageSidebar
          nav={
            <Nav onSelect={onSelect}>
              <NavList>
                <NavItem className="keycloak__page_nav__nav_item__realm-selector">
                  <RealmSelector realmList={realmList.data || []} />
                </NavItem>
              </NavList>
              <NavGroup title={t("manage")}>
                {makeNavItem("clients", "clients")}
                {makeNavItem("clientScopes", "client-scopes")}
                {makeNavItem("realmRoles", "roles")}
                {makeNavItem("users", "users")}
                {makeNavItem("groups", "groups")}
                {makeNavItem("sessions", "sessions")}
                {makeNavItem("events", "events")}
              </NavGroup>

              <NavGroup title={t("configure")}>
                {makeNavItem("realmSettings", "realm-settings")}
                {makeNavItem("authentication", "authentication")}
                {makeNavItem("identityProviders", "identity-providers")}
                {makeNavItem("userFederation", "user-federation")}
              </NavGroup>
            </Nav>
          }
        />
      )}
    </DataLoader>
  );
};
