import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import {
  AlertVariant,
  Button,
  PageSection,
  Wizard,
  WizardContextConsumer,
  WizardFooter,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom-v5-compat";
import { useAlerts } from "../../components/alert/Alerts";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertFormValuesToObject } from "../../util";
import { FormFields } from "../ClientDetails";
import { toClient } from "../routes/Client";
import { toClients } from "../routes/Clients";
import { CapabilityConfig } from "./CapabilityConfig";
import { GeneralSettings } from "./GeneralSettings";

export default function NewClientForm() {
  const { t } = useTranslation("clients");
  const { realm } = useRealm();
  const { adminClient } = useAdminClient();
  const navigate = useNavigate();

  const [showCapabilityConfig, setShowCapabilityConfig] = useState(false);
  const [client, setClient] = useState<ClientRepresentation>({
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
  });
  const { addAlert, addError } = useAlerts();
  const methods = useForm<FormFields>({ defaultValues: client });
  const protocol = methods.watch("protocol");

  const save = async () => {
    try {
      const newClient = await adminClient.clients.create({
        ...client,
        clientId: client.clientId?.trim(),
      });
      addAlert(t("createSuccess"), AlertVariant.success);
      navigate(toClient({ realm, clientId: newClient.id, tab: "settings" }));
    } catch (error) {
      addError("clients:createError", error);
    }
  };

  const forward = async (onNext?: () => void) => {
    if (await methods.trigger()) {
      setClient({
        ...client,
        ...convertFormValuesToObject(methods.getValues()),
      });
      if (!isFinalStep()) {
        setShowCapabilityConfig(true);
      }
      onNext?.();
    }
  };

  const isFinalStep = () =>
    showCapabilityConfig || protocol !== "openid-connect";

  const back = () => {
    setClient({ ...client, ...convertFormValuesToObject(methods.getValues()) });
    methods.reset({
      ...client,
      ...convertFormValuesToObject(methods.getValues()),
    });
    setShowCapabilityConfig(false);
  };

  const onGoToStep = (newStep: { id?: string | number }) => {
    if (newStep.id === "generalSettings") {
      back();
    } else {
      forward();
    }
  };

  const Footer = () => (
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
              {isFinalStep() ? t("common:save") : t("common:next")}
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
              {t("common:back")}
            </Button>
            <Button data-testid="cancel" variant="link" onClick={onClose}>
              {t("common:cancel")}
            </Button>
          </>
        )}
      </WizardContextConsumer>
    </WizardFooter>
  );

  const title = t("createClient");
  return (
    <>
      <ViewHeader
        titleKey="clients:createClient"
        subKey="clients:clientsExplain"
      />
      <PageSection variant="light">
        <FormProvider {...methods}>
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
              ...(showCapabilityConfig
                ? [
                    {
                      id: "capabilityConfig",
                      name: t("capabilityConfig"),
                      component: (
                        <CapabilityConfig protocol={client.protocol} />
                      ),
                    },
                  ]
                : []),
            ]}
            footer={<Footer />}
            onSave={save}
            onGoToStep={onGoToStep}
          />
        </FormProvider>
      </PageSection>
    </>
  );
}
