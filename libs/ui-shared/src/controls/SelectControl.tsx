import { useState } from "react";
import {
  Controller,
  ControllerProps,
  FieldValues,
  FieldPath,
  useFormContext,
  UseControllerProps,
} from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectProps,
  ValidatedOptions,
} from "@patternfly/react-core";

type Option = {
  key: string;
  value: string;
};

export type SelectControlProps<
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>
> = Omit<
  SelectProps,
  "name" | "onToggle" | "selections" | "onSelect" | "onClear" | "isOpen"
> &
  UseControllerProps<T, P> & {
    name: string;
    label?: string;
    options: string[] | Option[];
    controller: Omit<ControllerProps, "name" | "render">;
  };

export const SelectControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>
>({
  name,
  label,
  options,
  controller,
  ...rest
}: SelectControlProps<T, P>) => {
  const {
    control,
    formState: { errors },
  } = useFormContext();
  const [open, setOpen] = useState(false);
  return (
    <FormGroup
      isRequired={controller.rules?.required === true}
      label={label || name}
      fieldId={name}
      helperTextInvalid={errors[name]?.message as string}
      validated={
        errors[name] ? ValidatedOptions.error : ValidatedOptions.default
      }
    >
      <Controller
        {...controller}
        name={name}
        control={control}
        render={({ field: { onChange, value } }) => (
          <Select
            {...rest}
            toggleId={name}
            onToggle={(isOpen) => setOpen(isOpen)}
            selections={value}
            onSelect={(_, v) => {
              const option = v.toString();
              if (value.includes(option)) {
                onChange(value.filter((item: string) => item !== option));
              } else {
                onChange([...value, option]);
              }
            }}
            onClear={(event) => {
              event.stopPropagation();
              onChange([]);
            }}
            isOpen={open}
            validated={
              errors[name] ? ValidatedOptions.error : ValidatedOptions.default
            }
          >
            {options.map((option) => (
              <SelectOption
                key={typeof option === "string" ? option : option.key}
                value={typeof option === "string" ? option : option.key}
              >
                {typeof option === "string" ? option : option.value}
              </SelectOption>
            ))}
          </Select>
        )}
      />
    </FormGroup>
  );
};
