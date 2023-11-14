import {
  Button,
  ButtonVariant,
  InputGroup,
  TextInput,
  TextInputProps,
} from "@patternfly/react-core";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { Fragment, useEffect, useMemo } from "react";
import { FieldPath, UseFormReturn, useWatch } from "react-hook-form";
import { UserProfileFieldProps } from "./UserProfileFields";
import { UserProfileGroup } from "./UserProfileGroup";
import {
  TranslationFunction,
  UserFormFields,
  fieldName,
  labelAttribute,
} from "./utils";

export const MultiInputComponent = ({
  t,
  form,
  attribute,
  renderer,
}: UserProfileFieldProps) => (
  <UserProfileGroup t={t} form={form} attribute={attribute} renderer={renderer}>
    <MultiLineInput
      t={t}
      form={form}
      aria-label={labelAttribute(t, attribute)}
      name={fieldName(attribute.name)!}
      addButtonLabel={t("addMultivaluedLabel", {
        fieldLabel: labelAttribute(t, attribute),
      })}
    />
  </UserProfileGroup>
);

export type MultiLineInputProps = Omit<TextInputProps, "form"> & {
  t: TranslationFunction;
  name: FieldPath<UserFormFields>;
  form: UseFormReturn<UserFormFields>;
  addButtonLabel?: string;
  isDisabled?: boolean;
  defaultValue?: string[];
};

const MultiLineInput = ({
  t,
  name,
  form,
  addButtonLabel,
  isDisabled = false,
  defaultValue,
  id,
  ...rest
}: MultiLineInputProps) => {
  const { register, setValue, control } = form;
  const value = useWatch({
    name,
    control,
    defaultValue: defaultValue || "",
  });

  const fields = useMemo<string[]>(() => {
    return Array.isArray(value) && value.length !== 0
      ? value
      : defaultValue || [""];
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
    setValue(name, fieldValue, {
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
              aria-label={t("remove")}
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
              aria-label={t("add")}
              data-testid="addValue"
              isDisabled={!value || isDisabled}
            >
              <PlusCircleIcon /> {t(addButtonLabel || "add")}
            </Button>
          )}
        </Fragment>
      ))}
    </div>
  );
};
