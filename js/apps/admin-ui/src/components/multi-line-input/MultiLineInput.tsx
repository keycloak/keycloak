import {
  Button,
  ButtonVariant,
  InputGroup,
  TextInput,
  TextInputProps,
} from "@patternfly/react-core";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { Fragment, useEffect, useMemo } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

function stringToMultiline(value?: string): string[] {
  return typeof value === "string" ? value.split("##") : [];
}

function toStringValue(formValue: string[]): string {
  return formValue.join("##");
}

export type MultiLineInputProps = Omit<TextInputProps, "form"> & {
  name: string;
  addButtonLabel?: string;
  isDisabled?: boolean;
  defaultValue?: string[];
  stringify?: boolean;
};

export const MultiLineInput = ({
  name,
  addButtonLabel,
  isDisabled = false,
  defaultValue,
  stringify = false,
  id,
  ...rest
}: MultiLineInputProps) => {
  const { t } = useTranslation();
  const { register, setValue, control } = useFormContext();
  const value = useWatch({
    name,
    control,
    defaultValue: defaultValue || "",
  });

  const fields = useMemo<string[]>(() => {
    let values = stringify
      ? stringToMultiline(
          Array.isArray(value) && value.length === 1 ? value[0] : value,
        )
      : value;

    values =
      Array.isArray(values) && values.length !== 0
        ? values
        : (stringify
            ? stringToMultiline(defaultValue as string)
            : defaultValue) || [""];

    return values;
  }, [value]);

  const remove = (index: number) => {
    update([...fields.slice(0, index), ...fields.slice(index + 1)]);
  };

  const append = () => {
    update([...fields, ""]);
  };

  const updateValue = (index: number, value: string) => {
    update([...fields.slice(0, index), value, ...fields.slice(index + 1)]);
  };

  const update = (values: string[]) => {
    const fieldValue = values.flatMap((field) => field);
    setValue(name, stringify ? toStringValue(fieldValue) : fieldValue, {
      shouldDirty: true,
    });
  };

  useEffect(() => {
    register(name);
  }, [register]);

  return (
    <div id={id}>
      {fields.map((value, index) => (
        <Fragment key={index}>
          <InputGroup>
            <TextInput
              data-testid={name + index}
              onChange={(value) => updateValue(index, value)}
              name={`${name}.${index}.value`}
              value={value}
              isDisabled={isDisabled}
              {...rest}
            />
            <Button
              data-testid={"remove" + index}
              variant={ButtonVariant.link}
              onClick={() => remove(index)}
              tabIndex={-1}
              aria-label={t("common:remove")}
              isDisabled={fields.length === 1 || isDisabled}
            >
              <MinusCircleIcon />
            </Button>
          </InputGroup>
          {index === fields.length - 1 && (
            <Button
              variant={ButtonVariant.link}
              onClick={append}
              tabIndex={-1}
              aria-label={t("common:add")}
              data-testid="addValue"
              isDisabled={!value || isDisabled}
            >
              <PlusCircleIcon /> {t(addButtonLabel || "common:add")}
            </Button>
          )}
        </Fragment>
      ))}
    </div>
  );
};
