import {
  ActionGroup,
  Alert,
  Button,
  ExpandableSection,
  Form,
  Spinner,
} from "@patternfly/react-core";
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
import { environment } from "../environment";
import { TFuncKey } from "../i18n";
import { keycloak } from "../keycloak";
import { usePromise } from "../utils/usePromise";
import { UserProfileFields } from "./UserProfileFields";

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
    },
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
          e.params.map((p) => t((isBundleKey(p) ? unWrap(p) : p) as TFuncKey)),
        );
        setError(fieldName(e.field) as keyof UserRepresentation, {
          message: t(e.errorMessage as TFuncKey, {
            ...params,
            defaultValue: e.field,
          }),
          type: "server",
        });
      });
    }
  };

  if (!userProfileMetadata) {
    return <Spinner />;
  }

  return (
    <Page title={t("personalInfo")} description={t("personalInfoDescription")}>
      <Form isHorizontal onSubmit={handleSubmit(onSubmit)}>
        <FormProvider {...form}>
          <UserProfileFields metaData={userProfileMetadata} />
        </FormProvider>
        <ActionGroup>
          <Button
            data-testid="save"
            type="submit"
            id="save-btn"
            variant="primary"
          >
            {t("save")}
          </Button>
          <Button
            data-testid="cancel"
            id="cancel-btn"
            variant="link"
            onClick={() => reset()}
          >
            {t("cancel")}
          </Button>
        </ActionGroup>
        {environment.features.deleteAccountAllowed && (
          <ExpandableSection toggleText={t("deleteAccount")}>
            <Alert
              isInline
              title={t("deleteAccount")}
              variant="danger"
              actionLinks={
                <Button
                  id="delete-account-btn"
                  variant="danger"
                  onClick={() =>
                    keycloak.login({
                      action: "delete_account",
                    })
                  }
                  className="delete-button"
                >
                  {t("delete")}
                </Button>
              }
            >
              {t("deleteAccountWarning")}
            </Alert>
          </ExpandableSection>
        )}
      </Form>
    </Page>
  );
};

export default PersonalInfo;
