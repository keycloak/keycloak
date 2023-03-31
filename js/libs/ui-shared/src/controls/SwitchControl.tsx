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
  P extends FieldPath<T> = FieldPath<T>
> = SwitchProps &
  UseControllerProps<T, P> & {
    name: string;
    label?: string;
    labelIcon?: string;
    labelOn: string;
    labelOff: string;
  };

export const SwitchControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>
>(
  props: SwitchControlProps<T, P>
) => {
  const defaultValue = props.defaultValue ?? (false as PathValue<T, P>);
  const { control } = useFormContext();
  return (
    <FormLabel
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
            id={props.name}
            data-testid={props.name}
            label={props.labelOn}
            labelOff={props.labelOff}
            isChecked={value}
            onChange={(checked) => onChange(checked)}
          />
        )}
      />
    </FormLabel>
  );
};
