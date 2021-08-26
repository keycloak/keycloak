import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useForm, useWatch } from "react-hook-form";
import {
  ActionGroup,
  AlertVariant,
  Button,
  FormGroup,
  PageSection,
  Switch,
} from "@patternfly/react-core";

import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { FormPanel } from "../components/scroll-form/FormPanel";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { useRealm } from "../context/realm-context/RealmContext";

import "./RealmSettingsSection.css";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { TimeSelector } from "../components/time-selector/TimeSelector";

type RealmSettingsSessionsTabProps = {
  realm?: RealmRepresentation;
  user?: UserRepresentation;
};

export const RealmSettingsSessionsTab = ({
  realm: initialRealm,
}: RealmSettingsSessionsTabProps) => {
  const { t } = useTranslation("realm-settings");
  const adminClient = useAdminClient();
  const { realm: realmName } = useRealm();
  const { addAlert, addError } = useAlerts();

  const [realm, setRealm] = useState(initialRealm);

  const {
    control,
    handleSubmit,
    reset: resetForm,
    formState,
  } = useForm<RealmRepresentation>();

  const offlineSessionMaxEnabled = useWatch({
    control,
    name: "offlineSessionMaxLifespanEnabled",
    defaultValue: realm?.offlineSessionMaxLifespanEnabled,
  });

  useEffect(() => resetForm(realm), [realm]);

  const save = async (form: RealmRepresentation) => {
    try {
      const savedRealm = { ...realm, ...form };
      await adminClient.realms.update({ realm: realmName }, savedRealm);
      setRealm(savedRealm);
      addAlert(t("saveSuccess"), AlertVariant.success);
    } catch (error) {
      addError("realm-settings:saveError", error);
    }
  };

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
                forLabel={t("SSOSessionIdle")}
                forID="SSOSessionIdle"
                id="SSOSessionIdle"
              />
            }
          >
            <Controller
              name="ssoSessionIdleTimeout"
              defaultValue={realm?.ssoSessionIdleTimeout}
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
                forLabel={t("SSOSessionMax")}
                forID="SSOSessionMax"
                id="SSOSessionMax"
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
                forLabel={t("SSOSessionIdleRememberMe")}
                forID="SSOSessionIdleRememberMe"
                id="SSOSessionIdleRememberMe"
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
                forLabel={t("SSOSessionMaxRememberMe")}
                forID="SSOSessionMaxRememberMe"
                id="SSOSessionMaxRememberMe"
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
                forLabel={t("clientSessionIdle")}
                forID="clientSessionIdle"
                id="clientSessionIdle"
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
                forLabel={t("clientSessionMax")}
                forID="clientSessionMax"
                id="clientSessionMax"
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
                forLabel={t("offlineSessionIdle")}
                forID="offlineSessionIdle"
                id="offlineSessionIdle"
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
                forLabel={t("offlineSessionMaxLimited")}
                forID="offlineSessionMaxLimited"
                id="offlineSessionMaxLimited"
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
                  forLabel={t("offlineSessionMax")}
                  forID="offlineSessionMax"
                  id="offlineSessionMax"
                />
              }
            >
              <Controller
                name="offlineSessionMaxLifespan"
                defaultValue=""
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
                forLabel={t("loginTimeout")}
                forID="loginTimeout"
                id="loginTimeout"
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
                forLabel={t("loginActionTimeout")}
                forID="loginActionTimeout"
                id="loginActionTimeout"
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
