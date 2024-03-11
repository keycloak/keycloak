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
  Select,
  SelectOption,
  SelectProps,
  SelectVariant,
  ValidatedOptions,
} from "@patternfly/react-core";
import { FormLabel } from "./FormLabel";

export type SelectControlOption = {
  key: string;
  value: string;
};

export type SelectControlProps<
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
> = Omit<
  SelectProps,
  "name" | "onToggle" | "selections" | "onSelect" | "onClear" | "isOpen"
> &
  UseControllerProps<T, P> & {
    name: string;
    label?: string;
    options: string[] | SelectControlOption[];
    labelIcon?: string;
    controller: Omit<ControllerProps, "name" | "render">;
  };

export const SelectControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
>({
  name,
  label,
  options,
  controller,
  variant = SelectVariant.single,
  labelIcon,
  ...rest
}: SelectControlProps<T, P>) => {
  const {
    control,
    formState: { errors },
  } = useFormContext();
  const [open, setOpen] = useState(false);
  return (
    <FormLabel
      name={name}
      label={label}
      isRequired={controller.rules?.required === true}
      error={errors[name]}
      labelIcon={labelIcon}
    >
      <Controller
        {...controller}
        name={name}
        control={control}
        render={({ field: { onChange, value } }) => (
          <Select
            {...rest}
            toggleId={name.slice(name.lastIndexOf(".") + 1)}
            onToggle={(isOpen) => setOpen(isOpen)}
            selections={
              typeof options[0] !== "string"
                ? (options as SelectControlOption[])
                    .filter((o) => value.includes(o.key))
                    .map((o) => o.value)
                : value
            }
            onSelect={(_, v) => {
              if (variant === "typeaheadmulti") {
                const option = v.toString();
                if (value.includes(option)) {
                  onChange(value.filter((item: string) => item !== option));
                } else {
                  onChange([...value, option]);
                }
              } else {
                onChange(v);
                setOpen(false);
              }
            }}
            onClear={
              variant !== SelectVariant.single
                ? (event) => {
                    event.stopPropagation();
                    onChange([]);
                  }
                : undefined
            }
            isOpen={open}
            variant={variant}
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
    </FormLabel>
  );
};
