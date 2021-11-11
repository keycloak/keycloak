import React, { FunctionComponent, Suspense } from "react";
import { Page, Spinner } from "@patternfly/react-core";
import { HashRouter as Router, Route, Switch } from "react-router-dom";
import { ErrorBoundary } from "react-error-boundary";

import { Header } from "./PageHeader";
import { PageNav } from "./PageNav";
import { Help } from "./components/help-enabler/HelpHeader";

import { ServerInfoProvider } from "./context/server-info/ServerInfoProvider";
import { AlertProvider } from "./components/alert/Alerts";

import { AccessContextProvider, useAccess } from "./context/access/Access";
import { routes, RouteDef } from "./route-config";
import { PageBreadCrumbs } from "./components/bread-crumb/PageBreadCrumbs";
import { ForbiddenSection } from "./ForbiddenSection";
import { SubGroups } from "./groups/SubGroupsContext";
import { RealmsProvider } from "./context/RealmsContext";
import { RealmContextProvider } from "./context/realm-context/RealmContext";
import { ErrorRenderer } from "./components/error/ErrorRenderer";
import { AdminClient } from "./context/auth/AdminClient";
import { WhoAmIContextProvider } from "./context/whoami/WhoAmI";
import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";

export const mainPageContentId = "kc-main-content-page-container";

export type AdminClientProps = {
  adminClient: KeycloakAdminClient;
};

const AppContexts: FunctionComponent<AdminClientProps> = ({
  children,
  adminClient,
}) => (
  <Router>
    <AdminClient.Provider value={adminClient}>
      <WhoAmIContextProvider>
        <RealmsProvider>
          <RealmContextProvider>
            <AccessContextProvider>
              <Help>
                <AlertProvider>
                  <ServerInfoProvider>
                    <SubGroups>{children}</SubGroups>
                  </ServerInfoProvider>
                </AlertProvider>
              </Help>
            </AccessContextProvider>
          </RealmContextProvider>
        </RealmsProvider>
      </WhoAmIContextProvider>
    </AdminClient.Provider>
  </Router>
);

// If someone tries to go directly to a route they don't
// have access to, show forbidden page.
type SecuredRouteProps = { route: RouteDef };
const SecuredRoute = ({ route }: SecuredRouteProps) => {
  const { hasAccess } = useAccess();
  const accessAllowed =
    route.access instanceof Array
      ? hasAccess(...route.access)
      : hasAccess(route.access);

  if (accessAllowed)
    return (
      <Suspense fallback={<Spinner />}>
        <route.component />
      </Suspense>
    );

  return <ForbiddenSection />;
};

export const App = ({ adminClient }: AdminClientProps) => {
  return (
    <AppContexts adminClient={adminClient}>
      <Page
        header={<Header />}
        isManagedSidebar
        sidebar={<PageNav />}
        breadcrumb={<PageBreadCrumbs />}
        mainContainerId={mainPageContentId}
      >
        <ErrorBoundary
          FallbackComponent={ErrorRenderer}
          onReset={() =>
            (window.location.href =
              window.location.origin + window.location.pathname)
          }
        >
          <Switch>
            {routes.map((route, i) => (
              <Route
                exact={route.matchOptions?.exact ?? true}
                key={i}
                path={route.path}
                component={() => <SecuredRoute route={route} />}
              />
            ))}
          </Switch>
        </ErrorBoundary>
      </Page>
    </AppContexts>
  );
};
