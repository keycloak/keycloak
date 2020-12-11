import React, { ReactNode } from "react";
import { Page } from "@patternfly/react-core";
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";

import { Header } from "./PageHeader";
import { PageNav } from "./PageNav";
import { Help } from "./components/help-enabler/HelpHeader";

import { ServerInfoProvider } from "./context/server-info/ServerInfoProvider";
import { AlertProvider } from "./components/alert/Alerts";

import { AccessContextProvider, useAccess } from "./context/access/Access";
import { routes, RouteDef } from "./route-config";
import { PageBreadCrumbs } from "./components/bread-crumb/PageBreadCrumbs";
import { ForbiddenSection } from "./ForbiddenSection";

// This must match the id given as scrollableSelector in scroll-form
const mainPageContentId = "kc-main-content-page-container";

const AppContexts = ({ children }: { children: ReactNode }) => (
  <AccessContextProvider>
    <Help>
      <AlertProvider>
        <ServerInfoProvider>{children}</ServerInfoProvider>
      </AlertProvider>
    </Help>
  </AccessContextProvider>
);

// If someone tries to go directly to a route they don't
// have access to, show forbidden page.
type SecuredRouteProps = { route: RouteDef };
const SecuredRoute = ({ route }: SecuredRouteProps) => {
  const { hasAccess } = useAccess();

  if (hasAccess(route.access)) return <route.component />;

  return <ForbiddenSection />;
};

export const App = () => {
  return (
    <AppContexts>
      <Router>
        <Page
          header={<Header />}
          isManagedSidebar
          sidebar={<PageNav />}
          breadcrumb={<PageBreadCrumbs />}
          mainContainerId={mainPageContentId}
        >
          <Switch>
            {routes(() => {}).map((route, i) => (
              <Route
                exact
                key={i}
                path={route.path}
                component={() => <SecuredRoute route={route} />}
              />
            ))}
          </Switch>
        </Page>
      </Router>
    </AppContexts>
  );
};
