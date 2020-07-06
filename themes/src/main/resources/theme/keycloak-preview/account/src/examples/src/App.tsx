import React, { Fragment } from 'react'

import { AccountServiceClient, AccountServiceContext, AccountPage, KeycloakContext, KeycloakService, KeycloakClient } from 'keycloak-preview';
import '@patternfly/react-core/dist/styles/base.css';
import 'keycloak-preview/dist/index.css'

declare const keycloak: KeycloakClient;

const App = () => {
  const keycloakService = new KeycloakService(keycloak);
  return <Fragment>
    <h1 style={{fontSize: 'xx-large'}}>Hello to my integrated keycloak account</h1>
    <KeycloakContext.Provider value={keycloakService}>
      <AccountServiceContext.Provider value={new AccountServiceClient(keycloakService)}>
        <AccountPage />
      </AccountServiceContext.Provider>
    </KeycloakContext.Provider>
  </Fragment>
}

export default App
