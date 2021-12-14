import React, { useState } from "react";
import {
  FormGroup,
  Select,
  SelectVariant,
  SelectOption,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";

import { useLoginProviders } from "../../context/server-info/ServerInfoProvider";
import { ClientDescription } from "../ClientDescription";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { getProtocolName } from "../utils";

export const GeneralSettings = () => {
  const { t } = useTranslation("clients");
  const { errors, control } = useFormContext();

  const providers = useLoginProviders();
  const [open, isOpen] = useState(false);

  return (
    <FormAccess isHorizontal role="manage-clients">
      <FormGroup
        label={t("clientType")}
        fieldId="kc-type"
        validated={errors.protocol ? "error" : "default"}
        labelIcon={
          <HelpItem
            helpText="clients-help:clientType"
            fieldLabelId="clients:clientType"
          />
        }
      >
        <Controller
          name="protocol"
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <Select
              id="kc-type"
              onToggle={isOpen}
              onSelect={(_, value) => {
                onChange(value.toString());
                isOpen(false);
              }}
              selections={value}
              variant={SelectVariant.single}
              aria-label={t("selectEncryptionType")}
              isOpen={open}
            >
              {providers.map((option) => (
                <SelectOption
                  selected={option === value}
                  key={option}
                  value={option}
                  data-testid={`option-${option}`}
                >
                  {getProtocolName(t, option)}
                </SelectOption>
              ))}
            </Select>
          )}
        />
      </FormGroup>
      <ClientDescription />
    </FormAccess>
  );
};
