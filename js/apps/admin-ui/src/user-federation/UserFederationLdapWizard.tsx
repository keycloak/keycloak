import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { Button } from "@patternfly/react-core";
import {
  Wizard,
  WizardContextConsumer,
  WizardFooter,
} from "@patternfly/react-core/deprecated";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";
import { LdapSettingsAdvanced } from "./ldap/LdapSettingsAdvanced";
import { LdapSettingsConnection } from "./ldap/LdapSettingsConnection";
import { LdapSettingsGeneral } from "./ldap/LdapSettingsGeneral";
import { LdapSettingsKerberosIntegration } from "./ldap/LdapSettingsKerberosIntegration";
import { LdapSettingsSearching } from "./ldap/LdapSettingsSearching";
import { LdapSettingsSynchronization } from "./ldap/LdapSettingsSynchronization";
import { SettingsCache } from "./shared/SettingsCache";

export const UserFederationLdapWizard = () => {
  const form = useForm<ComponentRepresentation>();
  const { t } = useTranslation();
  const isFeatureEnabled = useIsFeatureEnabled();

  const steps = [
    {
      name: t("requiredSettings"),
      id: "ldapRequiredSettingsStep",
      component: (
        <LdapSettingsGeneral
          form={form}
          showSectionHeading
          showSectionDescription
        />
      ),
    },
    {
      name: t("connectionAndAuthenticationSettings"),
      id: "ldapConnectionSettingsStep",
      component: (
        <LdapSettingsConnection
          form={form}
          showSectionHeading
          showSectionDescription
        />
      ),
    },
    {
      name: t("ldapSearchingAndUpdatingSettings"),
      id: "ldapSearchingSettingsStep",
      component: (
        <LdapSettingsSearching
          form={form}
          showSectionHeading
          showSectionDescription
        />
      ),
    },
    {
      name: t("synchronizationSettings"),
      id: "ldapSynchronizationSettingsStep",
      component: (
        <LdapSettingsSynchronization
          form={form}
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
          form={form}
          showSectionHeading
          showSectionDescription
        />
      ),
      isDisabled: !isFeatureEnabled(Feature.Kerberos),
    },
    {
      name: t("cacheSettings"),
      id: "ldapCacheSettingsStep",
      component: (
        <SettingsCache form={form} showSectionHeading showSectionDescription />
      ),
    },
    {
      name: t("advancedSettings"),
      id: "ldapAdvancedSettingsStep",
      component: (
        <LdapSettingsAdvanced
          form={form}
          showSectionHeading
          showSectionDescription
        />
      ),
    },
  ];

  const footer = (
    <WizardFooter>
      <WizardContextConsumer>
        {({ activeStep, onNext, onBack, onClose }) => {
          // First step buttons
          if (activeStep.id === "ldapRequiredSettingsStep") {
            return (
              <>
                <Button variant="primary" type="submit" onClick={onNext}>
                  {t("next")}
                </Button>
                <Button
                  variant="secondary"
                  onClick={onBack}
                  className="pf-m-disabled"
                >
                  {t("back")}
                </Button>
                <Button variant="link" onClick={onClose}>
                  {t("cancel")}
                </Button>
              </>
            );
          }
          // Other required step buttons
          else if (
            activeStep.id === "ldapConnectionSettingsStep" ||
            activeStep.id === "ldapSearchingSettingsStep"
          ) {
            return (
              <>
                <Button variant="primary" type="submit" onClick={onNext}>
                  {t("next")}
                </Button>
                <Button variant="secondary" onClick={onBack}>
                  {t("back")}
                </Button>
                <Button variant="link" onClick={onClose}>
                  {t("cancel")}
                </Button>
              </>
            );
          }
          // Last step buttons
          else if (activeStep.id === "ldapAdvancedSettingsStep") {
            return (
              <>
                {/* TODO: close the wizard and finish */}
                <Button>{t("finish")}</Button>
                <Button variant="secondary" onClick={onBack}>
                  {t("back")}
                </Button>
                <Button variant="link" onClick={onClose}>
                  {t("cancel")}
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
              {/* TODO: validate last step and finish */}
              <Button variant="link">{t("skipCustomizationAndFinish")}</Button>
              <Button variant="link" onClick={onClose}>
                {t("cancel")}
              </Button>
            </>
          );
        }}
      </WizardContextConsumer>
    </WizardFooter>
  );

  return (
    <Wizard
      // Because this is an inline wizard, this title and description should be put into the page. Specifying them here causes the wizard component to make a header that would be used on a modal.
      // title={t("addLdapWizardTitle")}
      // description={helpText("addLdapWizardDescription")}
      height="100%"
      steps={steps}
      footer={footer}
    />
  );
};
