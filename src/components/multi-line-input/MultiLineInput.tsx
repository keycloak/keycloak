import React, { Fragment, useEffect } from "react";
import { useFieldArray, useFormContext, useWatch } from "react-hook-form";
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
};

export const MultiLineInput = ({
  name,
  addButtonLabel,
  ...rest
}: MultiLineInputProps) => {
  const { t } = useTranslation();
  const { register, control, reset } = useFormContext();
  const { fields, append, remove } = useFieldArray({
    name,
    control,
  });
  const currentValues: { [name: string]: { value: string } } | undefined =
    useWatch({ control, name });

  useEffect(() => {
    reset({
      [name]: [{ value: "" }],
    });
  }, []);
  return (
    <>
      {fields.map(({ id, value }, index) => (
        <Fragment key={id}>
          <InputGroup>
            <TextInput
              id={id}
              ref={register()}
              name={`${name}[${index}].value`}
              defaultValue={value}
              {...rest}
            />
            <Button
              variant={ButtonVariant.link}
              onClick={() => remove(index)}
              tabIndex={-1}
              aria-label={t("common:remove")}
              isDisabled={index === fields.length - 1}
            >
              <MinusCircleIcon />
            </Button>
          </InputGroup>
          {index === fields.length - 1 && (
            <Button
              variant={ButtonVariant.link}
              onClick={() => append({})}
              tabIndex={-1}
              aria-label={t("common:add")}
              data-testid="addValue"
              // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
              isDisabled={rest.isDisabled || !currentValues?.[index]?.value}
            >
              <PlusCircleIcon /> {t(addButtonLabel || "common:add")}
            </Button>
          )}
        </Fragment>
      ))}
    </>
  );
};
