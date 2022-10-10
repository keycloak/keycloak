import { Page, Spinner } from "@patternfly/react-core";
import { Suspense } from "react";
import { Outlet } from "react-router";

import { PageHeader } from "./PageHeader";
import { PageNav } from "./PageNav";

export const Root = () => (
  <Page header={<PageHeader />} sidebar={<PageNav />} isManagedSidebar>
    <Suspense fallback={<Spinner />}>
      <Outlet />
    </Suspense>
  </Page>
);
