import React, { useState, FormEvent, useEffect, useContext } from "react";
import {
  FormGroup,
  Form,
  Select,
  SelectVariant,
  SelectOption,
} from "@patternfly/react-core";

import { HttpClientContext } from "../../http-service/HttpClientContext";
import { sortProvider } from "../../util";
import { ServerInfoRepresentation } from "../models/server-info";
import { ClientRepresentation } from "../models/client-model";
import { ClientDescription } from "../ClientDescription";

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
      <ClientDescription onChange={onChange} client={client} />
    </Form>
  );
};
