import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
} from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  FormPanel,
  HelpItem,
  FormSubmitButton,
} from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../components/form/FormAccess";
import { TimeSelector } from "../components/time-selector/TimeSelector";
import { OrganizationFormType } from "./OrganizationForm";

type OrganizationSessionsTabProps = {
  save: (org: OrganizationFormType) => void;
};

export const OrganizationSessionsTab = ({
  save,
}: OrganizationSessionsTabProps) => {
  const { t } = useTranslation();

  const { control, handleSubmit, watch, formState } =
    useFormContext<OrganizationFormType>();

  const rememberMeEnabled = watch("rememberMe");

  return (
    <PageSection variant="light">
      <FormPanel
        title={t("SSOSessionSettings")}
        className="kc-sso-session-template"
      >
        <FormAccess
          isHorizontal
          role="manage-realm"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("SSOSessionIdle")}
            fieldId="SSOSessionIdle"
            labelIcon={
              <HelpItem
                helpText={t("ssoSessionIdle")}
                fieldLabelId="SSOSessionIdle"
              />
            }
          >
            <Controller
              name="sessionIdleTimeout"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-sso-session-idle"
                  data-testid="organization-sso-session-idle-input"
                  value={field.value || 0}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>

          <FormGroup
            label={t("SSOSessionMax")}
            fieldId="SSOSessionMax"
            labelIcon={
              <HelpItem
                helpText={t("ssoSessionMax")}
                fieldLabelId="SSOSessionMax"
              />
            }
          >
            <Controller
              name="sessionMaxLifespan"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-sso-session-max"
                  data-testid="organization-sso-session-max-input"
                  value={field.value || 0}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>

          {rememberMeEnabled && (
            <>
              <FormGroup
                label={t("SSOSessionIdleRememberMe")}
                fieldId="SSOSessionIdleRememberMe"
                labelIcon={
                  <HelpItem
                    helpText={t("ssoSessionIdleRememberMe")}
                    fieldLabelId="SSOSessionIdleRememberMe"
                  />
                }
              >
                <Controller
                  name="sessionIdleTimeoutRememberMe"
                  control={control}
                  render={({ field }) => (
                    <TimeSelector
                      className="kc-sso-session-idle-remember-me"
                      data-testid="organization-sso-session-idle-remember-me-input"
                      value={field.value || 0}
                      onChange={field.onChange}
                      units={["minute", "hour", "day"]}
                    />
                  )}
                />
              </FormGroup>

              <FormGroup
                label={t("SSOSessionMaxRememberMe")}
                fieldId="SSOSessionMaxRememberMe"
                labelIcon={
                  <HelpItem
                    helpText={t("ssoSessionMaxRememberMe")}
                    fieldLabelId="SSOSessionMaxRememberMe"
                  />
                }
              >
                <Controller
                  name="sessionMaxLifespanRememberMe"
                  control={control}
                  render={({ field }) => (
                    <TimeSelector
                      className="kc-sso-session-max-remember-me"
                      data-testid="organization-sso-session-max-remember-me-input"
                      value={field.value || 0}
                      onChange={field.onChange}
                      units={["minute", "hour", "day"]}
                    />
                  )}
                />
              </FormGroup>
            </>
          )}

          <ActionGroup>
            <FormSubmitButton
              formState={formState}
              data-testid="organization-sessions-save"
            >
              {t("save")}
            </FormSubmitButton>
            <Button variant="link" data-testid="organization-sessions-reset">
              {t("reset")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </FormPanel>
    </PageSection>
  );
};
