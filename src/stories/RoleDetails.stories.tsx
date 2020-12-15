import React from "react";
import { Page } from "@patternfly/react-core";
import { Meta } from "@storybook/react";

import { MockAdminClient } from "./MockAdminClient";
import { RealmRolesForm } from "../realm-roles/RealmRoleForm";

export default {
  title: "New role form",
  component: RealmRolesForm,
} as Meta;

export const View = () => {
  return (
    <Page>
      <MockAdminClient>
        <RealmRolesForm />
      </MockAdminClient>
    </Page>
  );
};
