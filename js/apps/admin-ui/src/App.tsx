import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import {
  mainPageContentId,
  useEnvironment,
} from "@keycloak/keycloak-ui-shared";
import { Page } from "@patternfly/react-core";
import { PropsWithChildren, Suspense, useEffect, useState } from "react";
import { Outlet } from "react-router-dom";

import { Header } from "./PageHeader";
import { PageNav } from "./PageNav";
import { AdminClientContext, initAdminClient } from "./admin-client";
import { AlertProvider } from "./components/alert/Alerts";
import { PageBreadCrumbs } from "./components/bread-crumb/PageBreadCrumbs";
import { ErrorRenderer } from "./components/error/ErrorRenderer";
import { KeycloakSpinner } from "./components/keycloak-spinner/KeycloakSpinner";
import {
  ErrorBoundaryFallback,
  ErrorBoundaryProvider,
} from "./context/ErrorBoundary";
import { RealmsProvider } from "./context/RealmsContext";
import { RecentRealmsProvider } from "./context/RecentRealms";
import { AccessContextProvider } from "./context/access/Access";
import { RealmContextProvider } from "./context/realm-context/RealmContext";
import { ServerInfoProvider } from "./context/server-info/ServerInfoProvider";
import { WhoAmIContextProvider } from "./context/whoami/WhoAmI";
import type { Environment } from "./environment";
import { SubGroups } from "./groups/SubGroupsContext";
import { AuthWall } from "./root/AuthWall";

const AppContexts = ({ children }: PropsWithChildren) => (
  <ErrorBoundaryProvider>
    <ServerInfoProvider>
      <RealmContextProvider>
        <WhoAmIContextProvider>
          <RealmsProvider>
            <RecentRealmsProvider>
              <AccessContextProvider>
                <AlertProvider>
                  <SubGroups>{children}</SubGroups>
                </AlertProvider>
              </AccessContextProvider>
            </RecentRealmsProvider>
          </RealmsProvider>
        </WhoAmIContextProvider>
      </RealmContextProvider>
    </ServerInfoProvider>
  </ErrorBoundaryProvider>
);

export const App = () => {
  const { keycloak, environment } = useEnvironment<Environment>();
  const [adminClient, setAdminClient] = useState<KeycloakAdminClient>();

  useEffect(() => {
    const init = async () => {
      const client = await initAdminClient(keycloak, environment);
      setAdminClient(client);
    };
    init().catch(console.error);
  }, []);

  if (!adminClient) return <KeycloakSpinner />;
  return (
    <AdminClientContext.Provider value={{ keycloak, adminClient }}>
      <AppContexts>
        <Page
          header={<Header />}
          isManagedSidebar
          sidebar={<PageNav />}
          breadcrumb={<PageBreadCrumbs />}
          mainContainerId={mainPageContentId}
        >
          <ErrorBoundaryFallback fallback={ErrorRenderer}>
            <Suspense fallback={<KeycloakSpinner />}>
              <AuthWall>
                <Outlet />
              </AuthWall>
            </Suspense>
          </ErrorBoundaryFallback>
        </Page>
      </AppContexts>
    </AdminClientContext.Provider>
  );
};
