import { Fragment, useEffect } from "react";
import { useFormContext } from "react-hook-form";
import {
  TextInput,
  Button,
  ButtonVariant,
  TextInputProps,
  InputGroup,
} from "@patternfly/react-core";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";

export type MultiLineInputProps = Omit<TextInputProps, "form"> & {
  name: string;
  addButtonLabel?: string;
  isDisabled?: boolean;
  defaultValue?: string[];
};

export const MultiLineInput = ({
  name,
  addButtonLabel,
  isDisabled = false,
  defaultValue,
  ...rest
}: MultiLineInputProps) => {
  const { t } = useTranslation();
  const { register, watch, setValue } = useFormContext();

  const value = watch(name, defaultValue);
  const fields = Array.isArray(value) && value.length !== 0 ? value : [""];

  const remove = (index: number) => {
    setValue(name, [...fields.slice(0, index), ...fields.slice(index + 1)]);
  };

  const append = () => {
    setValue(name, [...fields, ""]);
  };

  useEffect(() => register(name), [register]);

  return (
    <>
      {fields.map((value: string, index: number) => (
        <Fragment key={index}>
          <InputGroup>
            <TextInput
              id={name + index}
              onChange={(value) => {
                setValue(name, [
                  ...fields.slice(0, index),
                  value,
                  ...fields.slice(index + 1),
                ]);
              }}
              name={`${name}[${index}]`}
              value={value}
              isDisabled={isDisabled}
              {...rest}
            />
            <Button
              variant={ButtonVariant.link}
              onClick={() => remove(index)}
              tabIndex={-1}
              aria-label={t("common:remove")}
              isDisabled={fields.length === 1}
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
              isDisabled={!value}
            >
              <PlusCircleIcon /> {t(addButtonLabel || "common:add")}
            </Button>
          )}
        </Fragment>
      ))}
    </>
  );
};
