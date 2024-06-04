import {
  KeycloakProvider,
  environmentAccount as environment,
} from "@keycloak/keycloak-ui-shared";
import { Page, Spinner } from "@patternfly/react-core";
import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import { Header } from "./Header";
import { PageNav } from "./PageNav";

export const Root = () => {
  return (
    <KeycloakProvider environment={environment}>
      <Page header={<Header />} sidebar={<PageNav />} isManagedSidebar>
        <Suspense fallback={<Spinner />}>
          <Outlet />
        </Suspense>
      </Page>
    </KeycloakProvider>
  );
};
