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

function stringToMultiline(value?: string): string[] {
  return typeof value === "string" && value.length > 0 ? value.split("##") : [];
}

function toStringValue(formValue: string[]): string {
  return formValue.join("##");
}

export const MultiValuedListComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  options,
  isDisabled = false,
  stringify,
  required,
  convertToName,
  onSearch,
}: ComponentProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext();
  const [open, setOpen] = useState(false);

  function setSearch(value: string) {
    if (onSearch) {
      onSearch(value);
    }
  }

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />}
      fieldId={name!}
      isRequired={required}
    >
      <Controller
        name={convertToName(name!)}
        control={control}
        defaultValue={
          stringify ? defaultValue || "" : defaultValue ? [defaultValue] : []
        }
        render={({ field }) => (
          <KeycloakSelect
            toggleId={name}
            data-testid={name}
            isDisabled={isDisabled}
            chipGroupProps={{
              numChips: 3,
              expandedText: t("hide"),
              collapsedText: t("showRemaining"),
            }}
            variant={SelectVariant.typeaheadMulti}
            typeAheadAriaLabel="Select"
            onToggle={(isOpen) => setOpen(isOpen)}
            selections={
              stringify ? stringToMultiline(field.value) : field.value
            }
            onSelect={(v) => {
              const option = v.toString();
              const values = stringify
                ? stringToMultiline(field.value)
                : field.value;
              let newValue;
              if (values.includes(option)) {
                newValue = values.filter((item: string) => item !== option);
              } else {
                newValue = [...values, option];
              }
              field.onChange(stringify ? toStringValue(newValue) : newValue);
            }}
            onClear={() => {
              field.onChange(stringify ? "" : []);
            }}
            onFilter={(value) => setSearch(value)}
            isOpen={open}
            aria-label={t(label!)}
          >
            {options?.map((option) => (
              <SelectOption key={option} value={option}>
                {option}
              </SelectOption>
            ))}
          </KeycloakSelect>
        )}
      />
    </FormGroup>
  );
};
