import { PageSection } from "@patternfly/react-core";
import React from "react";
import { KerberosSettingsRequired } from "./kerberos/KerberosSettingsRequired";
import { KerberosSettingsCache } from "./kerberos/KerberosSettingsCache";

export const UserFederationKerberosSettings = () => {
  return (
    <>
      <PageSection variant="light">
        <KerberosSettingsRequired showSectionHeading />
      </PageSection>
      <PageSection variant="light" isFilled>
        <KerberosSettingsCache showSectionHeading />
      </PageSection>
    </>
  );
};
