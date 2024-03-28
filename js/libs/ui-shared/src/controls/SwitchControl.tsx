import {
  Controller,
  FieldValues,
  FieldPath,
  UseControllerProps,
  PathValue,
  useFormContext,
} from "react-hook-form";
import { SwitchProps, Switch } from "@patternfly/react-core";
import { FormLabel } from "./FormLabel";

export type SwitchControlProps<
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
> = Omit<SwitchProps, "name" | "defaultValue" | "ref"> &
  UseControllerProps<T, P> & {
    name: string;
    label?: string;
    labelIcon?: string;
    labelOn: string;
    labelOff: string;
    stringify?: boolean;
  };

export const SwitchControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
>(
  props: SwitchControlProps<T, P>,
) => {
  const fallbackValue = props.stringify ? "false" : false;
  const defaultValue = props.defaultValue ?? (fallbackValue as PathValue<T, P>);
  const { control } = useFormContext();
  return (
    <FormLabel
      hasNoPaddingTop
      name={props.name}
      isRequired={props.rules?.required === true}
      label={props.label}
      labelIcon={props.labelIcon}
    >
      <Controller
        control={control}
        name={props.name}
        defaultValue={defaultValue}
        render={({ field: { onChange, value } }) => (
          <Switch
            {...props}
            id={props.name}
            data-testid={props.name}
            label={props.labelOn}
            isChecked={props.stringify ? value === "true" : value}
            onChange={(e, checked) => {
              const value = props.stringify ? checked.toString() : checked;
              props.onChange?.(e, checked);
              onChange(value);
            }}
          />
        )}
      />
    </FormLabel>
  );
};
