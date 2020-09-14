import React, { useState, useContext } from "react";
import { useHistory } from "react-router-dom";
import {
  Nav,
  NavItem,
  NavGroup,
  NavList,
  PageSidebar,
} from "@patternfly/react-core";
import { RealmSelector } from "./components/realm-selector/RealmSelector";
import { DataLoader } from "./components/data-loader/DataLoader";
import { HttpClientContext } from "./http-service/HttpClientContext";
import { Realm } from "./realm/models/Realm";

export const PageNav: React.FunctionComponent = () => {
  const httpClient = useContext(HttpClientContext)!;
  const realmLoader = async () => {
    const response = await httpClient.doGet<Realm[]>("/admin/realms");
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
        {title}
      </NavItem>
    );
  };

  return (
    <PageSidebar
      nav={
        <Nav onSelect={onSelect}>
          <NavList>
            <NavItem className="keycloak__page_nav__nav_item__realm-selector">
              <DataLoader loader={realmLoader}>
                {(realmList) => (
                  <RealmSelector realm="Master" realmList={realmList || []} />
                )}
              </DataLoader>
            </NavItem>
          </NavList>
          <NavGroup title="Manage">
            {makeNavItem("Clients", "clients")}
            {makeNavItem("Client Scopes", "client-scopes")}
            {makeNavItem("Realm Roles", "realm-roles")}
            {makeNavItem("Users", "users")}
            {makeNavItem("Groups", "groups")}
            {makeNavItem("Sessions", "sessions")}
            {makeNavItem("Events", "events")}
          </NavGroup>

          <NavGroup title="Configure">
            {makeNavItem("Realm settings", "realm-settings")}
            {makeNavItem("Authentication", "authentication")}
            {makeNavItem("Identity providers", "identity-providers")}
            {makeNavItem("User federation", "user-federation")}
          </NavGroup>
        </Nav>
      }
    />
  );
};
