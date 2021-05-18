import React from "react";
import type { Meta } from "@storybook/react";
import { Page } from "@patternfly/react-core";
import { UserFederationLdapSettings } from "../user-federation/UserFederationLdapSettings";
import { MockAdminClient } from "./MockAdminClient";

export default {
  title: "User Federation LDAP Settings Tab",
  component: UserFederationLdapSettings,
} as Meta;

export const view = () => {
  return (
    <Page>
      <MockAdminClient
        mock={{ components: { findOne: () => Promise.resolve({}) } }}
      >
        <UserFederationLdapSettings />
      </MockAdminClient>
    </Page>
  );
};
