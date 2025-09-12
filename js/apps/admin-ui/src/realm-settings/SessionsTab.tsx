import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
  Switch,
} from "@patternfly/react-core";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormPanel, HelpItem } from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../components/form/FormAccess";
import { TimeSelector } from "../components/time-selector/TimeSelector";

import "./realm-settings-section.css";

type RealmSettingsSessionsTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export const RealmSettingsSessionsTab = ({
  realm,
  save,
}: RealmSettingsSessionsTabProps) => {
  const { t } = useTranslation();

  const { control, handleSubmit, formState, reset } =
    useFormContext<RealmRepresentation>();

  const offlineSessionMaxEnabled = useWatch({
    control,
    name: "offlineSessionMaxLifespanEnabled",
  });

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
              name="ssoSessionIdleTimeout"
              defaultValue={realm.ssoSessionIdleTimeout}
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-sso-session-idle"
                  data-testid="sso-session-idle-input"
                  value={field.value!}
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
              name="ssoSessionMaxLifespan"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-sso-session-max"
                  data-testid="sso-session-max-input"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>

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
              name="ssoSessionIdleTimeoutRememberMe"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-sso-session-idle-remember-me"
                  data-testid="sso-session-idle-remember-me-input"
                  value={field.value!}
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
              name="ssoSessionMaxLifespanRememberMe"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-sso-session-max-remember-me"
                  data-testid="sso-session-max-remember-me-input"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
        </FormAccess>
      </FormPanel>
      <FormPanel
        title={t("clientSessionSettings")}
        className="kc-client-session-template"
      >
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-v5-u-mt-lg"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("clientSessionIdle")}
            fieldId="clientSessionIdle"
            labelIcon={
              <HelpItem
                helpText={t("clientSessionIdleHelp")}
                fieldLabelId="clientSessionIdle"
              />
            }
          >
            <Controller
              name="clientSessionIdleTimeout"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-client-session-idle"
                  data-testid="client-session-idle-input"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>

          <FormGroup
            label={t("clientSessionMax")}
            fieldId="clientSessionMax"
            labelIcon={
              <HelpItem
                helpText={t("clientSessionMaxHelp")}
                fieldLabelId="clientSessionMax"
              />
            }
          >
            <Controller
              name="clientSessionMaxLifespan"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-client-session-max"
                  data-testid="client-session-max-input"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
        </FormAccess>
      </FormPanel>
      <FormPanel
        title={t("offlineSessionSettings")}
        className="kc-offline-session-template"
      >
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-v5-u-mt-lg"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("offlineSessionIdle")}
            fieldId="offlineSessionIdle"
            labelIcon={
              <HelpItem
                helpText={t("offlineSessionIdleHelp")}
                fieldLabelId="offlineSessionIdle"
              />
            }
          >
            <Controller
              name="offlineSessionIdleTimeout"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-offline-session-idle"
                  data-testid="offline-session-idle-input"
                  aria-label="offline-session-idle-input"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>

          <FormGroup
            label={t("clientOfflineSessionIdle")}
            fieldId="clientOfflineSessionIdle"
            labelIcon={
              <HelpItem
                helpText={t("clientOfflineSessionIdleHelp")}
                fieldLabelId="clientOfflineSessionIdle"
              />
            }
          >
            <Controller
              name="clientOfflineSessionIdleTimeout"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-client-offline-session-idle"
                  data-testid="client-offline-session-idle-input"
                  aria-label="client-offline-session-idle-input"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>

          <FormGroup
            hasNoPaddingTop
            label={t("offlineSessionMaxLimited")}
            fieldId="kc-offlineSessionMaxLimited"
            labelIcon={
              <HelpItem
                helpText={t("offlineSessionMaxLimitedHelp")}
                fieldLabelId="offlineSessionMaxLimited"
              />
            }
          >
            <Controller
              name="offlineSessionMaxLifespanEnabled"
              control={control}
              defaultValue={false}
              render={({ field }) => (
                <Switch
                  id="kc-offline-session-max"
                  data-testid="offline-session-max-switch"
                  aria-label={t("offlineSessionMaxLimited")}
                  label={t("enabled")}
                  labelOff={t("disabled")}
                  isChecked={field.value}
                  onChange={field.onChange}
                />
              )}
            />
          </FormGroup>
          {offlineSessionMaxEnabled && (
            <FormGroup
              label={t("offlineSessionMax")}
              fieldId="offlineSessionMax"
              id="offline-session-max-label"
              labelIcon={
                <HelpItem
                  helpText={t("offlineSessionMaxHelp")}
                  fieldLabelId="offlineSessionMax"
                />
              }
            >
              <Controller
                name="offlineSessionMaxLifespan"
                control={control}
                render={({ field }) => (
                  <TimeSelector
                    className="kc-offline-session-max"
                    data-testid="offline-session-max-input"
                    value={field.value!}
                    onChange={field.onChange}
                    units={["minute", "hour", "day"]}
                  />
                )}
              />
            </FormGroup>
          )}
          {offlineSessionMaxEnabled && (
            <FormGroup
              label={t("clientOfflineSessionMax")}
              fieldId="clientOfflineSessionMax"
              id="client-offline-session-max-label"
              labelIcon={
                <HelpItem
                  helpText={t("clientOfflineSessionMaxHelp")}
                  fieldLabelId="clientOfflineSessionMax"
                />
              }
            >
              <Controller
                name="clientOfflineSessionMaxLifespan"
                control={control}
                render={({ field }) => (
                  <TimeSelector
                    className="kc-client-offline-session-max"
                    data-testid="client-offline-session-max-input"
                    value={field.value!}
                    onChange={field.onChange}
                    units={["minute", "hour", "day"]}
                  />
                )}
              />
            </FormGroup>
          )}
        </FormAccess>
      </FormPanel>
      <FormPanel
        className="kc-login-settings-template"
        title={t("loginSettings")}
      >
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-v5-u-mt-lg"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("loginTimeout")}
            id="kc-login-timeout-label"
            fieldId="offlineSessionIdle"
            labelIcon={
              <HelpItem
                helpText={t("loginTimeoutHelp")}
                fieldLabelId="loginTimeout"
              />
            }
          >
            <Controller
              name="accessCodeLifespanLogin"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-login-timeout"
                  data-testid="login-timeout-input"
                  aria-label="login-timeout-input"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("loginActionTimeout")}
            fieldId="loginActionTimeout"
            id="login-action-timeout-label"
            labelIcon={
              <HelpItem
                helpText={t("loginActionTimeoutHelp")}
                fieldLabelId="loginActionTimeout"
              />
            }
          >
            <Controller
              name="accessCodeLifespanUserAction"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-login-action-timeout"
                  data-testid="login-action-timeout-input"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
          <ActionGroup>
            <Button
              variant="primary"
              type="submit"
              data-testid="sessions-tab-save"
              isDisabled={!formState.isDirty}
            >
              {t("save")}
            </Button>
            <Button variant="link" onClick={() => reset(realm)}>
              {t("revert")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </FormPanel>
    </PageSection>
  );
};
