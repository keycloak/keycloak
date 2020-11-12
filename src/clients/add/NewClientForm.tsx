import React, { useState } from "react";
import { useHistory } from "react-router-dom";
import {
  PageSection,
  Wizard,
  AlertVariant,
  WizardFooter,
  WizardContextConsumer,
  Button,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";

import { GeneralSettings } from "./GeneralSettings";
import { CapabilityConfig } from "./CapabilityConfig";
import { useAlerts } from "../../components/alert/Alerts";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";
import { useAdminClient } from "../../context/auth/AdminClient";

export const NewClientForm = () => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const history = useHistory();

  const [client, setClient] = useState<ClientRepresentation>({
    protocol: "",
    clientId: "",
    name: "",
    description: "",
    publicClient: false,
    authorizationServicesEnabled: false,
    serviceAccountsEnabled: false,
    implicitFlowEnabled: false,
    directAccessGrantsEnabled: false,
    standardFlowEnabled: false,
  });
  const { addAlert } = useAlerts();
  const methods = useForm<ClientRepresentation>({ defaultValues: client });

  const save = async () => {
    try {
      await adminClient.clients.create({ ...client });
      addAlert(t("createSuccess"), AlertVariant.success);
    } catch (error) {
      addAlert(t("createError", { error }), AlertVariant.danger);
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
                onClick={async () => {
                  if (await methods.trigger()) {
                    setClient({ ...client, ...methods.getValues() });
                    onNext();
                  }
                }}
              >
                {activeStep.name === t("capabilityConfig")
                  ? t("common:save")
                  : t("common:next")}
              </Button>
              <Button
                variant="secondary"
                onClick={() => {
                  setClient({ ...client, ...methods.getValues() });
                  methods.reset({ ...client, ...methods.getValues() });
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
        <Wizard
          onClose={() => history.push("/clients")}
          navAriaLabel={`${title} steps`}
          mainAriaLabel={`${title} content`}
          steps={[
            {
              name: t("generalSettings"),
              component: <GeneralSettings form={methods} />,
            },
            {
              name: t("capabilityConfig"),
              component: <CapabilityConfig form={methods} />,
            },
          ]}
          footer={<Footer />}
          onSave={() => save()}
        />
      </PageSection>
    </>
  );
};
