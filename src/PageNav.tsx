import React, { useState } from "react";
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
import { useAdminClient } from "./context/auth/AdminClient";
import { useAccess } from "./context/access/Access";
import { routes } from "./route-config";

export const PageNav: React.FunctionComponent = () => {
  const { t } = useTranslation("common");
  const { hasAccess, hasSomeAccess } = useAccess();
  const adminClient = useAdminClient();
  const realmLoader = async () => {
    return await adminClient.realms.find();
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
    "query-clients",
    "view-events"
  );

  const showConfigure = hasSomeAccess(
    "view-realm",
    "query-clients",
    "view-identity-providers"
  );

  return (
    <PageSidebar
      nav={
        <Nav onSelect={onSelect}>
          <NavList>
            <DataLoader loader={realmLoader}>
              {(realmList) => (
                <NavItem className="keycloak__page_nav__nav_item__realm-selector">
                  <RealmSelector realmList={realmList.data || []} />
                </NavItem>
              )}
            </DataLoader>
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
              <LeftNav title="identityProviders" path="/identity-providers" />
              <LeftNav title="userFederation" path="/user-federation" />
            </NavGroup>
          )}
        </Nav>
      }
    />
  );
};
