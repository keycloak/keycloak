import { SelectOption } from "@patternfly/react-core";
import { useState } from "react";
import { Controller, ControllerRenderProps } from "react-hook-form";
import { KeycloakSelect, SelectVariant } from "../select/KeycloakSelect";
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
  const [filter, setFilter] = useState("");
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
      field.onChange(value === field.value ? "" : value);
    }
  };

  const options =
    (attribute.validators?.options as Options | undefined)?.options || [];

  const optionLabel =
    (attribute.annotations?.["inputOptionLabels"] as OptionLabel) || {};
  const prefix = attribute.annotations?.[
    "inputOptionLabelsI18nPrefix"
  ] as string;

  const fetchLabel = (option: string) =>
    label(props.t, optionLabel[option], option, prefix);

  const convertOptions = (selected: string) =>
    options
      .filter((o) =>
        fetchLabel(o)!.toLowerCase().includes(filter.toLowerCase()),
      )
      .map((option) => (
        <SelectOption
          selected={selected === option}
          key={option}
          value={option}
        >
          {fetchLabel(option)}
        </SelectOption>
      ));

  return (
    <UserProfileGroup {...props}>
      <Controller
        name={fieldName(attribute.name)}
        defaultValue={attribute.defaultValue}
        control={form.control}
        render={({ field }) => (
          <KeycloakSelect
            toggleId={attribute.name}
            onToggle={(b) => setOpen(b)}
            onClear={() => setValue("", field)}
            onSelect={(value) => {
              const option = value.toString();
              setValue(option, field);
              if (!Array.isArray(field.value)) {
                setOpen(false);
              }
            }}
            selections={
              isMultiValue && Array.isArray(field.value)
                ? field.value.map((option) => fetchLabel(option))
                : fetchLabel(field.value)
            }
            variant={
              isMultiValue
                ? SelectVariant.typeaheadMulti
                : options.length >= 10
                  ? SelectVariant.typeahead
                  : SelectVariant.single
            }
            aria-label={t("selectOne")}
            isOpen={open}
            isDisabled={attribute.readOnly}
            onFilter={(value) => {
              setFilter(value);
              return convertOptions(field.value);
            }}
          >
            {convertOptions(field.value)}
          </KeycloakSelect>
        )}
      />
    </UserProfileGroup>
  );
};
