import { Wizard } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { KerberosSettingsRequired } from "./kerberos/KerberosSettingsRequired";
import { KerberosSettingsCache } from "./kerberos/KerberosSettingsCache";

export const UserFederationKerberosWizard = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  const steps = [
    {
      name: t("requiredSettings"),
      component: (
        <KerberosSettingsRequired showSectionHeading showSectionDescription />
      ),
    },
    {
      name: t("cacheSettings"),
      component: (
        <KerberosSettingsCache showSectionHeading showSectionDescription />
      ),
      nextButtonText: t("common:finish"), // TODO: needs to disable until cache policy is valid
    },
  ];

  return (
    <Wizard
      title={t("addKerberosWizardTitle")}
      description={helpText("addKerberosWizardDescription")}
      steps={steps}
    />
  );
};
