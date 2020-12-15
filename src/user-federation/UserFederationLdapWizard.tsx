import {
  Button,
  Wizard,
  WizardContextConsumer,
  WizardFooter,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { LdapSettingsGeneral } from "./LdapSettingsGeneral";
import { LdapSettingsConnection } from "./LdapSettingsConnection";
import { LdapSettingsSearching } from "./LdapSettingsSearching";
import { LdapSettingsSynchronization } from "./LdapSettingsSynchronization";
import { LdapSettingsKerberosIntegration } from "./LdapSettingsKerberosIntegration";
import { LdapSettingsCache } from "./LdapSettingsCache";
import { LdapSettingsAdvanced } from "./LdapSettingsAdvanced";

export const UserFederationLdapWizard = () => {
  const { t } = useTranslation("user-federation");

  const steps = [
    {
      name: "Required settings",
      id: "ldapRequiredSettingsStep",
      component: <LdapSettingsGeneral />,
    },
    {
      name: "Connection settings",
      id: "ldapConnectionSettingsStep",
      component: <LdapSettingsConnection />,
    },
    {
      name: "Searching settings",
      id: "ldapSearchingSettingsStep",
      component: <LdapSettingsSearching />,
    },
    {
      name: "Synchronization settings",
      id: "ldapSynchronizationSettingsStep",
      component: <LdapSettingsSynchronization />,
    },
    {
      name: "KerberosIntegration settings",
      id: "ldapKerberosIntegrationSettingsStep",
      component: <LdapSettingsKerberosIntegration />,
    },
    {
      name: "Cache settings",
      id: "ldapCacheSettingsStep",
      component: <LdapSettingsCache />,
    },
    {
      name: "Advanced settings",
      id: "ldapAdvancedSettingsStep",
      component: <LdapSettingsAdvanced />,
    },
  ];

  const title = "Add LDAP user federation provider";

  const footer = (
    <WizardFooter>
      <WizardContextConsumer>
        {({
          activeStep,
          goToStepByName,
          goToStepById,
          onNext,
          onBack,
          onClose,
        }) => {
          // First step buttons
          if (activeStep.id == "ldapRequiredSettingsStep") {
            return (
              <>
                <Button variant="primary" type="submit" onClick={onNext}>
                  Next
                </Button>
                <Button
                  variant="secondary"
                  onClick={onBack}
                  className="pf-m-disabled"
                >
                  Back
                </Button>
                <Button variant="link" onClick={onClose}>
                  Cancel
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
                  Next
                </Button>
                <Button variant="secondary" onClick={onBack}>
                  Back
                </Button>
                <Button variant="link" onClick={onClose}>
                  Cancel
                </Button>
              </>
            );
          }
          // Last step buttons
          else if (activeStep.id == "ldapAdvancedSettingsStep") {
            return (
              <>
                <Button onClick={() => this.closeWizard}>Finish</Button>
                <Button variant="secondary" onClick={onBack}>
                  Back
                </Button>
                <Button variant="link" onClick={onClose}>
                  Cancel
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
                onClick={() => this.validateLastStep(onNext)}
              >
                Skip customization and finish
              </Button>
              <Button variant="link" onClick={onClose}>
                Cancel
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
      description="Text needed here"
      height="100%"
      steps={steps}
      footer={footer}
    />
  );
};
