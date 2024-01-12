import { FormGroup, ValidatedOptions } from "@patternfly/react-core";
import {
  Controller,
  ControllerProps,
  FieldPath,
  FieldValues,
  UseControllerProps,
  useFormContext,
} from "react-hook-form";
import { HelpItem } from "ui-shared";
import { TimeSelector, TimeSelectorProps } from "./TimeSelector";

export type NumberControlOption = {
  key: string;
  value: string;
};

export type TimeSelectorControlProps<
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
> = Omit<TimeSelectorProps, "name"> &
  UseControllerProps<T, P> & {
    name: string;
    label?: string;
    labelIcon?: string;
    controller: Omit<ControllerProps, "name" | "render">;
  };

export const TimeSelectorControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
>({
  name,
  label,
  controller,
  labelIcon,
  ...rest
}: TimeSelectorControlProps<T, P>) => {
  const {
    control,
    formState: { errors },
  } = useFormContext();
  return (
    <FormGroup
      isRequired={controller.rules?.required === true}
      label={label || name}
      fieldId={name}
      labelIcon={
        labelIcon ? (
          <HelpItem helpText={labelIcon} fieldLabelId={name} />
        ) : undefined
      }
      helperTextInvalid={errors[name]?.message as string}
      validated={
        errors[name] ? ValidatedOptions.error : ValidatedOptions.default
      }
    >
      <Controller
        {...controller}
        name={name}
        control={control}
        render={({ field }) => (
          <TimeSelector
            {...rest}
            id={name}
            data-testid={name}
            value={field.value}
            onChange={field.onChange}
            validated={
              errors[name] ? ValidatedOptions.error : ValidatedOptions.default
            }
          />
        )}
      />
    </FormGroup>
  );
};
