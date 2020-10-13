import React, { useState } from "react";
import {
  FormGroup,
  Form,
  Select,
  SelectVariant,
  SelectOption,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { Controller, UseFormMethods } from "react-hook-form";

import { useLoginProviders } from "../../context/server-info/ServerInfoProvider";
import { ClientDescription } from "../ClientDescription";

type GeneralSettingsProps = {
  form: UseFormMethods;
};

export const GeneralSettings = ({ form }: GeneralSettingsProps) => {
  const { t } = useTranslation();
  const { errors, control } = form;

  const providers = useLoginProviders();
  const [open, isOpen] = useState(false);

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
              aria-label={t("selectEncryptionType")}
              placeholderText={t("common:selectOne")}
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
      <ClientDescription form={form} />
    </Form>
  );
};
