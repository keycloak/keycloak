import { ActionGroup, Button, Form } from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAlerts } from "ui-shared";

import { getPersonalInfo, savePersonalInfo } from "../api/methods";
import {
  UserProfileMetadata,
  UserRepresentation,
} from "../api/representations";
import { Page } from "../components/page/Page";
import { usePromise } from "../utils/usePromise";
import { FormField } from "./FormField";

type FieldError = {
  field: string;
  errorMessage: string;
  params: string[];
};

const ROOT_ATTRIBUTES = ["username", "firstName", "lastName", "email"];
export const isBundleKey = (key?: string) => key?.includes("${");
export const unWrap = (key: string) => key.substring(2, key.length - 1);
export const isRootAttribute = (attr?: string) =>
  attr && ROOT_ATTRIBUTES.includes(attr);
export const fieldName = (name: string) =>
  `${isRootAttribute(name) ? "" : "attributes."}${name}`;

const PersonalInfo = () => {
  const { t } = useTranslation();
  const [userProfileMetadata, setUserProfileMetadata] =
    useState<UserProfileMetadata>();
  const form = useForm<UserRepresentation>({ mode: "onChange" });
  const { handleSubmit, reset, setError } = form;
  const { addAlert, addError } = useAlerts();

  usePromise(
    (signal) => getPersonalInfo({ signal }),
    (personalInfo) => {
      setUserProfileMetadata(personalInfo.userProfileMetadata);
      reset(personalInfo);
    }
  );

  const onSubmit = async (user: UserRepresentation) => {
    try {
      await savePersonalInfo(user);
      addAlert(t("accountUpdatedMessage"));
    } catch (error) {
      addError(t("accountUpdatedError").toString());

      (error as FieldError[]).forEach((e) => {
        const params = Object.assign(
          {},
          e.params.map((p) => (isBundleKey(p) ? unWrap(p) : p))
        );
        setError(fieldName(e.field) as keyof UserRepresentation, {
          message: t(e.errorMessage, { ...params, defaultValue: e.field }),
          type: "server",
        });
      });
    }
  };

  return (
    <Page title={t("personalInfo")} description={t("personalInfoDescription")}>
      <Form isHorizontal onSubmit={handleSubmit(onSubmit)}>
        <FormProvider {...form}>
          {(userProfileMetadata?.attributes || []).map((attribute) => (
            <FormField key={attribute.name} attribute={attribute} />
          ))}
        </FormProvider>
        <ActionGroup>
          <Button type="submit" id="save-btn" variant="primary">
            {t("doSave")}
          </Button>
          <Button id="cancel-btn" variant="link" onClick={() => reset()}>
            {t("doCancel")}
          </Button>
        </ActionGroup>
      </Form>
    </Page>
  );
};

export default PersonalInfo;
