import React from "react";
import { Meta } from "@storybook/react";
import { Page } from "@patternfly/react-core";
import { NewRealmForm } from "../forms/realm/NewRealmForm";

export default {
  title: "New reaml form",
  component: NewRealmForm,
} as Meta;

export const view = () => {
  return (
    <Page>
      <NewRealmForm />
    </Page>
  );
};
