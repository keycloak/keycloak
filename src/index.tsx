import React from 'react';
import ReactDom from 'react-dom';

import { App } from './App';
import init from './auth/keycloak';
import { KeycloakContext } from './auth/KeycloakContext';
import { KeycloakService } from './auth/keycloak.service';
import { HttpClientContext } from './http-service/HttpClientContext';
import { HttpClient } from './http-service/http-client';

init().then((keycloak) => {
  const keycloakService = new KeycloakService(keycloak);
  ReactDom.render(
    <KeycloakContext.Provider value={keycloakService}>
      <HttpClientContext.Provider value={new HttpClient(keycloakService)}>
        <App />
      </HttpClientContext.Provider>
    </KeycloakContext.Provider>,
    document.getElementById('app')
  );
});

(document.getElementById('favicon') as HTMLAnchorElement).href = `${
  import.meta.env.SNOWPACK_PUBLIC_FAVICON
}`;
