import React from "react";
import type { Meta } from "@storybook/react";
import { Page } from "@patternfly/react-core";
import { UserFederationKerberosSettings } from "../user-federation/UserFederationKerberosSettings";
import { MockAdminClient } from "./MockAdminClient";

export default {
  title: "User Federation Kerberos Settings Tab",
  component: UserFederationKerberosSettings,
} as Meta;

export const view = () => {
  return (
    <Page>
      <MockAdminClient
        mock={{ components: { findOne: () => Promise.resolve({}) } }}
      >
        <UserFederationKerberosSettings />
      </MockAdminClient>
    </Page>
  );
};
