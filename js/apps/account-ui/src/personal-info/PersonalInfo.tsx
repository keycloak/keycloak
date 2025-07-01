import {
  UserProfileFields,
  beerify,
  debeerify,
  setUserProfileServerError,
  useEnvironment,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Alert,
  AlertVariant,
  Button,
  ExpandableSection,
  Form,
  Spinner,
} from "@patternfly/react-core";
import { ExternalLinkSquareAltIcon } from "@patternfly/react-icons";
import { TFunction } from "i18next";
import { useState } from "react";
import { ErrorOption, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import {
  getPersonalInfo,
  getSupportedLocales,
  savePersonalInfo,
} from "../api/methods";
import {
  UserProfileMetadata,
  UserRepresentation,
} from "../api/representations";
import { Page } from "../components/page/Page";
import type { Environment } from "../environment";
import { TFuncKey, i18n } from "../i18n";
import { useAccountAlerts } from "../utils/useAccountAlerts";
import { usePromise } from "../utils/usePromise";

export const PersonalInfo = () => {
  const { t } = useTranslation();
  const context = useEnvironment<Environment>();
  const [userProfileMetadata, setUserProfileMetadata] =
    useState<UserProfileMetadata>();
  const [supportedLocales, setSupportedLocales] = useState<string[]>([]);
  const form = useForm<UserRepresentation>({ mode: "onChange" });
  const { handleSubmit, reset, setValue, setError } = form;
  const { addAlert } = useAccountAlerts();

  usePromise(
    (signal) =>
      Promise.all([
        getPersonalInfo({ signal, context }),
        getSupportedLocales({ signal, context }),
      ]),
    ([personalInfo, supportedLocales]) => {
      setUserProfileMetadata(personalInfo.userProfileMetadata);
      setSupportedLocales(supportedLocales);
      reset(personalInfo);
      Object.entries(personalInfo.attributes || {}).forEach(([k, v]) =>
        setValue(`attributes[${beerify(k)}]`, v),
      );
    },
  );

  const onSubmit = async (user: UserRepresentation) => {
    try {
      const attributes = Object.fromEntries(
        Object.entries(user.attributes || {}).map(([k, v]) => [
          debeerify(k),
          v,
        ]),
      );
      await savePersonalInfo(context, { ...user, attributes });
      const locale = attributes["locale"]?.toString();
      if (locale) {
        await i18n.changeLanguage(locale, (error) => {
          if (error) {
            console.warn("Error(s) loading locale", locale, error);
          }
        });
      }
      await context.keycloak.updateToken();
      addAlert(t("accountUpdatedMessage"));
    } catch (error) {
      addAlert(t("accountUpdatedError"), AlertVariant.danger);

      setUserProfileServerError(
        { responseData: { errors: error as any } },
        (name: string | number, error: unknown) =>
          setError(name as string, error as ErrorOption),
        ((key: TFuncKey, param?: object) => t(key, param as any)) as TFunction,
      );
    }
  };

  if (!userProfileMetadata) {
    return <Spinner />;
  }

  const allFieldsReadOnly = () =>
    userProfileMetadata?.attributes
      ?.map((a) => a.readOnly)
      .reduce((p, c) => p && c, true);

  const {
    updateEmailFeatureEnabled,
    updateEmailActionEnabled,
    isRegistrationEmailAsUsername,
    isEditUserNameAllowed,
  } = context.environment.features;
  return (
    <Page title={t("personalInfo")} description={t("personalInfoDescription")}>
      <Form isHorizontal onSubmit={handleSubmit(onSubmit)}>
        <UserProfileFields
          form={form}
          userProfileMetadata={userProfileMetadata}
          supportedLocales={supportedLocales}
          currentLocale={context.environment.locale}
          t={
            ((key: unknown, params) =>
              t(key as TFuncKey, params as any)) as TFunction
          }
          renderer={(attribute) => {
            const annotations = attribute.annotations
              ? attribute.annotations
              : {};
            return attribute.name === "email" &&
              updateEmailFeatureEnabled &&
              updateEmailActionEnabled &&
              annotations["kc.required.action.supported"] &&
              (!isRegistrationEmailAsUsername || isEditUserNameAllowed) ? (
              <Button
                id="update-email-btn"
                variant="link"
                onClick={() =>
                  context.keycloak.login({ action: "UPDATE_EMAIL" })
                }
                icon={<ExternalLinkSquareAltIcon />}
                iconPosition="right"
              >
                {t("updateEmail")}
              </Button>
            ) : undefined;
          }}
        />
        {!allFieldsReadOnly() && (
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
        )}
        {context.environment.features.deleteAccountAllowed && (
          <ExpandableSection
            data-testid="delete-account"
            toggleText={t("deleteAccount")}
          >
            <Alert
              isInline
              title={t("deleteAccount")}
              variant="danger"
              actionLinks={
                <Button
                  id="delete-account-btn"
                  variant="danger"
                  onClick={() =>
                    context.keycloak.login({
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
