import React from "react";
import { Meta } from "@storybook/react";
import { Page } from "@patternfly/react-core";
import { UserFederationKerberosSettings } from "../user-federation/UserFederationKerberosSettings";

export default {
  title: "User Federation Kerberos Settings Tab",
  component: UserFederationKerberosSettings,
} as Meta;

export const view = () => {
  return (
    <Page>
      <UserFederationKerberosSettings />
    </Page>
  );
};
