import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import { Page } from "@patternfly/react-core";
import type Keycloak from "keycloak-js";
import { PropsWithChildren, Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { HashRouter as Router, Route, Routes } from "react-router-dom";

import { AlertProvider } from "./components/alert/Alerts";
import { PageBreadCrumbs } from "./components/bread-crumb/PageBreadCrumbs";
import { ErrorRenderer } from "./components/error/ErrorRenderer";
import { Help } from "ui-shared";
import { KeycloakSpinner } from "./components/keycloak-spinner/KeycloakSpinner";
import { AccessContextProvider, useAccess } from "./context/access/Access";
import { AdminClientContext } from "./context/auth/AdminClient";
import { RealmContextProvider } from "./context/realm-context/RealmContext";
import { RealmsProvider } from "./context/RealmsContext";
import { RecentRealmsProvider } from "./context/RecentRealms";
import { ServerInfoProvider } from "./context/server-info/ServerInfoProvider";
import { WhoAmIContextProvider } from "./context/whoami/WhoAmI";
import { ForbiddenSection } from "./ForbiddenSection";
import { SubGroups } from "./groups/SubGroupsContext";
import { Header } from "./PageHeader";
import { PageNav } from "./PageNav";
import { RouteDef, routes } from "./route-config";

export const mainPageContentId = "kc-main-content-page-container";

export type AdminClientProps = {
  keycloak: Keycloak;
  adminClient: KeycloakAdminClient;
};

const AppContexts = ({
  children,
  keycloak,
  adminClient,
}: PropsWithChildren<AdminClientProps>) => (
  <Router>
    <AdminClientContext.Provider value={{ keycloak, adminClient }}>
      <WhoAmIContextProvider>
        <RealmsProvider>
          <RealmContextProvider>
            <RecentRealmsProvider>
              <AccessContextProvider>
                <Help>
                  <AlertProvider>
                    <SubGroups>{children}</SubGroups>
                  </AlertProvider>
                </Help>
              </AccessContextProvider>
            </RecentRealmsProvider>
          </RealmContextProvider>
        </RealmsProvider>
      </WhoAmIContextProvider>
    </AdminClientContext.Provider>
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
            <Routes>
              {routes.map((route, i) => (
                <Route
                  key={i}
                  path={route.path}
                  element={<SecuredRoute route={route} />}
                />
              ))}
            </Routes>
          </ServerInfoProvider>
        </ErrorBoundary>
      </Page>
    </AppContexts>
  );
};
