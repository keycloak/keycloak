import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, UseFormMethods } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { sortProviders } from "../../util";

export type SignedJWTProps = {
  form: UseFormMethods;
};

export const SignedJWT = ({ form }: SignedJWTProps) => {
  const providers = sortProviders(
    useServerInfo().providers!.clientSignature.providers
  );
  const { t } = useTranslation("clients");

  const [open, isOpen] = useState(false);
  return (
    <>
      <FormGroup
        label={t("signatureAlgorithm")}
        fieldId="kc-signature-algorithm"
        labelIcon={
          <HelpItem
            helpText="clients-help:signature-algorithm"
            forLabel={t("signatureAlgorithm")}
            forID="kc-signature-algorithm"
          />
        }
      >
        <Controller
          name="attributes.token_endpoint_auth_signing_alg"
          defaultValue={providers[0]}
          control={form.control}
          render={({ onChange, value }) => (
            <Select
              toggleId="kc-signature-algorithm"
              onToggle={() => isOpen(!open)}
              onSelect={(_, value) => {
                onChange(value as string);
                isOpen(false);
              }}
              selections={value}
              variant={SelectVariant.single}
              aria-label={t("signatureAlgorithm")}
              isOpen={open}
            >
              {providers.map((option) => (
                <SelectOption
                  selected={option === value}
                  key={option}
                  value={option}
                />
              ))}
            </Select>
          )}
        />
      </FormGroup>
    </>
  );
};
