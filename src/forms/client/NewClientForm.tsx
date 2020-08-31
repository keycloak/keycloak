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
import { ClientRepresentation } from "../../model/client-model";
import { AlertPanel } from "../../components/alert/AlertPanel";
import { useAlerts } from "../../components/alert/Alerts";

export const NewClientForm = () => {
  const httpClient = useContext(HttpClientContext)!;
  const [client, setClient] = useState<ClientRepresentation>({
    protocol: "",
    clientId: "",
    name: "",
    description: "",
    publicClient: false,
    authorizationServicesEnabled: false,
  });
  const [add, alerts, hide] = useAlerts();

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

  const title = "Create client";
  return (
    <>
      <AlertPanel alerts={alerts} onCloseAlert={hide} />
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
              name: "General Settings",
              component: <Step1 onChange={handleInputChange} client={client} />,
            },
            {
              name: "Capability config",
              component: <Step2 onChange={handleInputChange} client={client} />,
              nextButtonText: "Save",
            },
          ]}
          onSave={save}
        />
      </PageSection>
    </>
  );
};
