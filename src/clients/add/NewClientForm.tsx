import React, { useState } from "react";
import { useHistory } from "react-router-dom";
import {
  PageSection,
  Wizard,
  AlertVariant,
  WizardFooter,
  WizardContextConsumer,
  Button,
  WizardStepFunctionType,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";

import { GeneralSettings } from "./GeneralSettings";
import { CapabilityConfig } from "./CapabilityConfig";
import { useAlerts } from "../../components/alert/Alerts";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";

export const NewClientForm = () => {
  const { t } = useTranslation("clients");
  const { realm } = useRealm();
  const adminClient = useAdminClient();
  const history = useHistory();

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
  });
  const { addAlert } = useAlerts();
  const methods = useForm<ClientRepresentation>({ defaultValues: client });

  const save = async () => {
    try {
      const newClient = await adminClient.clients.create({ ...client });
      addAlert(t("createSuccess"), AlertVariant.success);
      history.push(`/${realm}/clients/${newClient.id}/settings`);
    } catch (error) {
      addAlert(t("createError", { error }), AlertVariant.danger);
    }
  };

  const forward = async (onNext: () => void) => {
    if (await methods.trigger()) {
      setClient({ ...client, ...methods.getValues() });
      setShowCapabilityConfig(true);
      onNext();
    }
  };

  const back = () => {
    setClient({ ...client, ...methods.getValues() });
    methods.reset({ ...client, ...methods.getValues() });
  };

  const onGoToStep = (newStep: { id?: string | number }) => {
    if (newStep.id === "generalSettings") {
      back();
    } else {
      forward(() => {});
    }
  };

  const Footer = () => (
    <WizardFooter>
      <WizardContextConsumer>
        {({ activeStep, onNext, onBack, onClose }) => {
          return (
            <>
              <Button
                variant="primary"
                type="submit"
                onClick={() => {
                  forward(onNext);
                }}
              >
                {activeStep.name === t("capabilityConfig")
                  ? t("common:save")
                  : t("common:next")}
              </Button>
              <Button
                variant="secondary"
                onClick={() => {
                  back();
                  onBack();
                }}
                isDisabled={activeStep.name === t("generalSettings")}
              >
                {t("common:back")}
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
            onClose={() => history.push(`/${realm}/clients`)}
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
};
