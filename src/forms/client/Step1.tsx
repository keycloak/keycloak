import React, { useState, FormEvent, useEffect, useContext } from "react";
import {
  FormGroup,
  TextInput,
  Form,
  Select,
  SelectVariant,
  SelectOption,
} from "@patternfly/react-core";
import { HttpClientContext } from "../../http-service/HttpClientContext";
import { sortProvider } from "../../util";
import { ServerInfoRepresentation } from "../../model/server-info";
import { ClientRepresentation } from "../../model/client-model";

type Step1Props = {
  onChange: (value: string, event: FormEvent<HTMLInputElement>) => void;
  client: ClientRepresentation;
};

export const Step1 = ({ client, onChange }: Step1Props) => {
  const httpClient = useContext(HttpClientContext)!;

  const [providers, setProviders] = useState<string[]>([]);
  const [open, isOpen] = useState(false);

  useEffect(() => {
    (async () => {
      const response = await httpClient.doGet<ServerInfoRepresentation>(
        "/admin/serverinfo"
      );
      const providers = Object.entries(
        response.data!.providers["login-protocol"].providers
      );
      setProviders(["", ...new Map(providers.sort(sortProvider)).keys()]);
    })();
  }, []);

  return (
    <Form isHorizontal>
      <FormGroup label="Client Type" fieldId="kc-type" isRequired>
        <Select
          id="kc-type"
          required
          onToggle={() => isOpen(!open)}
          onSelect={(_, value) => {
            onChange(
              value as string,
              ({
                target: {
                  name: "protocol",
                },
              } as unknown) as FormEvent<HTMLInputElement>
            );
            isOpen(false);
          }}
          selections={client.protocol}
          variant={SelectVariant.single}
          aria-label="Select Encryption type"
          isOpen={open}
        >
          {providers.map((option) => (
            <SelectOption
              key={option}
              value={option || "Select an option"}
              isPlaceholder={option === ""}
            />
          ))}
        </Select>
      </FormGroup>
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
    </Form>
  );
};
