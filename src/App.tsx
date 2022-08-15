import { FunctionComponent, Suspense } from "react";
import { Page } from "@patternfly/react-core";
import { HashRouter as Router, Route, Switch } from "react-router-dom";
import { CompatRouter } from "react-router-dom-v5-compat";
import { ErrorBoundary } from "react-error-boundary";
import type Keycloak from "keycloak-js";
import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";

import { Header } from "./PageHeader";
import { PageNav } from "./PageNav";
import { Help } from "./components/help-enabler/HelpHeader";

import { ServerInfoProvider } from "./context/server-info/ServerInfoProvider";
import { AlertProvider } from "./components/alert/Alerts";

import { AccessContextProvider, useAccess } from "./context/access/Access";
import { routes, RouteDef } from "./route-config";
import { PageBreadCrumbs } from "./components/bread-crumb/PageBreadCrumbs";
import { KeycloakSpinner } from "./components/keycloak-spinner/KeycloakSpinner";
import { ForbiddenSection } from "./ForbiddenSection";
import { SubGroups } from "./groups/SubGroupsContext";
import { RealmsProvider } from "./context/RealmsContext";
import { RealmContextProvider } from "./context/realm-context/RealmContext";
import { ErrorRenderer } from "./components/error/ErrorRenderer";
import { AdminClientContext } from "./context/auth/AdminClient";
import { WhoAmIContextProvider } from "./context/whoami/WhoAmI";

export const mainPageContentId = "kc-main-content-page-container";

export type AdminClientProps = {
  keycloak: Keycloak;
  adminClient: KeycloakAdminClient;
};

const AppContexts: FunctionComponent<AdminClientProps> = ({
  children,
  keycloak,
  adminClient,
}) => (
  <Router>
    <CompatRouter>
      <AdminClientContext.Provider value={{ keycloak, adminClient }}>
        <WhoAmIContextProvider>
          <RealmsProvider>
            <RealmContextProvider>
              <AccessContextProvider>
                <Help>
                  <AlertProvider>
                    <SubGroups>{children}</SubGroups>
                  </AlertProvider>
                </Help>
              </AccessContextProvider>
            </RealmContextProvider>
          </RealmsProvider>
        </WhoAmIContextProvider>
      </AdminClientContext.Provider>
    </CompatRouter>
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
      <Suspense fallback={<KeycloakSpinner />}>
        <route.component />
      </Suspense>
    );

  return <ForbiddenSection permissionNeeded={route.access} />;
};

export const App = ({ keycloak, adminClient }: AdminClientProps) => {
  return (
    <AppContexts keycloak={keycloak} adminClient={adminClient}>
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
          <ServerInfoProvider>
            <Switch>
              {routes.map((route, i) => (
                <Route
                  key={i}
                  path={route.path}
                  exact={route.matchOptions?.exact ?? true}
                >
                  <SecuredRoute route={route} />
                </Route>
              ))}
            </Switch>
          </ServerInfoProvider>
        </ErrorBoundary>
      </Page>
    </AppContexts>
  );
};
