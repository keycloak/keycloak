import React from "react";
import { Page } from "@patternfly/react-core";
import type { Meta } from "@storybook/react";

import { MockAdminClient } from "./MockAdminClient";
import { RealmRoleTabs } from "../realm-roles/RealmRoleTabs";

export default {
  title: "New role form",
  component: RealmRoleTabs,
} as Meta;

export const View = () => {
  return (
    <Page>
      <MockAdminClient>
        <RealmRoleTabs />
      </MockAdminClient>
    </Page>
  );
};
