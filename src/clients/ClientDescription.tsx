import React from "react";
import { FormGroup, TextInput, ValidatedOptions } from "@patternfly/react-core";
import { UseFormMethods } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../components/form-access/FormAccess";

type ClientDescriptionProps = {
  form: UseFormMethods;
};

export const ClientDescription = ({ form }: ClientDescriptionProps) => {
  const { t } = useTranslation("clients");
  const { register, errors } = form;
  return (
    <FormAccess role="manage-clients" unWrap>
      <FormGroup
        label={t("clientID")}
        fieldId="kc-client-id"
        helperTextInvalid={t("common:required")}
        validated={
          errors.clientId ? ValidatedOptions.error : ValidatedOptions.default
        }
        isRequired
      >
        <TextInput
          ref={register({ required: true })}
          type="text"
          id="kc-client-id"
          name="clientId"
          validated={
            errors.clientId ? ValidatedOptions.error : ValidatedOptions.default
          }
        />
      </FormGroup>
      <FormGroup label={t("name")} fieldId="kc-name">
        <TextInput ref={register()} type="text" id="kc-name" name="name" />
      </FormGroup>
      <FormGroup label={t("description")} fieldId="kc-description">
        <TextInput
          ref={register()}
          type="text"
          id="kc-description"
          name="description"
        />
      </FormGroup>
    </FormAccess>
  );
};
