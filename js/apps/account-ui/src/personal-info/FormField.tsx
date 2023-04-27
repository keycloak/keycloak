import { FormGroup, Select, SelectOption } from "@patternfly/react-core";
import { TFuncKey } from "i18next";
import { get } from "lodash-es";
import { useState } from "react";
import { useFormContext, Controller } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { KeycloakTextInput } from "ui-shared";
import { UserProfileAttributeMetadata } from "../api/representations";
import { fieldName, isBundleKey, unWrap } from "./PersonalInfo";

type FormFieldProps = {
  attribute: UserProfileAttributeMetadata;
};

export const FormField = ({ attribute }: FormFieldProps) => {
  const { t } = useTranslation();
  const {
    formState: { errors },
    register,
    control,
  } = useFormContext();
  const [open, setOpen] = useState(false);
  const toggle = () => setOpen(!open);

  const isSelect = (attribute: UserProfileAttributeMetadata) =>
    Object.hasOwn(attribute.validators, "options");

  return (
    <FormGroup
      key={attribute.name}
      label={
        (isBundleKey(attribute.displayName)
          ? t(unWrap(attribute.displayName) as TFuncKey)
          : attribute.displayName) || attribute.name
      }
      fieldId={attribute.name}
      isRequired={attribute.required}
      validated={get(errors, fieldName(attribute.name)) ? "error" : "default"}
      helperTextInvalid={
        get(errors, fieldName(attribute.name))?.message as string
      }
    >
      {isSelect(attribute) ? (
        <Controller
          name={fieldName(attribute.name)}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              toggleId={attribute.name}
              onToggle={toggle}
              onSelect={(_, value) => {
                field.onChange(value.toString());
                toggle();
              }}
              selections={field.value}
              variant="single"
              aria-label={t("selectOne")}
              isOpen={open}
            >
              {[
                <SelectOption key="empty" value="">
                  {t("choose")}
                </SelectOption>,
                ...(
                  attribute.validators.options as { options: string[] }
                ).options.map((option) => (
                  <SelectOption
                    selected={field.value === option}
                    key={option}
                    value={option}
                  >
                    {option}
                  </SelectOption>
                )),
              ]}
            </Select>
          )}
        />
      ) : (
        <KeycloakTextInput
          id={attribute.name}
          {...register(fieldName(attribute.name))}
        />
      )}
    </FormGroup>
  );
};
