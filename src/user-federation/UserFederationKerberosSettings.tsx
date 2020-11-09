import { PageSection, Title } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { KerberosSettingsRequired } from "./KerberosSettingsRequired";
import { KerberosSettingsCache } from "./KerberosSettingsCache";

export const UserFederationKerberosSettings = () => {
  const { t } = useTranslation("user-federation");

  return (
    <>
      <PageSection variant="light">
        {/* Required settings */}
        <Title size={"xl"} headingLevel={"h2"} className="pf-u-mb-lg">
          {t("requiredSettings")}
        </Title>
        <KerberosSettingsRequired />
      </PageSection>
      <PageSection variant="light" isFilled>
        {/* Cache settings */}
        <Title size={"xl"} headingLevel={"h2"} className="pf-u-mb-lg">
          {t("cacheSettings")}
        </Title>
        <KerberosSettingsCache />
      </PageSection>
    </>
  );
};
