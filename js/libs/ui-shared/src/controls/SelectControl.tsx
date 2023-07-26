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
  variant,
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
            selections={
              typeof options[0] !== "string"
                ? (options as SelectControlOption[]).find(
                    (o) => o.key === value[0],
                  )?.value || value
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
                onChange([v]);
                setOpen(false);
              }
            }}
            onClear={(event) => {
              event.stopPropagation();
              onChange([]);
            }}
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
