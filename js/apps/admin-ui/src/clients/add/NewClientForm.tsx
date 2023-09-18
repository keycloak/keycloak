import { AlertVariant, Button, PageSection } from "@patternfly/react-core";
import {
  Wizard,
  WizardContextConsumer,
  WizardFooter,
} from "@patternfly/react-core/deprecated";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
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

export default function NewClientForm() {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const navigate = useNavigate();

  const [step, setStep] = useState(0);

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
      directAccessGrantsEnabled: true,
      standardFlowEnabled: true,
      frontchannelLogout: true,
      attributes: {
        saml_idp_initiated_sso_url_name: "",
      },
    },
  });
  const { getValues, watch, trigger } = form;
  const protocol = watch("protocol");

  const save = async () => {
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
    }
  };

  const forward = async (onNext?: () => void) => {
    if (!(await trigger())) {
      return;
    }
    if (!isFinalStep()) {
      setStep(step + 1);
    }
    onNext?.();
  };

  const isFinalStep = () =>
    protocol === "openid-connect" ? step === 2 : step === 1;

  const back = () => {
    setStep(step - 1);
  };

  const onGoToStep = (newStep: { id?: string | number }) => {
    if (newStep.id === "generalSettings") {
      setStep(0);
    } else if (newStep.id === "capabilityConfig") {
      setStep(1);
    } else {
      setStep(2);
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
            mainAriaLabel={`${title} content`}
            steps={[
              {
                id: "generalSettings",
                name: t("generalSettings"),
                component: <GeneralSettings />,
              },
              ...(protocol !== "saml"
                ? [
                    {
                      id: "capabilityConfig",
                      name: t("capabilityConfig"),
                      component: <CapabilityConfig protocol={protocol} />,
                      canJumpTo: step >= 1,
                    },
                  ]
                : []),
              {
                id: "loginSettings",
                name: t("loginSettings"),
                component: (
                  <FormAccess isHorizontal role="manage-clients">
                    <LoginSettings protocol={protocol} />
                  </FormAccess>
                ),
                canJumpTo: step >= 1,
              },
            ]}
            footer={
              <WizardFooter>
                <WizardContextConsumer>
                  {({ activeStep, onNext, onBack, onClose }) => (
                    <>
                      <Button
                        variant="primary"
                        data-testid={isFinalStep() ? "save" : "next"}
                        type="submit"
                        onClick={() => {
                          forward(onNext);
                        }}
                      >
                        {isFinalStep() ? t("save") : t("next")}
                      </Button>
                      <Button
                        variant="secondary"
                        data-testid="back"
                        onClick={() => {
                          back();
                          onBack();
                        }}
                        isDisabled={activeStep.name === t("generalSettings")}
                      >
                        {t("back")}
                      </Button>
                      <Button
                        data-testid="cancel"
                        variant="link"
                        onClick={onClose}
                      >
                        {t("cancel")}
                      </Button>
                    </>
                  )}
                </WizardContextConsumer>
              </WizardFooter>
            }
            onSave={save}
            onGoToStep={onGoToStep}
          />
        </FormProvider>
      </PageSection>
    </>
  );
}
