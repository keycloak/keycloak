import { ValidatedOptions } from "@patternfly/react-core";
import {
  Select,
  SelectOption,
  SelectProps,
  SelectVariant,
} from "@patternfly/react-core/deprecated";
import { useState } from "react";
import {
  Controller,
  ControllerProps,
  FieldPath,
  FieldValues,
  UseControllerProps,
  useFormContext,
} from "react-hook-form";
import { FormLabel } from "./FormLabel";

export type SelectControlOption = {
  key: string;
  value: string;
};

type OptionType = string[] | SelectControlOption[];

export type SelectControlProps<
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
> = Omit<
  SelectProps,
  | "name"
  | "onToggle"
  | "selections"
  | "onSelect"
  | "onClear"
  | "isOpen"
  | "onFilter"
> &
  UseControllerProps<T, P> & {
    name: string;
    label?: string;
    options: OptionType;
    labelIcon?: string;
    controller: Omit<ControllerProps, "name" | "render">;
    onFilter?: (value: string) => void;
  };

const isString = (option: SelectControlOption | string): option is string =>
  typeof option === "string";
const key = (option: SelectControlOption | string) =>
  isString(option) ? option : option.key;

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
  onFilter,
  ...rest
}: SelectControlProps<T, P>) => {
  const {
    control,
    formState: { errors },
  } = useFormContext();
  const [open, setOpen] = useState(false);
  const convert = (prefix: string = "") => {
    const lowercasePrefix = prefix.toLowerCase();
    return options
      .filter((option) =>
        (isString(option) ? option : option.value)
          .toLowerCase()
          .startsWith(lowercasePrefix),
      )
      .map((option) => (
        <SelectOption key={key(option)} value={key(option)}>
          {isString(option) ? option : option.value}
        </SelectOption>
      ));
  };
  const isSelectBasedOptions = (
    options: OptionType,
  ): options is SelectControlOption[] => typeof options[0] !== "string";
  return (
    <FormLabel
      name={name}
      label={label}
      isRequired={!!controller.rules?.required}
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
            onToggle={(_event, isOpen) => setOpen(isOpen)}
            selections={
              isSelectBasedOptions(options)
                ? options
                    .filter((o) =>
                      Array.isArray(value)
                        ? value.includes(o.key)
                        : value === o.key,
                    )
                    .map((o) => o.value)
                : value
            }
            onSelect={(event, v) => {
              event.stopPropagation();
              if (variant.includes("multi") && Array.isArray(value)) {
                const option = v.toString();
                const key = isSelectBasedOptions(options)
                  ? options.find((o) => o.value === option)?.key
                  : option;
                if (value.includes(key)) {
                  onChange(value.filter((item: string) => item !== key));
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
            onFilter={(_, value) => {
              onFilter?.(value);
              return convert(value);
            }}
            isOpen={open}
            variant={variant}
            validated={
              errors[name] ? ValidatedOptions.error : ValidatedOptions.default
            }
          >
            {convert()}
          </Select>
        )}
      />
    </FormLabel>
  );
};
