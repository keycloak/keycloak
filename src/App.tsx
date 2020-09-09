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
import { ClientScopesPage } from "./page/ClientScopesPage";
import { RealmRolesPage } from "./page/RealmRolesPage";
import { UsersPage } from "./page/UsersPage";
import { GroupsPage } from "./page/GroupsPage";
import { SessionsPage } from "./page/SessionsPage";
import { EventsPage } from "./page/EventsPage";
import { RealmSettingsPage } from "./page/RealmSettingsPage";
import { AuthenticationPage } from "./page/AuthenticationPage";
import { IdentityProvidersPage } from "./page/IdentityProvidersPage";
import { UserFederationPage } from "./page/UserFederationPage";

import { PageNotFoundPage } from "./page/PageNotFoundPage";

export const App = () => {
  return (
    <Router>
      <Help>
        <Page header={<Header />} isManagedSidebar sidebar={<PageNav />}>
          <PageSection variant="light">
            <Switch>
              <Route exact path="/add-realm" component={NewRealmForm}></Route>

              <Route exact path="/clients" component={ClientsPage}></Route>
              <Route exact path="/add-client" component={NewClientForm}></Route>
              <Route exact path="/import-client" component={ImportForm}></Route>

              <Route
                exact
                path="/client-scopes"
                component={ClientScopesPage}
              ></Route>
              <Route
                exact
                path="/realm-roles"
                component={RealmRolesPage}
              ></Route>
              <Route exact path="/users" component={UsersPage}></Route>
              <Route exact path="/groups" component={GroupsPage}></Route>
              <Route exact path="/sessions" component={SessionsPage}></Route>
              <Route exact path="/events" component={EventsPage}></Route>

              <Route
                exact
                path="/realm-settings"
                component={RealmSettingsPage}
              ></Route>
              <Route
                exact
                path="/authentication"
                component={AuthenticationPage}
              ></Route>
              <Route
                exact
                path="/identity-providers"
                component={IdentityProvidersPage}
              ></Route>
              <Route
                exact
                path="/user-federation"
                component={UserFederationPage}
              ></Route>

              <Route exact path="/" component={ClientsPage} />
              <Route component={PageNotFoundPage} />
            </Switch>
          </PageSection>
        </Page>
      </Help>
    </Router>
  );
};
