import React from "react";
import { FormGroup, TextInput } from "@patternfly/react-core";
import { UseFormMethods } from "react-hook-form";
import { useTranslation } from "react-i18next";

type ClientDescriptionProps = {
  form: UseFormMethods;
};

export const ClientDescription = ({ form }: ClientDescriptionProps) => {
  const { t } = useTranslation("clients");
  const { register, errors } = form;
  return (
    <>
      <FormGroup
        label={t("clientID")}
        fieldId="kc-client-id"
        helperTextInvalid={t("common:required")}
        validated={errors.clientId ? "error" : "default"}
        isRequired
      >
        <TextInput
          ref={register({ required: true })}
          type="text"
          id="kc-client-id"
          name="clientId"
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
    </>
  );
};
