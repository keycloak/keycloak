import React, { useState, useEffect, useContext } from "react";
import {
  FormGroup,
  Form,
  Select,
  SelectVariant,
  SelectOption,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { Controller, UseFormMethods } from "react-hook-form";

import { HttpClientContext } from "../../http-service/HttpClientContext";
import { sortProvider } from "../../util";
import { ServerInfoRepresentation } from "../models/server-info";
import { ClientDescription } from "../ClientDescription";

type GeneralSettingsProps = {
  form: UseFormMethods;
};

export const GeneralSettings = ({ form }: GeneralSettingsProps) => {
  const httpClient = useContext(HttpClientContext)!;
  const { t } = useTranslation();
  const { errors, control, register } = form;

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
      <FormGroup
        label="Client Type"
        fieldId="kc-type"
        isRequired
        helperTextInvalid={t("common:required")}
        validated={errors.protocol ? "error" : "default"}
      >
        <Controller
          name="protocol"
          defaultValue=""
          control={control}
          rules={{ required: true }}
          render={({ onChange, value }) => (
            <Select
              id="kc-type"
              required
              onToggle={() => isOpen(!open)}
              onSelect={(_, value, isPlaceholder) => {
                onChange(isPlaceholder ? "" : (value as string));
                isOpen(false);
              }}
              selections={value}
              variant={SelectVariant.single}
              aria-label="Select Encryption type"
              isOpen={open}
            >
              {providers.map((option) => (
                <SelectOption
                  selected={option === value}
                  key={option}
                  value={option === "" ? "Select an option" : option}
                  isPlaceholder={option === ""}
                />
              ))}
            </Select>
          )}
        />
      </FormGroup>
      <ClientDescription form={form} />
    </Form>
  );
};
