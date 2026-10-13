import {
  ErrorBoundaryFallback,
  ErrorBoundaryProvider,
  KeycloakSpinner,
  mainPageContentId,
} from "@keycloak/keycloak-ui-shared";
import { Flex, FlexItem, Page } from "@patternfly/react-core";
import { PropsWithChildren, Suspense, useEffect } from "react";
import { Outlet } from "react-router-dom";
import { AdminClientProvider } from "./admin-client";
import { Header } from "./PageHeader";
import { PageNav } from "./PageNav";
import { PageBreadCrumbs } from "./components/bread-crumb/PageBreadCrumbs";
import { ErrorRenderer } from "./components/error/ErrorRenderer";
import { RecentRealmsProvider } from "./context/RecentRealms";
import { AccessContextProvider } from "./context/access/Access";
import { RealmContextProvider } from "./context/realm-context/RealmContext";
import { ServerInfoProvider } from "./context/server-info/ServerInfoProvider";
import { WhoAmIContextProvider } from "./context/whoami/WhoAmI";
import { SubGroups } from "./groups/SubGroupsContext";
import { AuthWall } from "./root/AuthWall";
import { Banners } from "./Banners";

export const AppContexts = ({ children }: PropsWithChildren) => (
  <ErrorBoundaryProvider>
    <ErrorBoundaryFallback fallback={ErrorRenderer}>
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
    </ErrorBoundaryFallback>
  </ErrorBoundaryProvider>
);

export const App = () => {
  const hrefEndsWithHashSlash = location.href.endsWith("#/");
  useEffect(() => {
    if (!hrefEndsWithHashSlash) return;
    history.replaceState(null, "", location.pathname);
  }, [hrefEndsWithHashSlash, location.pathname]);

  return (
    <AdminClientProvider>
      <AppContexts>
        <Flex
          direction={{ default: "column" }}
          flexWrap={{ default: "nowrap" }}
          spaceItems={{ default: "spaceItemsNone" }}
          style={{ height: "100%" }}
        >
          <FlexItem>
            <Banners />
          </FlexItem>
          <FlexItem grow={{ default: "grow" }} style={{ minHeight: 0 }}>
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
          </FlexItem>
        </Flex>
      </AppContexts>
    </AdminClientProvider>
  );
};
