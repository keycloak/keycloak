import { Page, Spinner } from "@patternfly/react-core";
import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import { AlertProvider, Help } from "ui-shared";
import { environment } from "../environment";
import { Header } from "./Header";
import { KeycloakProvider } from "./KeycloakContext";
import { PageNav } from "./PageNav";

export const Root = () => {
  return (
    <KeycloakProvider environment={environment}>
      <Page header={<Header />} sidebar={<PageNav />} isManagedSidebar>
        <AlertProvider>
          <Help>
            <Suspense fallback={<Spinner />}>
              <Outlet />
            </Suspense>
          </Help>
        </AlertProvider>
      </Page>
    </KeycloakProvider>
  );
};
