import { KeycloakProvider } from "@keycloak/keycloak-ui-shared";
import { Alert, AlertActionLink, Page, Spinner } from "@patternfly/react-core";
import { Suspense } from "react";
import { Outlet } from "react-router-dom";

import { environment } from "../environment";
import { Header } from "./Header";
import { PageNav } from "./PageNav";

export const Root = () => {
  return (
    <KeycloakProvider environment={environment}>
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
          Please configure Keycloak so that it is served securely, such as from
          HTTPS in production or localhost during development.
        </Alert>
      )}
      <Page header={<Header />} sidebar={<PageNav />} isManagedSidebar>
        <Suspense fallback={<Spinner />}>
          <Outlet />
        </Suspense>
      </Page>
    </KeycloakProvider>
  );
};
