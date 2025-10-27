import {
  AlertVariant,
  PageSection,
  useWizardContext,
  Wizard,
  WizardFooter,
  WizardStep,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertFormValuesToObject } from "../../util";
import { FormFields } from "../ClientDetails";
import { toClient } from "../routes/Client";
import { toClients } from "../routes/Clients";
import { CapabilityConfig } from "./CapabilityConfig";
import { GeneralSettings } from "./GeneralSettings";
import { LoginSettings } from "./LoginSettings";
import { useState } from "react";

const NewClientFooter = (newClientForm: any) => {
  const { t } = useTranslation();
  const { trigger } = newClientForm;
  const { activeStep, goToNextStep, goToPrevStep, close } = useWizardContext();

  const forward = async (onNext: () => void) => {
    if (!(await trigger())) {
      return;
    }
    onNext?.();
  };

  return (
    <WizardFooter
      activeStep={activeStep}
      onNext={() => forward(goToNextStep)}
      onBack={goToPrevStep}
      onClose={close}
      isBackDisabled={activeStep.index === 1}
      backButtonText={t("back")}
      nextButtonText={t("next")}
      cancelButtonText={t("cancel")}
    />
  );
};

export default function NewClientForm() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { realm } = useRealm();
  const navigate = useNavigate();
  const [saving, setSaving] = useState<boolean>(false);

  const { addAlert, addError } = useAlerts();
  const form = useForm<FormFields>({
    defaultValues: {
      protocol: "openid-connect",
      clientId: "",
      name: "",
      description: "",
      publicClient: true,
      authorizationServicesEnabled: false,
      serviceAccountsEnabled: false,
      implicitFlowEnabled: false,
      directAccessGrantsEnabled: false,
      standardFlowEnabled: true,
      frontchannelLogout: true,
      attributes: {
        saml_idp_initiated_sso_url_name: "",
      },
    },
  });
  const { getValues, watch } = form;
  const protocol = watch("protocol");

  const save = async () => {
    if (saving) return;
    setSaving(true);
    const client = convertFormValuesToObject(getValues());
    try {
      const newClient = await adminClient.clients.create({
        ...client,
        clientId: client.clientId?.trim(),
      });
      addAlert(t("createClientSuccess"), AlertVariant.success);
      navigate(toClient({ realm, clientId: newClient.id, tab: "settings" }));
    } catch (error) {
      addError("createClientError", error);
    } finally {
      setSaving(false);
    }
  };

  const title = t("createClient");
  return (
    <>
      <ViewHeader titleKey="createClient" subKey="clientsExplain" />
      <PageSection variant="light">
        <FormProvider {...form}>
          <Wizard
            onClose={() => navigate(toClients({ realm }))}
            navAriaLabel={`${title} steps`}
            onSave={save}
            isProgressive
            footer={<NewClientFooter {...form} />}
          >
            <WizardStep
              name={t("generalSettings")}
              id="generalSettings"
              key="generalSettings"
            >
              <GeneralSettings />
            </WizardStep>
            <WizardStep
              name={t("capabilityConfig")}
              id="capabilityConfig"
              key="capabilityConfig"
              isHidden={protocol === "saml"}
            >
              <CapabilityConfig protocol={protocol} />
            </WizardStep>
            <WizardStep
              name={t("loginSettings")}
              id="loginSettings"
              key="loginSettings"
              footer={{
                backButtonText: t("back"),
                nextButtonText: t("save"),
                cancelButtonText: t("cancel"),
              }}
            >
              <FormAccess isHorizontal role="manage-clients">
                <LoginSettings protocol={protocol} />
              </FormAccess>
            </WizardStep>
          </Wizard>
        </FormProvider>
      </PageSection>
    </>
  );
}
