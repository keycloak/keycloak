import React from "react";
import type { Meta } from "@storybook/react";
import { Page } from "@patternfly/react-core";
import { NewRealmForm } from "../realm/add/NewRealmForm";
import { MockAdminClient } from "./MockAdminClient";

export default {
  title: "New realm form",
  component: NewRealmForm,
} as Meta;

export const view = () => {
  return (
    <Page>
      <MockAdminClient>
        <NewRealmForm />
      </MockAdminClient>
    </Page>
  );
};
