import { Button, Wizard } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { KerberosSettingsRequired } from "./KerberosSettingsRequired";
import { KerberosSettingsCache } from "./KerberosSettingsCache";

export const UserFederationKerberosWizard = () => {
  const { t } = useTranslation("user-federation");

  const steps = [
    { name: "Required settings", component: <KerberosSettingsRequired /> },
    { name: "Cache settings", component: <KerberosSettingsCache /> },
  ];
  const title = "Add Kerberos user federation provider";

  return (
    <Wizard
      title={title}
      description="Text needed here"
      steps={steps}
      // onClose={handleModalToggle}
      // isOpen={isOpen}
    />
  );
};
