import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
import type { ComponentProps } from "./components";
import { convertToName } from "./DynamicComponents";

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
        control={control}
        defaultValue={defaultValue ? [defaultValue] : []}
        render={({ field }) => (
          <Select
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
            onSelect={(_, v) => {
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
            onClear={(event) => {
              event.stopPropagation();
              field.onChange(stringify ? "" : []);
            }}
            isOpen={open}
            aria-label={t(label!)}
          >
            {options?.map((option) => (
              <SelectOption key={option} value={option} />
            ))}
          </Select>
        )}
      />
    </FormGroup>
  );
};
