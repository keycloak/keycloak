import React, { useState, useContext } from "react";
import { useHistory } from "react-router-dom";
import {
  Text,
  PageSection,
  TextContent,
  Divider,
  Wizard,
  AlertVariant,
  WizardFooter,
  WizardContextConsumer,
  Button,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";

import { HttpClientContext } from "../../http-service/HttpClientContext";
import { Step1 } from "./Step1";
import { Step2 } from "./Step2";
import { ClientRepresentation } from "../models/client-model";
import { useAlerts } from "../../components/alert/Alerts";
import { RealmContext } from "../../components/realm-context/RealmContext";

export const NewClientForm = () => {
  const { t } = useTranslation("clients");
  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);
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
  const [add, Alerts] = useAlerts();
  const methods = useForm<ClientRepresentation>({ defaultValues: client });

  const save = async () => {
    try {
      await httpClient.doPost(`/admin/realms/${realm}/clients`, client);
      add("Client created", AlertVariant.success);
    } catch (error) {
      add(`Could not create client: '${error}'`, AlertVariant.danger);
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
      <Alerts />
      <PageSection variant="light">
        <TextContent>
          <Text component="h1">{title}</Text>
        </TextContent>
      </PageSection>
      <Divider />
      <PageSection variant="light">
        <Wizard
          onClose={() => history.push("/clients")}
          navAriaLabel={`${title} steps`}
          mainAriaLabel={`${title} content`}
          steps={[
            {
              name: t("generalSettings"),
              component: <Step1 form={methods} />,
            },
            {
              name: t("capabilityConfig"),
              component: <Step2 form={methods} />,
            },
          ]}
          footer={<Footer />}
          onSave={() => save()}
        />
      </PageSection>
    </>
  );
};
