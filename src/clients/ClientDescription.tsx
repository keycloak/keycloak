import React from "react";
import { FormGroup, TextInput } from "@patternfly/react-core";
import { FieldElement, ValidationRules, Ref } from "react-hook-form";
import { useTranslation } from "react-i18next";

type ClientDescriptionProps = {
  register<TFieldElement extends FieldElement>(
    rules?: ValidationRules
  ): (ref: (TFieldElement & Ref) | null) => void;
};

export const ClientDescription = ({ register }: ClientDescriptionProps) => {
  const { t } = useTranslation("clients");
  return (
    <>
      <FormGroup label={t("clientID")} fieldId="kc-client-id">
        <TextInput
          ref={register()}
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
