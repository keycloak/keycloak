import { FormGroup, ValidatedOptions } from "@patternfly/react-core";
import {
  Controller,
  ControllerProps,
  FieldPath,
  FieldValues,
  UseControllerProps,
  useFormContext,
} from "react-hook-form";
import { FormErrorText, HelpItem } from "@keycloak/keycloak-ui-shared";
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

  const error = errors[name];

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
              error ? ValidatedOptions.error : ValidatedOptions.default
            }
          />
        )}
      />
      {error && (
        <FormErrorText
          data-testid={`${name}-helper`}
          message={error.message as string}
        />
      )}
    </FormGroup>
  );
};
