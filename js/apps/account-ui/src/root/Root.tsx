import { Page } from "@patternfly/react-core";
import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import { environment } from "../environment";
import { Header } from "./Header";
import { KeycloakProvider } from "./KeycloakContext";
import { PageNav } from "./PageNav";

export const Root = () => {
  return (
    <KeycloakProvider environment={environment}>
      <Page header={<Header />} sidebar={<PageNav />} isManagedSidebar>
        <Suspense>
          <Outlet />
        </Suspense>
      </Page>
    </KeycloakProvider>
  );
};
