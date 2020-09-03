import React from "react";
import { Page, PageSection, Button } from "@patternfly/react-core";
import { Header } from "./PageHeader";
import { PageNav } from "./PageNav";

import { Help } from "./components/help-enabler/HelpHeader";
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";
import { NewRealmForm } from "./forms/realm/NewRealmForm";
import { NewClientForm } from "./forms/client/NewClientForm";
import { ImportForm } from "./forms/client/ImportForm";
import { ClientsPage } from "./page/ClientsPage";

export const App = () => {
  return (
    <Router>
      <Help>
        <Page header={<Header />} isManagedSidebar sidebar={<PageNav />}>
          <PageSection variant="light">
            <Switch>
              <Route exact path="/add-realm" component={NewRealmForm}></Route>
              <Route exact path="/add-client" component={NewClientForm}></Route>
              <Route exact path="/import-client" component={ImportForm}></Route>
              <Route exact path="/" component={ClientsPage}></Route>
            </Switch>
          </PageSection>
        </Page>
      </Help>
    </Router>
  );
};
