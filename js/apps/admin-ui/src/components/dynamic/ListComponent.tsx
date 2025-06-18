import {
  HelpItem,
  KeycloakSelect,
  SelectVariant,
} from "@keycloak/keycloak-ui-shared";
import { FormGroup, SelectOption } from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import type { ComponentProps } from "./components";

export const ListComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  options,
  required,
  isDisabled = false,
  convertToName,
}: ComponentProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext();
  const [open, setOpen] = useState(false);

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />}
      fieldId={name!}
      isRequired={required}
    >
      <Controller
        name={convertToName(name!)}
        data-testid={name}
        defaultValue={defaultValue || options?.[0] || ""}
        control={control}
        render={({ field }) => (
          <KeycloakSelect
            toggleId={name}
            isDisabled={isDisabled}
            onToggle={(toggle) => setOpen(toggle)}
            onSelect={(value) => {
              field.onChange(value as string);
              setOpen(false);
            }}
            selections={field.value}
            variant={SelectVariant.single}
            aria-label={t(label!)}
            isOpen={open}
          >
            {options?.map((option) => (
              <SelectOption
                selected={option === field.value}
                key={option}
                value={option}
              >
                {option}
              </SelectOption>
            ))}
          </KeycloakSelect>
        )}
      />
    </FormGroup>
  );
};
