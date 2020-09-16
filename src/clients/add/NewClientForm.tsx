import React, { useState, FormEvent, useContext } from "react";
import {
  Text,
  PageSection,
  TextContent,
  Divider,
  Wizard,
  AlertVariant,
} from "@patternfly/react-core";
import { HttpClientContext } from "../../http-service/HttpClientContext";
import { Step1 } from "./Step1";
import { Step2 } from "./Step2";
import { ClientRepresentation } from "../models/client-model";
import { useAlerts } from "../../components/alert/Alerts";
import { useTranslation } from "react-i18next";

export const NewClientForm = () => {
  const { t } = useTranslation("clients");
  const httpClient = useContext(HttpClientContext)!;
  const [client, setClient] = useState<ClientRepresentation>({
    protocol: "",
    clientId: "",
    name: "",
    description: "",
    publicClient: false,
    authorizationServicesEnabled: false,
  });
  const [add, Alerts] = useAlerts();

  const save = async () => {
    try {
      await httpClient.doPost("/admin/realms/master/clients", client);
      add("Client created", AlertVariant.success);
    } catch (error) {
      add(`Could not create client: '${error}'`, AlertVariant.danger);
    }
  };

  const handleInputChange = (
    value: string | boolean,
    event: FormEvent<HTMLInputElement>
  ) => {
    const target = event.target;
    const name = (target as HTMLInputElement).name;

    setClient({
      ...client,
      [name]: value,
    });
  };

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
          navAriaLabel={`${title} steps`}
          mainAriaLabel={`${title} content`}
          steps={[
            {
              name: t("generalSettings"),
              component: <Step1 onChange={handleInputChange} client={client} />,
            },
            {
              name: t("capabilityConfig"),
              component: <Step2 onChange={handleInputChange} client={client} />,
              nextButtonText: t("common:save"),
            },
          ]}
          onSave={() => save()}
        />
      </PageSection>
    </>
  );
};
