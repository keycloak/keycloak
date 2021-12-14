import React from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
  Switch,
} from "@patternfly/react-core";

import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { FormPanel } from "../components/scroll-form/FormPanel";
import { TimeSelector } from "../components/time-selector/TimeSelector";

import "./RealmSettingsSection.css";

type RealmSettingsSessionsTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export const RealmSettingsSessionsTab = ({
  realm,
  save,
}: RealmSettingsSessionsTabProps) => {
  const { t } = useTranslation("realm-settings");

  const {
    control,
    handleSubmit,
    reset: resetForm,
    formState,
  } = useFormContext<RealmRepresentation>();

  const offlineSessionMaxEnabled = useWatch({
    control,
    name: "offlineSessionMaxLifespanEnabled",
  });

  const reset = () => {
    if (realm) {
      resetForm(realm);
    }
  };

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
                helpText="realm-settings-help:ssoSessionIdle"
                fieldLabelId="realm-settings:SSOSessionIdle"
              />
            }
          >
            <Controller
              name="ssoSessionIdleTimeout"
              defaultValue={realm.ssoSessionIdleTimeout}
              control={control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-sso-session-idle"
                  data-testid="sso-session-idle-input"
                  aria-label="sso-session-idle-input"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
                />
              )}
            />
          </FormGroup>

          <FormGroup
            label={t("SSOSessionMax")}
            fieldId="SSOSessionMax"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:ssoSessionMax"
                fieldLabelId="realm-settings:SSOSessionMax"
              />
            }
          >
            <Controller
              name="ssoSessionMaxLifespan"
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-sso-session-max"
                  data-testid="sso-session-max-input"
                  aria-label="sso-session-max-input"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
                />
              )}
            />
          </FormGroup>

          <FormGroup
            label={t("SSOSessionIdleRememberMe")}
            fieldId="SSOSessionIdleRememberMe"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:ssoSessionIdleRememberMe"
                fieldLabelId="realm-settings:SSOSessionIdleRememberMe"
              />
            }
          >
            <Controller
              name="ssoSessionIdleTimeoutRememberMe"
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-sso-session-idle-remember-me"
                  data-testid="sso-session-idle-remember-me-input"
                  aria-label="sso-session-idle-remember-me-input"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
                />
              )}
            />
          </FormGroup>

          <FormGroup
            label={t("SSOSessionMaxRememberMe")}
            fieldId="SSOSessionMaxRememberMe"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:ssoSessionMaxRememberMe"
                fieldLabelId="realm-settings:SSOSessionMaxRememberMe"
              />
            }
          >
            <Controller
              name="ssoSessionMaxLifespanRememberMe"
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-sso-session-max-remember-me"
                  aria-label="sso-session-max-remember-me-input"
                  data-testid="sso-session-max-remember-me-input"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
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
          className="pf-u-mt-lg"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("clientSessionIdle")}
            fieldId="clientSessionIdle"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:clientSessionIdle"
                fieldLabelId="realm-settings:clientSessionIdle"
              />
            }
          >
            <Controller
              name="clientSessionIdleTimeout"
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-client-session-idle"
                  data-testid="client-session-idle-input"
                  aria-label="client-session-idle-input"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
                />
              )}
            />
          </FormGroup>

          <FormGroup
            label={t("clientSessionMax")}
            fieldId="clientSessionMax"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:clientSessionMax"
                fieldLabelId="realm-settings:clientSessionMax"
              />
            }
          >
            <Controller
              name="clientSessionMaxLifespan"
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-client-session-max"
                  data-testid="client-session-max-input"
                  aria-label="client-session-max-input"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
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
          className="pf-u-mt-lg"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("offlineSessionIdle")}
            fieldId="offlineSessionIdle"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:offlineSessionIdle"
                fieldLabelId="realm-settings:offlineSessionIdle"
              />
            }
          >
            <Controller
              name="offlineSessionIdleTimeout"
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-offline-session-idle"
                  data-testid="offline-session-idle-input"
                  aria-label="offline-session-idle-input"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
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
                helpText="realm-settings-help:offlineSessionMaxLimited"
                fieldLabelId="realm-settings:offlineSessionMaxLimited"
              />
            }
          >
            <Controller
              name="offlineSessionMaxLifespanEnabled"
              control={control}
              defaultValue={false}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-offline-session-max"
                  data-testid="offline-session-max-switch"
                  aria-label="offline-session-max-switch"
                  label={t("common:enabled")}
                  labelOff={t("common:disabled")}
                  isChecked={value}
                  onChange={(value) => onChange(value.toString())}
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
                  helpText="realm-settings-help:offlineSessionMax"
                  fieldLabelId="realm-settings:offlineSessionMax"
                />
              }
            >
              <Controller
                name="offlineSessionMaxLifespan"
                control={control}
                render={({ onChange, value }) => (
                  <TimeSelector
                    className="kc-offline-session-max"
                    data-testid="offline-session-max-input"
                    aria-label="offline-session-max-input"
                    value={value}
                    onChange={onChange}
                    units={["minutes", "hours", "days"]}
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
          className="pf-u-mt-lg"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("loginTimeout")}
            id="kc-login-timeout-label"
            fieldId="offlineSessionIdle"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:loginTimeout"
                fieldLabelId="realm-settings:loginTimeout"
              />
            }
          >
            <Controller
              name="accessCodeLifespanLogin"
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-login-timeout"
                  data-testid="login-timeout-input"
                  aria-label="login-timeout-input"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
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
                helpText="realm-settings-help:loginActionTimeout"
                fieldLabelId="realm-settings:loginActionTimeout"
              />
            }
          >
            <Controller
              name="accessCodeLifespanUserAction"
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-login-action-timeout"
                  data-testid="login-action-timeout-input"
                  aria-label="login-action-timeout-input"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
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
              {t("common:save")}
            </Button>
            <Button variant="link" onClick={reset}>
              {t("common:revert")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </FormPanel>
    </PageSection>
  );
};
