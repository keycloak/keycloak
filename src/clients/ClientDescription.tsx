import React, { FormEvent } from "react";
import { FormGroup, TextInput } from "@patternfly/react-core";

import { ClientRepresentation } from "./models/client-model";
import { useTranslation } from "react-i18next";

type ClientDescriptionProps = {
  onChange: (value: string, event: FormEvent<HTMLInputElement>) => void;
  client: ClientRepresentation;
};

export const ClientDescription = ({
  client,
  onChange,
}: ClientDescriptionProps) => {
  const { t } = useTranslation("clients");
  return (
    <>
      <FormGroup label={t("clientID")} fieldId="kc-client-id">
        <TextInput
          type="text"
          id="kc-client-id"
          name="clientId"
          value={client.clientId}
          onChange={onChange}
        />
      </FormGroup>
      <FormGroup label={t("name")} fieldId="kc-name">
        <TextInput
          type="text"
          id="kc-name"
          name="name"
          value={client.name}
          onChange={onChange}
        />
      </FormGroup>
      <FormGroup label={t("description")} fieldId="kc-description">
        <TextInput
          type="text"
          id="kc-description"
          name="description"
          value={client.description}
          onChange={onChange}
        />
      </FormGroup>
    </>
  );
};
