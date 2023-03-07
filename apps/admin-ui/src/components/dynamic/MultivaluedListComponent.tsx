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

export const MultiValuedListComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  options,
  isDisabled = false,
}: ComponentProps) => {
  const { t } = useTranslation("dynamic");
  const { control } = useFormContext();
  const [open, setOpen] = useState(false);

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem helpText={t(helpText!)} fieldLabelId={`dynamic:${label}`} />
      }
      fieldId={name!}
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
              expandedText: t("common:hide"),
              collapsedText: t("common:showRemaining"),
            }}
            variant={SelectVariant.typeaheadMulti}
            typeAheadAriaLabel="Select"
            onToggle={(isOpen) => setOpen(isOpen)}
            selections={field.value}
            onSelect={(_, v) => {
              const option = v.toString();
              if (field.value.includes(option)) {
                field.onChange(
                  field.value.filter((item: string) => item !== option)
                );
              } else {
                field.onChange([...field.value, option]);
              }
            }}
            onClear={(event) => {
              event.stopPropagation();
              field.onChange([]);
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
