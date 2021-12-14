import React from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  FormGroup,
  TextArea,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";

import { HelpItem } from "../components/help-enabler/HelpItem";

import { FormAccess } from "../components/form-access/FormAccess";
import type { ClientForm } from "./ClientDetails";

export const ClientDescription = () => {
  const { t } = useTranslation("clients");
  const { register, errors } = useFormContext<ClientForm>();
  return (
    <FormAccess role="manage-clients" unWrap>
      <FormGroup
        labelIcon={
          <HelpItem helpText="clients-help:clientId" fieldLabelId="clientId" />
        }
        label={t("common:clientId")}
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
      <FormGroup
        labelIcon={
          <HelpItem helpText="clients-help:clientName" fieldLabelId="name" />
        }
        label={t("common:name")}
        fieldId="kc-name"
      >
        <TextInput ref={register()} type="text" id="kc-name" name="name" />
      </FormGroup>
      <FormGroup
        labelIcon={
          <HelpItem
            helpText="clients-help:description"
            fieldLabelId="description"
          />
        }
        label={t("common:description")}
        fieldId="kc-description"
        validated={
          errors.description ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={errors.description?.message}
      >
        <TextArea
          ref={register({
            maxLength: {
              value: 255,
              message: t("common:maxLength", { length: 255 }),
            },
          })}
          type="text"
          id="kc-description"
          name="description"
          validated={
            errors.description
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
        />
      </FormGroup>
    </FormAccess>
  );
};
