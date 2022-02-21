import React, { Fragment, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import {
  Button,
  ButtonVariant,
  FormGroup,
  InputGroup,
  TextInput,
} from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import type { ComponentProps } from "./components";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";

export const MultiValuedStringComponent = ({
  name,
  label,
  defaultValue,
  helpText,
  isDisabled = false,
}: ComponentProps) => {
  const { t } = useTranslation("dynamic");
  const fieldName = `config.${name}`;
  const { register, setValue, watch } = useFormContext();

  const fields = watch(fieldName, [defaultValue]);

  const remove = (id: number) => {
    fields.splice(id, 1);
    setValue(fieldName, [...fields]);
  };

  const append = () => {
    setValue(fieldName, [...fields, ""]);
  };

  useEffect(() => register(`config.${name}`), [register]);

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem helpText={t(helpText!)} fieldLabelId={`dynamic:${label}`} />
      }
      fieldId={name!}
    >
      {fields.map((value: string, index: number) => (
        <Fragment key={index}>
          <InputGroup>
            <TextInput
              id={fieldName + index}
              onChange={(value) => {
                fields[index] = value;
                setValue(fieldName, [...fields]);
              }}
              name={`${fieldName}[${index}]`}
              value={value}
              isDisabled={isDisabled}
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
              onClick={append}
              tabIndex={-1}
              aria-label={t("common:add")}
              data-testid="addValue"
              isDisabled={!value}
            >
              <PlusCircleIcon />{" "}
              {t("addMultivaluedLabel", {
                fieldLabel: t(label!).toLowerCase(),
              })}
            </Button>
          )}
        </Fragment>
      ))}
    </FormGroup>
  );
};
