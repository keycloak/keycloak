import {
  MenuToggle,
  MenuToggleStatus,
  Select,
  SelectList,
  SelectOption,
} from "@patternfly/react-core";
import { get } from "lodash-es";
import { useState } from "react";
import {
  Controller,
  FieldPath,
  FieldValues,
  useFormContext,
} from "react-hook-form";
import { getRuleValue } from "../../utils/getRuleValue";
import { FormLabel } from "../FormLabel";
import {
  SelectControlProps,
  isSelectBasedOptions,
  isString,
  key,
} from "./SelectControl";

export const SingleSelectControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
>({
  id,
  name,
  label,
  options,
  selectedOptions = [],
  controller,
  labelIcon,
  isDisabled,
  onSelect,
  ...rest
}: SelectControlProps<T, P>) => {
  const {
    control,
    formState: { errors },
  } = useFormContext();
  const [open, setOpen] = useState(false);
  const required = getRuleValue(controller.rules?.required) === true;

  return (
    <FormLabel
      id={id}
      name={name}
      label={label}
      isRequired={required}
      error={get(errors, name)}
      labelIcon={labelIcon}
    >
      <Controller
        {...controller}
        name={name}
        control={control}
        render={({ field: { onChange, value } }) => (
          <Select
            {...rest}
            variant="default"
            onClick={() => setOpen(!open)}
            onOpenChange={() => setOpen(false)}
            selected={
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
            toggle={(ref) => (
              <MenuToggle
                id={id || name}
                ref={ref}
                onClick={() => setOpen(!open)}
                isExpanded={open}
                isFullWidth
                status={get(errors, name) ? MenuToggleStatus.danger : undefined}
                aria-label={label}
                isDisabled={isDisabled}
              >
                {isSelectBasedOptions(options)
                  ? options.find(
                      (o) =>
                        o.key === (Array.isArray(value) ? value[0] : value),
                    )?.value
                  : value}
              </MenuToggle>
            )}
            onSelect={(_event, v) => {
              const option = v?.toString()!;
              const convertedValue = Array.isArray(value) ? [option] : option;
              if (onSelect) {
                onSelect(convertedValue, onChange);
              } else {
                onChange(convertedValue);
              }
              setOpen(false);
            }}
            isOpen={open}
          >
            <SelectList data-testid={`select-${name}`}>
              {[...options, ...selectedOptions].map((option) => (
                <SelectOption key={key(option)} value={key(option)}>
                  {isString(option) ? option : option.value}
                </SelectOption>
              ))}
            </SelectList>
          </Select>
        )}
      />
    </FormLabel>
  );
};
