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
import { HelpItem } from "ui-shared";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";

type SignedJWTProps = {
  clientAuthenticatorType: string;
};

export const SignedJWT = ({ clientAuthenticatorType }: SignedJWTProps) => {
  const { control } = useFormContext();
  const { cryptoInfo } = useServerInfo();
  const providers =
    clientAuthenticatorType === "client-jwt"
      ? cryptoInfo?.clientSignatureAsymmetricAlgorithms ?? []
      : cryptoInfo?.clientSignatureSymmetricAlgorithms ?? [];

  const { t } = useTranslation();

  const [open, isOpen] = useState(false);
  return (
    <FormGroup
      label={t("signatureAlgorithm")}
      fieldId="kc-signature-algorithm"
      labelIcon={
        <HelpItem
          helpText={t("signatureAlgorithmHelp")}
          fieldLabelId="signatureAlgorithm"
        />
      }
    >
      <Controller
        name={convertAttributeNameToForm<FormFields>(
          "attributes.token.endpoint.auth.signing.alg",
        )}
        defaultValue=""
        control={control}
        render={({ field }) => (
          <Select
            maxHeight={200}
            toggleId="kc-signature-algorithm"
            onToggle={isOpen}
            onSelect={(_, value) => {
              field.onChange(value.toString());
              isOpen(false);
            }}
            selections={field.value || t("anyAlgorithm")}
            variant={SelectVariant.single}
            aria-label={t("signatureAlgorithm")}
            isOpen={open}
          >
            <SelectOption selected={field.value === ""} key="any" value="">
              {t("anyAlgorithm")}
            </SelectOption>
            <>
              {providers.map((option) => (
                <SelectOption
                  selected={option === field.value}
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
