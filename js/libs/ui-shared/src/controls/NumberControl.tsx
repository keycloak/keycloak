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
import { FormLabel } from "./FormLabel";

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
          const min = controller.rules?.min;
          const value =
            field.value === 0 ? controller.defaultValue : field.value;
          const setValue = (newValue: number) =>
            field.onChange(min ? Math.max(newValue, Number(min)) : newValue);

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
              onPlus={() => setValue(value + 1)}
              onMinus={() => setValue(value - 1)}
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
