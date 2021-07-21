import React from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Nav,
  NavItem,
  NavGroup,
  NavList,
  PageSidebar,
} from "@patternfly/react-core";

import { RealmSelector } from "./components/realm-selector/RealmSelector";
import { useRealm } from "./context/realm-context/RealmContext";
import { useAccess } from "./context/access/Access";
import { routes } from "./route-config";

export const PageNav: React.FunctionComponent = () => {
  const { t } = useTranslation("common");
  const { hasAccess, hasSomeAccess } = useAccess();
  const { realm } = useRealm();

  const history = useHistory();

  type SelectedItem = {
    groupId: number | string;
    itemId: number | string;
    to: string;
    event: React.FormEvent<HTMLInputElement>;
  };

  const onSelect = (item: SelectedItem) => {
    history.push(item.to);
    item.event.preventDefault();
  };

  type LeftNavProps = { title: string; path: string };
  const LeftNav = ({ title, path }: LeftNavProps) => {
    const route = routes.find(
      (route) => route.path.replace(/\/:.+?(\?|(?:(?!\/).)*|$)/g, "") === path
    );
    if (!route || !hasAccess(route.access)) return <></>;
    //remove "/realm-name" from the start of the path
    const activeItem = history.location.pathname.substr(realm.length + 1);
    return (
      <NavItem
        id={"nav-item" + path.replace("/", "-")}
        to={`/${realm}${path}`}
        isActive={
          path === activeItem || (path !== "/" && activeItem.startsWith(path))
        }
      >
        {t(title)}
      </NavItem>
    );
  };

  const showManage = hasSomeAccess(
    "view-realm",
    "query-groups",
    "query-users",
    "query-clients",
    "view-events"
  );

  const showConfigure = hasSomeAccess(
    "view-realm",
    "query-clients",
    "view-identity-providers"
  );

  const { pathname } = useLocation();
  const isOnAddRealm = () => pathname.indexOf("add-realm") === -1;

  return (
    <PageSidebar
      nav={
        <Nav onSelect={onSelect}>
          <NavList>
            <NavItem className="keycloak__page_nav__nav_item__realm-selector">
              <RealmSelector />
            </NavItem>
          </NavList>
          {isOnAddRealm() && (
            <NavGroup title="">
              <LeftNav title="home" path="/" />
            </NavGroup>
          )}
          {showManage && isOnAddRealm() && (
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

          {showConfigure && isOnAddRealm() && (
            <NavGroup title={t("configure")}>
              <LeftNav title="realmSettings" path="/realm-settings" />
              <LeftNav title="authentication" path="/authentication" />
              <LeftNav title="identityProviders" path="/identity-providers" />
              <LeftNav title="userFederation" path="/user-federation" />
            </NavGroup>
          )}
        </Nav>
      }
    />
  );
};
