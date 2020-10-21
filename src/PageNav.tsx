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
import { useAccess } from "./context/access/Access";
import { RealmRepresentation } from "./realm/models/Realm";
import { routes } from "./route-config";

export const PageNav: React.FunctionComponent = () => {
  const { t } = useTranslation("common");
  const { hasAccess, hasSomeAccess } = useAccess();
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

  type LeftNavProps = { title: string; path: string };
  const LeftNav = ({ title, path }: LeftNavProps) => {
    const route = routes(() => {}).find((route) => route.path === path);
    console.log(`hasAccess(${route!.access})=` + hasAccess(route!.access));
    if (!route || !hasAccess(route.access)) return <></>;

    return (
      <NavItem
        id={"nav-item" + path.replace("/", "-")}
        to={path}
        isActive={activeItem === path}
      >
        {t(title)}
      </NavItem>
    );
  };

  const showManage = hasSomeAccess(
    "view-realm",
    "query-groups",
    "query-users",
    "view-events"
  );

  const showConfigure = hasSomeAccess(
    "view-realm",
    "query-clients",
    "view-identity-providers"
  );

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
              {showManage && (
                <NavGroup title={t("manage")}>
                  <LeftNav title="clients" path="/clients" />
                  <LeftNav title="clientScopes" path="/client-scopes" />
                  <LeftNav title="realmRoles" path="/roles" />
                  <LeftNav title="users" path="/users" />
                  <LeftNav title="groups" path="/groups" />
                  <LeftNav title="sessions" path="/sessions" />
                  <LeftNav title="events" path="/events" />
                </NavGroup>
              )}

              {showConfigure && (
                <NavGroup title={t("configure")}>
                  <LeftNav title="realmSettings" path="/realm-settings" />
                  <LeftNav title="authentication" path="/authentication" />
                  <LeftNav
                    title="identityProviders"
                    path="/identity-providers"
                  />
                  <LeftNav title="userFederation" path="/user-federation" />
                </NavGroup>
              )}
            </Nav>
          }
        />
      )}
    </DataLoader>
  );
};
