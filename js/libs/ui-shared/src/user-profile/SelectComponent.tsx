import { Select, SelectOption } from "@patternfly/react-core/deprecated";
import { useState } from "react";
import { Controller, ControllerRenderProps } from "react-hook-form";
import {
  OptionLabel,
  Options,
  UserProfileFieldProps,
} from "./UserProfileFields";
import { UserProfileGroup } from "./UserProfileGroup";
import { UserFormFields, fieldName, isRequiredAttribute, label } from "./utils";

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

  const options =
    (attribute.validators?.options as Options | undefined)?.options || [];

  const optionLabel =
    (attribute.annotations?.["inputOptionLabels"] as OptionLabel) || {};
  const fetchLabel = (option: string) =>
    label(props.t, optionLabel[option], option);

  return (
    <UserProfileGroup {...props}>
      <Controller
        name={fieldName(attribute.name)}
        defaultValue=""
        control={form.control}
        render={({ field }) => (
          <Select
            toggleId={attribute.name}
            onToggle={(_event, b) => setOpen(b)}
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
            {["", ...options].map((option) => (
              <SelectOption
                selected={field.value === option}
                key={option}
                value={option}
              >
                {option ? fetchLabel(option) : t("choose")}
              </SelectOption>
            ))}
          </Select>
        )}
      />
    </UserProfileGroup>
  );
};
