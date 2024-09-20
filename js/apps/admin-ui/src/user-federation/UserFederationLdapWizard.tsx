import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import {
  Button,
  useWizardContext,
  Wizard,
  WizardFooter,
  WizardFooterWrapper,
  WizardStep,
} from "@patternfly/react-core";
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

const UserFedLdapFooter = () => {
  const { t } = useTranslation();
  const { activeStep, goToNextStep, goToPrevStep, close } = useWizardContext();
  return (
    <WizardFooter
      activeStep={activeStep}
      onNext={goToNextStep}
      onBack={goToPrevStep}
      onClose={close}
      isBackDisabled={activeStep.index === 1}
      backButtonText={t("back")}
      nextButtonText={t("next")}
      cancelButtonText={t("cancel")}
    />
  );
};
const SkipCustomizationFooter = () => {
  const { goToNextStep, goToPrevStep, close } = useWizardContext();
  const { t } = useTranslation();
  return (
    <WizardFooterWrapper>
      <Button variant="secondary" onClick={goToPrevStep}>
        {t("back")}
      </Button>
      <Button variant="primary" type="submit" onClick={goToNextStep}>
        {t("next")}
      </Button>
      {/* TODO: validate last step and finish */}
      <Button variant="link">{t("skipCustomizationAndFinish")}</Button>
      <Button variant="link" onClick={close}>
        {t("cancel")}
      </Button>
    </WizardFooterWrapper>
  );
};
export const UserFederationLdapWizard = () => {
  const form = useForm<ComponentRepresentation>();
  const { t } = useTranslation();
  const isFeatureEnabled = useIsFeatureEnabled();

  return (
    <Wizard height="100%" footer={<UserFedLdapFooter />}>
      <WizardStep name={t("requiredSettings")} id="ldapRequiredSettingsStep">
        <LdapSettingsGeneral
          form={form}
          showSectionHeading
          showSectionDescription
        />
      </WizardStep>
      <WizardStep
        name={t("connectionAndAuthenticationSettings")}
        id="ldapConnectionSettingsStep"
      >
        <LdapSettingsConnection
          form={form}
          showSectionHeading
          showSectionDescription
        />
      </WizardStep>
      <WizardStep
        name={t("ldapSearchingAndUpdatingSettings")}
        id="ldapSearchingSettingsStep"
      >
        <LdapSettingsSearching
          form={form}
          showSectionHeading
          showSectionDescription
        />
      </WizardStep>
      <WizardStep
        name={t("synchronizationSettings")}
        id="ldapSynchronizationSettingsStep"
        footer={<SkipCustomizationFooter />}
      >
        <LdapSettingsSynchronization
          form={form}
          showSectionHeading
          showSectionDescription
        />
      </WizardStep>
      <WizardStep
        name={t("kerberosIntegration")}
        id="ldapKerberosIntegrationSettingsStep"
        isDisabled={!isFeatureEnabled(Feature.Kerberos)}
        footer={<SkipCustomizationFooter />}
      >
        <LdapSettingsKerberosIntegration
          form={form}
          showSectionHeading
          showSectionDescription
        />
      </WizardStep>
      <WizardStep
        name={t("cacheSettings")}
        id="ldapCacheSettingsStep"
        footer={<SkipCustomizationFooter />}
      >
        <SettingsCache form={form} showSectionHeading showSectionDescription />
      </WizardStep>
      <WizardStep
        name={t("advancedSettings")}
        id="ldapAdvancedSettingsStep"
        footer={{
          backButtonText: t("back"),
          nextButtonText: t("finish"),
          cancelButtonText: t("cancel"),
        }}
      >
        <LdapSettingsAdvanced
          form={form}
          showSectionHeading
          showSectionDescription
        />
      </WizardStep>
    </Wizard>
  );
};
