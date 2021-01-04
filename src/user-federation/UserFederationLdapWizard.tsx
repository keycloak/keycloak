import {
  Button,
  Wizard,
  WizardContextConsumer,
  WizardFooter,
} from "@patternfly/react-core";
import React from "react";
import { LdapSettingsGeneral } from "./ldap/LdapSettingsGeneral";
import { LdapSettingsConnection } from "./ldap/LdapSettingsConnection";
import { LdapSettingsSearching } from "./ldap/LdapSettingsSearching";
import { LdapSettingsSynchronization } from "./ldap/LdapSettingsSynchronization";
import { LdapSettingsKerberosIntegration } from "./ldap/LdapSettingsKerberosIntegration";
import { LdapSettingsCache } from "./ldap/LdapSettingsCache";
import { LdapSettingsAdvanced } from "./ldap/LdapSettingsAdvanced";
import { useTranslation } from "react-i18next";

export const UserFederationLdapWizard = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  const steps = [
    {
      name: t("requiredSettings"),
      id: "ldapRequiredSettingsStep",
      component: (
        <LdapSettingsGeneral showSectionHeading showSectionDescription />
      ),
    },
    {
      name: t("connectionAndAuthenticationSettings"),
      id: "ldapConnectionSettingsStep",
      component: (
        <LdapSettingsConnection showSectionHeading showSectionDescription />
      ),
    },
    {
      name: t("ldapSearchingAndUpdatingSettings"),
      id: "ldapSearchingSettingsStep",
      component: (
        <LdapSettingsSearching showSectionHeading showSectionDescription />
      ),
    },
    {
      name: t("synchronizationSettings"),
      id: "ldapSynchronizationSettingsStep",
      component: (
        <LdapSettingsSynchronization
          showSectionHeading
          showSectionDescription
        />
      ),
    },
    {
      name: t("kerberosIntegration"),
      id: "ldapKerberosIntegrationSettingsStep",
      component: (
        <LdapSettingsKerberosIntegration
          showSectionHeading
          showSectionDescription
        />
      ),
    },
    {
      name: t("cacheSettings"),
      id: "ldapCacheSettingsStep",
      component: (
        <LdapSettingsCache showSectionHeading showSectionDescription />
      ),
    },
    {
      name: t("advancedSettings"),
      id: "ldapAdvancedSettingsStep",
      component: (
        <LdapSettingsAdvanced showSectionHeading showSectionDescription />
      ),
    },
  ];

  const title = t("addLdapWizardTitle");
  const description = helpText("addLdapWizardDescription");

  const footer = (
    <WizardFooter>
      <WizardContextConsumer>
        {({ activeStep, onNext, onBack, onClose }) => {
          // First step buttons
          if (activeStep.id == "ldapRequiredSettingsStep") {
            return (
              <>
                <Button variant="primary" type="submit" onClick={onNext}>
                  {t("common:next")}
                </Button>
                <Button
                  variant="secondary"
                  onClick={onBack}
                  className="pf-m-disabled"
                >
                  {t("common:back")}
                </Button>
                <Button variant="link" onClick={onClose}>
                  {t("common:cancel")}
                </Button>
              </>
            );
          }
          // Other required step buttons
          else if (
            activeStep.id == "ldapConnectionSettingsStep" ||
            activeStep.id == "ldapSearchingSettingsStep"
          ) {
            return (
              <>
                <Button variant="primary" type="submit" onClick={onNext}>
                  {t("common:next")}
                </Button>
                <Button variant="secondary" onClick={onBack}>
                  {t("common:back")}
                </Button>
                <Button variant="link" onClick={onClose}>
                  {t("common:cancel")}
                </Button>
              </>
            );
          }
          // Last step buttons
          else if (activeStep.id == "ldapAdvancedSettingsStep") {
            return (
              <>
                <Button onClick={() => {}}>
                  {" "}
                  //TODO: close the wizard and finish
                  {t("common:finish")}
                </Button>
                <Button variant="secondary" onClick={onBack}>
                  {t("common:back")}
                </Button>
                <Button variant="link" onClick={onClose}>
                  {t("common:cancel")}
                </Button>
              </>
            );
          }
          // All the other steps buttons
          return (
            <>
              <Button onClick={onNext}>Next</Button>
              <Button variant="secondary" onClick={onBack}>
                Back
              </Button>
              <Button
                variant="link"
                onClick={() => {}} //TODO: validate last step and finish
              >
                {t("common:skipCustomizationAndFinish")}
              </Button>
              <Button variant="link" onClick={onClose}>
                {t("common:cancel")}
              </Button>
            </>
          );
        }}
      </WizardContextConsumer>
    </WizardFooter>
  );

  return (
    <Wizard
      title={title}
      description={description}
      height="100%"
      steps={steps}
      footer={footer}
    />
  );
};
