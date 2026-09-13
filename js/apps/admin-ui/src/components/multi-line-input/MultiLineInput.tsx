import {
  Button,
  ButtonVariant,
  InputGroup,
  InputGroupItem,
  TextInput,
  TextInputProps,
} from "@patternfly/react-core";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { Fragment, useEffect, useMemo } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormErrorText } from "@keycloak/keycloak-ui-shared";

function stringToMultiline(value?: string): string[] {
  return typeof value === "string" ? value.split("##") : [""];
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
  isRequired?: boolean;
};

export const MultiLineInput = ({
  name,
  addButtonLabel,
  isDisabled = false,
  defaultValue,
  stringify = false,
  isRequired = false,
  id,
  ...rest
}: MultiLineInputProps) => {
  const { t } = useTranslation();
  const {
    register,
    getValues,
    setValue,
    control,
    formState: { errors },
  } = useFormContext();
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
      : Array.isArray(value)
        ? value
        : [value];

    if (!Array.isArray(values) || values.length === 0) {
      values = (stringify
        ? stringToMultiline(defaultValue as string)
        : defaultValue) || [""];
    }

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
    // Avoid persisting blank-only values as "" / [""]. Empty string attributes
    // (e.g. post.logout.redirect.uris) break client policy executors such as
    // secure-redirect-uris-enforcer used by oauth-2-1-for-public-client.
    const isBlank = fieldValue.every((v) => !v.trim());
    setValue(
      name,
      isBlank ? undefined : stringify ? toStringValue(fieldValue) : fieldValue,
      {
        shouldDirty: true,
        shouldValidate: true,
      },
    );
  };

  // Only seed the form when there is a non-empty default to persist (see #43949).
  // Seeding blank values caused empty attributes to be submitted on save (#51315).
  if (typeof getValues(name) === "undefined" && fields.some((v) => v.trim())) {
    update(fields);
  }

  useEffect(() => {
    // Normalize blank string values left by older UI versions so they are omitted
    // on save instead of being re-submitted as "".
    if (stringify && getValues(name) === "") {
      setValue(name, undefined, { shouldDirty: false, shouldValidate: false });
    }

    register(name, {
      validate: (value) =>
        isRequired &&
        (stringify ? stringToMultiline(value) : value || []).filter(
          (s: string) => s.trim(),
        ).length === 0
          ? t("required")
          : undefined,
    });
  }, [register]);

  const getError = () => {
    return name.split(".").reduce((record: any, key) => record?.[key], errors);
  };

  return (
    <div id={id}>
      {fields.map((value, index) => (
        <Fragment key={index}>
          <InputGroup>
            <InputGroupItem isFill>
              <TextInput
                data-testid={name + index}
                onChange={(_event, value) => updateValue(index, value)}
                name={`${name}.${index}.value`}
                value={value}
                isDisabled={isDisabled}
                {...rest}
              />
            </InputGroupItem>
            <InputGroupItem>
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
            </InputGroupItem>
          </InputGroup>
          {index === fields.length - 1 && (
            <>
              {getError() && <FormErrorText message={t("required")} />}
              <Button
                variant={ButtonVariant.link}
                onClick={append}
                tabIndex={-1}
                aria-label={t("add")}
                data-testid={`${name}-addValue`}
                isDisabled={!value || isDisabled}
              >
                <PlusCircleIcon /> {t(addButtonLabel || "add")}
              </Button>
            </>
          )}
        </Fragment>
      ))}
    </div>
  );
};
