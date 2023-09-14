import { Select, SelectOption } from "@patternfly/react-core";
import { useState } from "react";
import {
  Controller,
  useFormContext,
  ControllerRenderProps,
  FieldValues,
} from "react-hook-form";
import { useTranslation } from "react-i18next";

import { Options } from "../UserProfileFields";
import { fieldName, unWrap } from "../utils";
import { UserProfileFieldsProps, UserProfileGroup } from "./UserProfileGroup";

type OptionLabel = Record<string, string> | undefined;
export const SelectComponent = (attribute: UserProfileFieldsProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext();
  const [open, setOpen] = useState(false);

  const isMultiValue = (field: ControllerRenderProps<FieldValues, string>) => {
    return (
      attribute.annotations?.["inputType"] === "multiselect" ||
      Array.isArray(field.value)
    );
  };

  const setValue = (
    value: string,
    field: ControllerRenderProps<FieldValues, string>,
  ) => {
    if (isMultiValue(field)) {
      if (field.value.includes(value)) {
        field.onChange(field.value.filter((item: string) => item !== value));
      } else {
        field.onChange([...field.value, value]);
      }
    } else {
      field.onChange(value);
    }
  };

  const options =
    (attribute.validations?.options as Options | undefined)?.options || [];

  const optionLabel = attribute.annotations?.[
    "inputOptionLabels"
  ] as OptionLabel;
  const label = (label: string) =>
    optionLabel ? t(unWrap(optionLabel[label])) : label;

  return (
    <UserProfileGroup {...attribute}>
      <Controller
        name={fieldName(attribute)}
        defaultValue=""
        control={control}
        render={({ field }) => (
          <Select
            toggleId={attribute.name}
            onToggle={(b) => setOpen(b)}
            isCreatable
            onCreateOption={(value) => setValue(value, field)}
            onSelect={(_, value) => {
              const option = value.toString();
              setValue(option, field);
              if (!Array.isArray(field.value)) {
                setOpen(false);
              }
            }}
            selections={
              field.value ? field.value : isMultiValue(field) ? [] : t("choose")
            }
            variant={isMultiValue(field) ? "typeaheadmulti" : "single"}
            aria-label={t("selectOne")}
            isOpen={open}
            readOnly={attribute.readOnly}
          >
            {options.map((option) => (
              <SelectOption
                selected={field.value === option}
                key={option}
                value={option}
              >
                {label(option)}
              </SelectOption>
            ))}
          </Select>
        )}
      />
    </UserProfileGroup>
  );
};
