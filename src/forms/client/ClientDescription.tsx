import React, { FormEvent } from "react";
import { FormGroup, TextInput } from "@patternfly/react-core";

import { ClientRepresentation } from "../../model/client-model";

type ClientDescriptionProps = {
  onChange: (value: string, event: FormEvent<HTMLInputElement>) => void;
  client: ClientRepresentation;
};

export const ClientDescription = ({
  client,
  onChange,
}: ClientDescriptionProps) => {
  return (
    <>
      <FormGroup label="Client ID" fieldId="kc-client-id">
        <TextInput
          type="text"
          id="kc-client-id"
          name="clientId"
          value={client.clientId}
          onChange={onChange}
        />
      </FormGroup>
      <FormGroup label="Name" fieldId="kc-name">
        <TextInput
          type="text"
          id="kc-name"
          name="name"
          value={client.name}
          onChange={onChange}
        />
      </FormGroup>
      <FormGroup label="Description" fieldId="kc-description">
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
