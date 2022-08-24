import { FormEvent, FunctionComponent } from "react";
import { NavLink, useRouteMatch } from "react-router-dom";
import { useLocation, useNavigate } from "react-router-dom-v5-compat";
import { useTranslation } from "react-i18next";
import {
  Nav,
  NavItem,
  NavGroup,
  NavList,
  PageSidebar,
  Divider,
} from "@patternfly/react-core";

import { RealmSelector } from "./components/realm-selector/RealmSelector";
import { useRealm } from "./context/realm-context/RealmContext";
import { useAccess } from "./context/access/Access";
import { routes } from "./route-config";
import { AddRealmRoute } from "./realm/routes/AddRealm";

import "./page-nav.css";

export const PageNav: FunctionComponent = () => {
  const { t } = useTranslation("common");
  const { hasAccess, hasSomeAccess } = useAccess();
  const { realm } = useRealm();
  const location = useLocation();
  const navigate = useNavigate();

  type SelectedItem = {
    groupId: number | string;
    itemId: number | string;
    to: string;
    event: FormEvent<HTMLInputElement>;
  };

  const onSelect = (item: SelectedItem) => {
    navigate(item.to);
    item.event.preventDefault();
  };

  type LeftNavProps = { title: string; path: string };
  const LeftNav = ({ title, path }: LeftNavProps) => {
    const route = routes.find(
      (route) => route.path.replace(/\/:.+?(\?|(?:(?!\/).)*|$)/g, "") === path
    );

    const accessAllowed =
      route &&
      (route.access instanceof Array
        ? hasAccess(...route.access)
        : hasAccess(route.access));

    if (!accessAllowed) {
      return null;
    }

    //remove "/realm-name" from the start of the path
    const activeItem = location.pathname.substring(realm.length + 1);
    return (
      <li>
        <NavLink
          id={"nav-item" + path.replace("/", "-")}
          to={`/${realm}${path}`}
          className="pf-c-nav__link"
          activeClassName="pf-m-current"
          isActive={() =>
            path === activeItem || (path !== "/" && activeItem.startsWith(path))
          }
        >
          {t(title)}
        </NavLink>
      </li>
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

  const isOnAddRealm = !!useRouteMatch(AddRealmRoute.path);

  return (
    <PageSidebar
      className="keycloak__page_nav__nav"
      nav={
        <Nav onSelect={onSelect}>
          <NavList>
            <NavItem className="keycloak__page_nav__nav_item__realm-selector">
              <RealmSelector />
            </NavItem>
          </NavList>
          <Divider />
          {showManage && !isOnAddRealm && (
            <NavGroup aria-label={t("manage")} title={t("manage")}>
              <LeftNav title="clients" path="/clients" />
              <LeftNav title="clientScopes" path="/client-scopes" />
              <LeftNav title="realmRoles" path="/roles" />
              <LeftNav title="users" path="/users" />
              <LeftNav title="groups" path="/groups" />
              <LeftNav title="sessions" path="/sessions" />
              <LeftNav title="events" path="/events" />
            </NavGroup>
          )}

          {showConfigure && !isOnAddRealm && (
            <NavGroup aria-label={t("configure")} title={t("configure")}>
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
