import React from "react";
import { useTranslation } from "react-i18next";
import { FormGroup, PageSection, Switch } from "@patternfly/react-core";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { FormPanel } from "../components/scroll-form/FormPanel";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";

type RealmSettingsLoginTabProps = {
  save: (realm: RealmRepresentation) => void;
  realm: RealmRepresentation;
};

export const RealmSettingsLoginTab = ({
  save,
  realm,
}: RealmSettingsLoginTabProps) => {
  const { t } = useTranslation("realm-settings");

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
                forLabel={t("userRegistration")}
                forID={t(`common:helpLabel`, {
                  label: t("userRegistration"),
                })}
              />
            }
            hasNoPaddingTop
          >
            <Switch
              id="kc-user-reg-switch"
              data-testid="user-reg-switch"
              name="registrationAllowed"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={realm?.registrationAllowed}
              onChange={(value) => {
                save({ ...realm, registrationAllowed: value });
              }}
            />
          </FormGroup>
          <FormGroup
            label={t("forgotPassword")}
            fieldId="kc-forgot-pw"
            labelIcon={
              <HelpItem
                helpText={t("forgotPasswordHelpText")}
                forLabel={t("forgotPassword")}
                forID={t(`common:helpLabel`, { label: t("forgotPassword") })}
              />
            }
            hasNoPaddingTop
          >
            <Switch
              id="kc-forgot-pw-switch"
              data-testid="forgot-pw-switch"
              name="resetPasswordAllowed"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={realm?.resetPasswordAllowed}
              onChange={(value) => {
                save({ ...realm, resetPasswordAllowed: value });
              }}
            />
          </FormGroup>
          <FormGroup
            label={t("rememberMe")}
            fieldId="kc-remember-me"
            labelIcon={
              <HelpItem
                helpText={t("rememberMeHelpText")}
                forLabel={t("rememberMe")}
                forID={t(`common:helpLabel`, { label: t("rememberMe") })}
              />
            }
            hasNoPaddingTop
          >
            <Switch
              id="kc-remember-me-switch"
              data-testid="remember-me-switch"
              name="rememberMe"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={realm?.rememberMe}
              onChange={(value) => {
                save({ ...realm, rememberMe: value });
              }}
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
                helpText={t("emailAsUsernameHelpText")}
                forLabel={t("emailAsUsername")}
                forID={t(`common:helpLabel`, { label: t("emailAsUsername") })}
              />
            }
            hasNoPaddingTop
          >
            <Switch
              id="kc-email-as-username-switch"
              data-testid="email-as-username-switch"
              name="registrationEmailAsUsername"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={realm?.registrationEmailAsUsername}
              onChange={(value) => {
                save({ ...realm, registrationEmailAsUsername: value });
              }}
            />
          </FormGroup>
          <FormGroup
            label={t("loginWithEmail")}
            fieldId="kc-login-with-email"
            labelIcon={
              <HelpItem
                helpText={t("loginWithEmailHelpText")}
                forLabel={t("loginWithEmail")}
                forID={t(`common:helpLabel`, { label: t("loginWithEmail") })}
              />
            }
            hasNoPaddingTop
          >
            <Switch
              id="kc-login-with-email-switch"
              data-testid="login-with-email-switch"
              name="loginWithEmailAllowed"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={realm?.loginWithEmailAllowed}
              onChange={(value) => {
                save({ ...realm, loginWithEmailAllowed: value });
              }}
            />
          </FormGroup>
          <FormGroup
            label={t("duplicateEmails")}
            fieldId="kc-duplicate-emails"
            labelIcon={
              <HelpItem
                helpText={t("duplicateEmailsHelpText")}
                forLabel={t("duplicateEmails")}
                forID={t(`common:helpLabel`, { label: t("duplicateEmails") })}
              />
            }
            hasNoPaddingTop
          >
            <Switch
              id="kc-duplicate-emails-switch"
              data-testid="duplicate-emails-switch"
              label={t("common:on")}
              labelOff={t("common:off")}
              name="duplicateEmailsAllowed"
              isChecked={
                realm?.duplicateEmailsAllowed &&
                !realm?.loginWithEmailAllowed &&
                !realm?.registrationEmailAsUsername
              }
              onChange={(value) => {
                save({ ...realm, duplicateEmailsAllowed: value });
              }}
              isDisabled={
                realm?.loginWithEmailAllowed ||
                realm?.registrationEmailAsUsername
              }
            />
          </FormGroup>
          <FormGroup
            label={t("verifyEmail")}
            fieldId="kc-verify-email"
            labelIcon={
              <HelpItem
                helpText={t("verifyEmailHelpText")}
                forLabel={t("verifyEmail")}
                forID={t(`common:helpLabel`, { label: t("verifyEmail") })}
              />
            }
            hasNoPaddingTop
          >
            <Switch
              id="kc-verify-email-switch"
              data-testid="verify-email-switch"
              name="verifyEmail"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={realm?.verifyEmail}
              onChange={(value) => {
                save({ ...realm, verifyEmail: value });
              }}
            />
          </FormGroup>
        </FormAccess>
      </FormPanel>
    </PageSection>
  );
};
