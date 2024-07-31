import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import {
  mainPageContentId,
  useEnvironment,
} from "@keycloak/keycloak-ui-shared";
import { Alert, AlertActionLink, Page } from "@patternfly/react-core";
import { PropsWithChildren, Suspense, useEffect, useState } from "react";
import { Outlet } from "react-router-dom";

import { Header } from "./PageHeader";
import { PageNav } from "./PageNav";
import { AdminClientContext, initAdminClient } from "./admin-client";
import { PageBreadCrumbs } from "./components/bread-crumb/PageBreadCrumbs";
import { ErrorRenderer } from "./components/error/ErrorRenderer";
import { KeycloakSpinner } from "./components/keycloak-spinner/KeycloakSpinner";
import {
  ErrorBoundaryFallback,
  ErrorBoundaryProvider,
} from "./context/ErrorBoundary";
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
          <RecentRealmsProvider>
            <AccessContextProvider>
              <SubGroups>{children}</SubGroups>
            </AccessContextProvider>
          </RecentRealmsProvider>
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
        {!window.isSecureContext && (
          <Alert
            variant="danger"
            title="Keycloak is running in an insecure context."
            actionLinks={
              <AlertActionLink
                component="a"
                href="https://developer.mozilla.org/en-US/docs/Web/Security/Secure_Contexts"
              >
                More information
              </AlertActionLink>
            }
            isInline
          >
            Keycloak is running in an insecure context, this is not a supported
            configuration and will lead to unexpected and breaking behavior.
            Please configure Keycloak so that it is served securely, such as
            from HTTPS in production or localhost during development.
          </Alert>
        )}
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
