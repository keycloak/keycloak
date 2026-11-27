import {
  NumberInput,
  NumberInputProps,
  ValidatedOptions,
} from "@patternfly/react-core";
import {
  Controller,
  ControllerProps,
  FieldPath,
  FieldValues,
  UseControllerProps,
  useFormContext,
} from "react-hook-form";

import { getRuleValue } from "../utils/getRuleValue";
import { FormLabel } from "./FormLabel";

/**
 * The form holds whatever the representation carries, and a config or attribute
 * map carries its numbers as strings. That breaks both steppers: PatternFly
 * substitutes 0 for a value that is not a number when deciding whether they are
 * in range, leaving minus permanently disabled against a `min` of 0, and
 * `value + 1` on a string appends rather than increments.
 *
 * Anything that is not a numeric string is handed on untouched, so an empty or
 * unparseable field still renders the way it did. The prop type does not cover
 * a stray string, but one could reach the input before this conversion too.
 */
const toNumber = (value: NumberInputProps["value"] | string) => {
  const converted =
    typeof value === "string" && value.trim() !== "" ? Number(value) : NaN;
  return Number.isNaN(converted)
    ? (value as NumberInputProps["value"])
    : converted;
};

export type NumberControlOption = {
  key: string;
  value: string;
};

export type NumberControlProps<
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
> = Omit<NumberInputProps, "name" | "isRequired" | "required"> &
  UseControllerProps<T, P> & {
    name: string;
    label?: string;
    labelIcon?: string;
    controller: Omit<ControllerProps, "name" | "render">;
  };

export const NumberControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
>({
  name,
  label,
  controller,
  labelIcon,
  ...rest
}: NumberControlProps<T, P>) => {
  const {
    control,
    formState: { errors },
  } = useFormContext();

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
        render={({ field }) => {
          const required = !!controller.rules?.required;
          const min = getRuleValue(controller.rules?.min);
          const value = toNumber(field.value ?? controller.defaultValue);
          const setValue = (newValue: number) =>
            field.onChange(
              min !== undefined ? Math.max(newValue, Number(min)) : newValue,
            );
          const step = (by: number) => setValue(Number(value) + by);

          return (
            <NumberInput
              {...rest}
              id={name}
              value={value}
              validated={
                errors[name] ? ValidatedOptions.error : ValidatedOptions.default
              }
              required={required}
              min={Number(min)}
              max={Number(controller.rules?.max)}
              onPlus={() => step(1)}
              onMinus={() => step(-1)}
              onChange={(event) => {
                const newValue = Number(event.currentTarget.value);
                setValue(!isNaN(newValue) ? newValue : controller.defaultValue);
              }}
            />
          );
        }}
      />
    </FormLabel>
  );
};
