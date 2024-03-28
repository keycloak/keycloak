import { Select, SelectOption } from "@patternfly/react-core";
import { useState } from "react";
import { Controller, ControllerRenderProps } from "react-hook-form";
import {
  OptionLabel,
  Options,
  UserProfileFieldProps,
} from "./UserProfileFields";
import { UserProfileGroup } from "./UserProfileGroup";
import {
  UserFormFields,
  fieldName,
  isRequiredAttribute,
  unWrap,
} from "./utils";

export const SelectComponent = (props: UserProfileFieldProps) => {
  const { t, form, inputType, attribute } = props;
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

  const optionLabel = attribute.annotations?.[
    "inputOptionLabels"
  ] as OptionLabel;
  const label = (label: string) =>
    optionLabel ? t(unWrap(optionLabel[label])) : label;

  let options =
    (attribute.validators?.options as Options | undefined)?.options || [];

  options = options.sort((a, b) => {
    return label(a).localeCompare(label(b));
  });

  return (
    <UserProfileGroup {...props}>
      <Controller
        name={fieldName(attribute.name)}
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
            variant={
              isMultiValue
                ? "typeaheadmulti"
                : options.length >= 10
                  ? "typeahead"
                  : "single"
            }
            aria-label={t("selectOne")}
            isOpen={open}
            isDisabled={attribute.readOnly}
            required={isRequired}
          >
            {["", ...options].map((option) => (
              <SelectOption
                selected={field.value === option}
                key={option}
                value={option}
              >
                {option ? label(option) : t("choose")}
              </SelectOption>
            ))}
          </Select>
        )}
      />
    </UserProfileGroup>
  );
};
