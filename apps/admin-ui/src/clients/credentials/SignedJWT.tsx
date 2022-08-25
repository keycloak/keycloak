import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { convertAttributeNameToForm, sortProviders } from "../../util";

export const SignedJWT = () => {
  const { control } = useFormContext();
  const providers = sortProviders(
    useServerInfo().providers!.clientSignature.providers
  );
  const { t } = useTranslation("clients");

  const [open, isOpen] = useState(false);
  return (
    <FormGroup
      label={t("signatureAlgorithm")}
      fieldId="kc-signature-algorithm"
      labelIcon={
        <HelpItem
          helpText="clients-help:signature-algorithm"
          fieldLabelId="clients:signatureAlgorithm"
        />
      }
    >
      <Controller
        name={convertAttributeNameToForm(
          "attributes.token.endpoint.auth.signing.alg"
        )}
        defaultValue=""
        control={control}
        render={({ onChange, value }) => (
          <Select
            maxHeight={200}
            toggleId="kc-signature-algorithm"
            onToggle={isOpen}
            onSelect={(_, value) => {
              onChange(value.toString());
              isOpen(false);
            }}
            selections={value || t("anyAlgorithm")}
            variant={SelectVariant.single}
            aria-label={t("signatureAlgorithm")}
            isOpen={open}
          >
            <SelectOption selected={value === ""} key="any" value="">
              {t("anyAlgorithm")}
            </SelectOption>
            <>
              {providers.map((option) => (
                <SelectOption
                  selected={option === value}
                  key={option}
                  value={option}
                />
              ))}
            </>
          </Select>
        )}
      />
    </FormGroup>
  );
};
