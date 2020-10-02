import React from "react";
import { Page } from "@patternfly/react-core";
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";

import { Header } from "./PageHeader";
import { PageNav } from "./PageNav";
import { Help } from "./components/help-enabler/HelpHeader";

import { RealmContextProvider } from "./components/realm-context/RealmContext";
import { WhoAmIContextProvider } from "./whoami/WhoAmI";

import { routes } from "./route-config";
import { PageBreadCrumbs } from "./components/bread-crumb/PageBreadCrumbs";

export const App = () => {
  return (
    <Router>
      <WhoAmIContextProvider>
        <RealmContextProvider>
          <Help>
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
          </Help>
        </RealmContextProvider>
      </WhoAmIContextProvider>
    </Router>
  );
};
