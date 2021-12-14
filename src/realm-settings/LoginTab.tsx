import React from "react";
import { useTranslation } from "react-i18next";
import { FormGroup, PageSection, Switch } from "@patternfly/react-core";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { FormPanel } from "../components/scroll-form/FormPanel";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { Controller, useForm } from "react-hook-form";

type RealmSettingsLoginTabProps = {
  save: (realm: RealmRepresentation) => void;
  realm: RealmRepresentation;
  refresh: () => void;
};

export const RealmSettingsLoginTab = ({
  save,
  realm,
  refresh,
}: RealmSettingsLoginTabProps) => {
  const { t } = useTranslation("realm-settings");

  const form = useForm<RealmRepresentation>({ mode: "onChange" });

  const updateSwitchValue = (
    onChange: (newValue: boolean) => void,
    value: boolean,
    name: string
  ) => {
    save({ ...realm, [name as keyof typeof realm]: value });
    onChange(value);
    refresh();
  };

  return (
    <PageSection variant="light">
      <FormPanel className="kc-login-screen" title="Login screen customization">
        <FormAccess isHorizontal role="manage-realm">
          <FormGroup
            label={t("userRegistration")}
            fieldId="kc-user-reg"
            labelIcon={
              <HelpItem
                helpText={t("userRegistrationHelpText")}
                fieldLabelId="realm-settings:userRegistration"
              />
            }
            hasNoPaddingTop
          >
            <Controller
              name="registrationAllowed"
              defaultValue={realm.registrationAllowed}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-user-reg-switch"
                  data-testid="user-reg-switch"
                  name="registrationAllowed"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={(value) => {
                    updateSwitchValue(onChange, value, "registrationAllowed");
                  }}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("forgotPassword")}
            fieldId="kc-forgot-pw"
            labelIcon={
              <HelpItem
                helpText="realm-settings:forgotPasswordHelpText"
                fieldLabelId="realm-settings:forgotPassword"
              />
            }
            hasNoPaddingTop
          >
            <Controller
              name="resetPasswordAllowed"
              defaultValue={realm.resetPasswordAllowed}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-forgot-pw-switch"
                  data-testid="forgot-pw-switch"
                  name="resetPasswordAllowed"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={(value) => {
                    updateSwitchValue(onChange, value, "resetPasswordAllowed");
                  }}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("rememberMe")}
            fieldId="kc-remember-me"
            labelIcon={
              <HelpItem
                helpText="realm-settings:rememberMeHelpText"
                fieldLabelId="realm-settings:rememberMe"
              />
            }
            hasNoPaddingTop
          >
            <Controller
              name="rememberMe"
              defaultValue={realm.rememberMe}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-remember-me-switch"
                  data-testid="remember-me-switch"
                  name="rememberMe"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={(value) => {
                    updateSwitchValue(onChange, value, "rememberMe");
                  }}
                />
              )}
            />
          </FormGroup>
        </FormAccess>
      </FormPanel>
      <FormPanel className="kc-email-settings" title="Email settings">
        <FormAccess isHorizontal role="manage-realm">
          <FormGroup
            label={t("emailAsUsername")}
            fieldId="kc-email-as-username"
            labelIcon={
              <HelpItem
                helpText="realm-settings:emailAsUsernameHelpText"
                fieldLabelId="realm-settings:emailAsUsername"
              />
            }
            hasNoPaddingTop
          >
            <Controller
              name="registrationEmailAsUsername"
              defaultValue={realm.registrationEmailAsUsername}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-email-as-username-switch"
                  data-testid="email-as-username-switch"
                  name="registrationEmailAsUsername"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={(value) => {
                    updateSwitchValue(
                      onChange,
                      value,
                      "registrationEmailAsUsername"
                    );
                  }}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("loginWithEmail")}
            fieldId="kc-login-with-email"
            labelIcon={
              <HelpItem
                helpText="realm-settings:loginWithEmailHelpText"
                fieldLabelId="realm-settings:loginWithEmail"
              />
            }
            hasNoPaddingTop
          >
            <Controller
              name="loginWithEmailAllowed"
              defaultValue={realm.loginWithEmailAllowed}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-login-with-email-switch"
                  data-testid="login-with-email-switch"
                  name="loginWithEmailAllowed"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={(value) => {
                    updateSwitchValue(onChange, value, "loginWithEmailAllowed");
                  }}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("duplicateEmails")}
            fieldId="kc-duplicate-emails"
            labelIcon={
              <HelpItem
                helpText="realm-settings:duplicateEmailsHelpText"
                fieldLabelId="realm-settings:duplicateEmails"
              />
            }
            hasNoPaddingTop
          >
            <Controller
              name="duplicateEmailsAllowed"
              defaultValue={realm.duplicateEmailsAllowed}
              control={form.control}
              render={({ onChange }) => (
                <Switch
                  id="kc-duplicate-emails-switch"
                  data-testid="duplicate-emails-switch"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  name="duplicateEmailsAllowed"
                  isChecked={
                    form.getValues().duplicateEmailsAllowed &&
                    !form.getValues().loginWithEmailAllowed &&
                    !form.getValues().registrationEmailAsUsername
                  }
                  onChange={(value) => {
                    updateSwitchValue(
                      onChange,
                      value,
                      "duplicateEmailsAllowed"
                    );
                  }}
                  isDisabled={
                    form.getValues().loginWithEmailAllowed ||
                    form.getValues().registrationEmailAsUsername
                  }
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("verifyEmail")}
            fieldId="kc-verify-email"
            labelIcon={
              <HelpItem
                helpText="realm-settings:verifyEmailHelpText"
                fieldLabelId="realm-settings:verifyEmail"
              />
            }
            hasNoPaddingTop
          >
            <Controller
              name="verifyEmail"
              defaultValue={realm.verifyEmail}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-verify-email-switch"
                  data-testid="verify-email-switch"
                  name="verifyEmail"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={(value) => {
                    updateSwitchValue(onChange, value, "verifyEmail");
                  }}
                />
              )}
            />
          </FormGroup>
        </FormAccess>
      </FormPanel>
    </PageSection>
  );
};
