import React, { ReactNode } from "react";
import { Page } from "@patternfly/react-core";
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";

import { Header } from "./PageHeader";
import { PageNav } from "./PageNav";
import { Help } from "./components/help-enabler/HelpHeader";

import { RealmContextProvider } from "./context/realm-context/RealmContext";
import { WhoAmIContextProvider } from "./context/whoami/WhoAmI";
import { ServerInfoProvider } from "./context/server-info/ServerInfoProvider";
import { AlertProvider } from "./components/alert/Alerts";

import { routes } from "./route-config";
import { PageBreadCrumbs } from "./components/bread-crumb/PageBreadCrumbs";
const AppContexts = ({ children }: { children: ReactNode }) => (
  <WhoAmIContextProvider>
    <RealmContextProvider>
      <Help>
        <AlertProvider>
          <ServerInfoProvider>{children}</ServerInfoProvider>
        </AlertProvider>
      </Help>
    </RealmContextProvider>
  </WhoAmIContextProvider>
);

export const App = () => {
  return (
    <AppContexts>
      <Router>
        <Page
          header={<Header />}
          isManagedSidebar
          sidebar={<PageNav />}
          breadcrumb={<PageBreadCrumbs />}
        >
          <Switch>
            {routes(() => {}).map((route, i) => (
              <Route key={i} {...route} exact />
            ))}
          </Switch>
        </Page>
      </Router>
    </AppContexts>
  );
};
