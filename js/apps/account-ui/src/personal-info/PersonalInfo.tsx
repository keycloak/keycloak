import {
  ActionGroup,
  Alert,
  Button,
  ExpandableSection,
  Form,
  Spinner,
} from "@patternfly/react-core";
import { ExternalLinkSquareAltIcon } from "@patternfly/react-icons";
import { useKeycloak } from "keycloak-masthead";
import { useState } from "react";
import { ErrorOption, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  UserProfileFields,
  setUserProfileServerError,
  useAlerts,
} from "ui-shared";
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
import { environment } from "../environment";
import { TFuncKey } from "../i18n";
import { usePromise } from "../utils/usePromise";

const PersonalInfo = () => {
  const { t } = useTranslation();
  const keycloak = useKeycloak();
  const [userProfileMetadata, setUserProfileMetadata] =
    useState<UserProfileMetadata>();
  const [supportedLocales, setSupportedLocales] = useState<string[]>([]);
  const form = useForm<UserRepresentation>({ mode: "onChange" });
  const { handleSubmit, reset, setError } = form;
  const { addAlert, addError } = useAlerts();

  usePromise(
    (signal) =>
      Promise.all([
        getPersonalInfo({ signal }),
        getSupportedLocales({ signal }),
      ]),
    ([personalInfo, supportedLocales]) => {
      setUserProfileMetadata(personalInfo.userProfileMetadata);
      setSupportedLocales(supportedLocales);
      reset(personalInfo);
    },
  );

  const onSubmit = async (user: UserRepresentation) => {
    try {
      await savePersonalInfo(user);
      keycloak?.updateToken();
      addAlert(t("accountUpdatedMessage"));
    } catch (error) {
      addError(t("accountUpdatedError").toString());

      setUserProfileServerError(
        { responseData: { errors: error as any } },
        (name: string | number, error: unknown) =>
          setError(name as string, error as ErrorOption),
        (key: TFuncKey, param?: object) => t(key, { ...param }),
      );
    }
  };

  if (!userProfileMetadata) {
    return <Spinner />;
  }

  const {
    updateEmailFeatureEnabled,
    updateEmailActionEnabled,
    isRegistrationEmailAsUsername,
    isEditUserNameAllowed,
  } = environment.features;
  return (
    <Page title={t("personalInfo")} description={t("personalInfoDescription")}>
      <Form isHorizontal onSubmit={handleSubmit(onSubmit)}>
        <UserProfileFields
          form={form}
          userProfileMetadata={userProfileMetadata}
          supportedLocales={supportedLocales}
          t={(key: unknown, params) => t(key as TFuncKey, { ...params })}
          renderer={(attribute) =>
            attribute.name === "email" &&
            updateEmailFeatureEnabled &&
            updateEmailActionEnabled &&
            (!isRegistrationEmailAsUsername || isEditUserNameAllowed) ? (
              <Button
                id="update-email-btn"
                variant="link"
                onClick={() =>
                  keycloak?.keycloak.login({ action: "UPDATE_EMAIL" })
                }
                icon={<ExternalLinkSquareAltIcon />}
                iconPosition="right"
              >
                {t("updateEmail")}
              </Button>
            ) : undefined
          }
        />
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
                    keycloak?.keycloak.login({
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
