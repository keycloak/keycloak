import {
  Chip,
  ChipGroup,
  MenuToggle,
  Select,
  SelectList,
  SelectOption,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, ControllerRenderProps } from "react-hook-form";
import {
  OptionLabel,
  Options,
  UserProfileFieldProps,
} from "./UserProfileFields";
import { UserProfileGroup } from "./UserProfileGroup";
import { UserFormFields, fieldName, label } from "./utils";

export const SelectComponent = (props: UserProfileFieldProps) => {
  const { t, form, inputType, attribute } = props;
  const [open, setOpen] = useState(false);
  const isMultiValue = inputType === "multiselect";

  const setValue = (
    value: string,
    field: ControllerRenderProps<UserFormFields>,
  ) => {
    if (isMultiValue) {
      if (field.value.includes(value)) {
        field.onChange(field.value.filter((item: string) => item !== value));
      } else {
        if (Array.isArray(field.value)) {
          field.onChange([...field.value, value]);
        } else {
          field.onChange([value]);
        }
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
            toggle={(ref) => (
              <MenuToggle
                id={attribute.name}
                ref={ref}
                onClick={() => setOpen(!open)}
                isExpanded={open}
                isFullWidth
                isDisabled={attribute.readOnly}
              >
                {(isMultiValue && Array.isArray(field.value) ? (
                  <ChipGroup>
                    {field.value.map((selection, index: number) => (
                      <Chip
                        key={index}
                        onClick={(ev) => {
                          ev.stopPropagation();
                          setValue(selection, field);
                        }}
                      >
                        {selection}
                      </Chip>
                    ))}
                  </ChipGroup>
                ) : (
                  fetchLabel(field.value)
                )) || t("choose")}
              </MenuToggle>
            )}
            onSelect={(_, value) => {
              const option = value?.toString() || "";
              setValue(option, field);
              if (!isMultiValue) {
                setOpen(false);
              }
            }}
            selected={field.value}
            aria-label={t("selectOne")}
            isOpen={open}
          >
            <SelectList>
              {options.map((option) => (
                <SelectOption
                  selected={field.value === option}
                  key={option}
                  value={option}
                >
                  {fetchLabel(option)}
                </SelectOption>
              ))}
            </SelectList>
          </Select>
        )}
      />
    </UserProfileGroup>
  );
};
