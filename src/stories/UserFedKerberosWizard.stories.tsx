import React from "react";
import type { Meta } from "@storybook/react";
import { Page, PageSection } from "@patternfly/react-core";
import { UserFederationKerberosWizard } from "../user-federation/UserFederationKerberosWizard";
import { MockAdminClient } from "./MockAdminClient";

export default {
  title: "User Federation Kerberos Wizard",
  component: UserFederationKerberosWizard,
} as Meta;

export const view = () => {
  return (
    <Page style={{ height: "80vh" }}>
      <PageSection isFilled>
        <MockAdminClient>
          <UserFederationKerberosWizard />
        </MockAdminClient>
      </PageSection>
    </Page>
  );
};
