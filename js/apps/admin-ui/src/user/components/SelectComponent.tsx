import { Select, SelectOption } from "@patternfly/react-core";
import { useState } from "react";
import { Controller, ControllerRenderProps, FieldPath } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { Options, UserProfileFieldProps } from "../UserProfileFields";
import { UserFormFields } from "../form-state";
import { fieldName, unWrap } from "../utils";
import { UserProfileGroup } from "./UserProfileGroup";
import { isRequiredAttribute } from "../utils/user-profile";

type OptionLabel = Record<string, string> | undefined;
export const SelectComponent = ({
  form,
  inputType,
  attribute,
}: UserProfileFieldProps) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const isRequired = isRequiredAttribute(attribute);
  const isMultiValue = inputType === "multiselect";

  const setValue = (
    value: string,
    field: ControllerRenderProps<UserFormFields>,
  ) => {
    if (isMultiValue) {
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
    (attribute.validators?.options as Options | undefined)?.options || [];

  const optionLabel = attribute.annotations?.[
    "inputOptionLabels"
  ] as OptionLabel;
  const label = (label: string) =>
    optionLabel ? t(unWrap(optionLabel[label])) : label;

  return (
    <UserProfileGroup form={form} attribute={attribute}>
      <Controller
        name={fieldName(attribute) as FieldPath<UserFormFields>}
        defaultValue=""
        control={form.control}
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
              field.value ? field.value : isMultiValue ? [] : t("choose")
            }
            variant={isMultiValue ? "typeaheadmulti" : "single"}
            aria-label={t("selectOne")}
            isOpen={open}
            isDisabled={attribute.readOnly}
            required={isRequired}
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
