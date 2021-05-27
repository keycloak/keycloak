import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Checkbox,
  FormGroup,
  PageSection,
  Switch,
  TextInput,
} from "@patternfly/react-core";

import type RealmRepresentation from "keycloak-admin/lib/defs/realmRepresentation";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { FormPanel } from "../components/scroll-form/FormPanel";
import { emailRegexPattern } from "../util";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { useRealm } from "../context/realm-context/RealmContext";

import "./RealmSettingsSection.css";

type RealmSettingsEmailTabProps = {
  realm: RealmRepresentation;
};

export const RealmSettingsEmailTab = ({
  realm: initialRealm,
}: RealmSettingsEmailTabProps) => {
  const { t } = useTranslation("realm-settings");
  const adminClient = useAdminClient();
  const { realm: realmName } = useRealm();
  const { addAlert } = useAlerts();

  const [isAuthenticationEnabled, setAuthenticationEnabled] = useState("true");
  const [realm, setRealm] = useState(initialRealm);
  const {
    register,
    control,
    handleSubmit,
    errors,
    setValue,
    reset: resetForm,
  } = useForm<RealmRepresentation>();

  useEffect(() => {
    reset();
  }, [realm]);

  const save = async (form: RealmRepresentation) => {
    try {
      const savedRealm = { ...realm, ...form };
      await adminClient.realms.update({ realm: realmName }, savedRealm);
      setRealm(savedRealm);
      addAlert(t("saveSuccess"), AlertVariant.success);
    } catch (error) {
      addAlert(
        t("saveError", { error: error.response?.data?.errorMessage || error }),
        AlertVariant.danger
      );
    }
  };

  const reset = () => {
    if (realm) {
      resetForm(realm);
      Object.entries(realm).map((entry) => setValue(entry[0], entry[1]));
    }
  };

  return (
    <>
      <PageSection variant="light">
        <FormPanel title={t("template")} className="kc-email-template">
          <FormAccess
            isHorizontal
            role="manage-realm"
            className="pf-u-mt-lg"
            onSubmit={handleSubmit(save)}
          >
            <FormGroup
              label={t("from")}
              fieldId="kc-display-name"
              isRequired
              validated={errors.smtpServer?.from ? "error" : "default"}
              helperTextInvalid={t("users:emailInvalid")}
            >
              <TextInput
                type="email"
                id="kc-sender-email-address"
                data-testid="sender-email-address"
                name="smtpServer.from"
                ref={register({
                  pattern: emailRegexPattern,
                  required: true,
                })}
                placeholder="Sender email address"
                validated={errors.smtpServer?.from ? "error" : "default"}
              />
            </FormGroup>
            <FormGroup
              label={t("fromDisplayName")}
              fieldId="kc-from-display-name"
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:fromDisplayName"
                  forLabel={t("authentication")}
                  forID="kc-user-manged-access"
                />
              }
            >
              <TextInput
                type="text"
                id="kc-from-display-name"
                data-testid="from-display-name"
                name="smtpServer.fromDisplayName"
                ref={register}
                placeholder="Display name for Sender email address"
              />
            </FormGroup>
            <FormGroup
              label={t("replyTo")}
              fieldId="kc-reply-to"
              validated={errors.smtpServer?.replyTo ? "error" : "default"}
              helperTextInvalid={t("users:emailInvalid")}
            >
              <TextInput
                type="email"
                id="kc-reply-to"
                name="smtpServer.replyTo"
                ref={register({
                  pattern: emailRegexPattern,
                })}
                placeholder="Reply to email address"
                validated={errors.smtpServer?.replyTo ? "error" : "default"}
              />
            </FormGroup>
            <FormGroup
              label={t("replyToDisplayName")}
              fieldId="kc-reply-to-display-name"
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:replyToDisplayName"
                  forLabel={t("replyToDisplayName")}
                  forID="kc-user-manged-access"
                />
              }
            >
              <TextInput
                type="text"
                id="kc-reply-to-display-name"
                name="smtpServer.replyToDisplayName"
                ref={register}
                placeholder='Display name for "reply to" email address'
              />
            </FormGroup>
            <FormGroup
              label={t("envelopeFrom")}
              fieldId="kc-envelope-from"
              labelIcon={
                <HelpItem
                  helpText="realm-settings-help:envelopeFrom"
                  forLabel={t("envelopeFrom")}
                  forID="kc-envelope-from"
                />
              }
            >
              <TextInput
                type="text"
                id="kc-envelope-from"
                name="smtpServer.envelopeFrom"
                ref={register}
                placeholder="Sender envelope email address"
              />
            </FormGroup>
          </FormAccess>
        </FormPanel>
        <FormPanel
          className="kc-email-connection"
          title={t("connectionAndAuthentication")}
        >
          <FormAccess
            isHorizontal
            role="manage-realm"
            className="pf-u-mt-lg"
            onSubmit={handleSubmit(save)}
          >
            <FormGroup
              label={t("host")}
              fieldId="kc-host"
              isRequired
              validated={errors.smtpServer?.host ? "error" : "default"}
              helperTextInvalid={t("common:required")}
            >
              <TextInput
                type="text"
                id="kc-host"
                name="smtpServer.host"
                ref={register({ required: true })}
                placeholder="SMTP host"
                validated={errors.smtpServer?.host ? "error" : "default"}
              />
            </FormGroup>
            <FormGroup label={t("port")} fieldId="kc-port">
              <TextInput
                type="text"
                id="kc-port"
                name="smtpServer.port"
                ref={register}
                placeholder="SMTP port (defaults to 25)"
              />
            </FormGroup>
            <FormGroup label={t("encryption")} fieldId="kc-html-display-name">
              <Controller
                name="smtpServer.ssl"
                control={control}
                defaultValue="false"
                render={({ onChange, value }) => (
                  <Checkbox
                    id="kc-enable-ssl"
                    data-testid="enable-ssl"
                    label={t("enableSSL")}
                    ref={register}
                    isChecked={value === "true"}
                    onChange={(value) => onChange("" + value)}
                  />
                )}
              />
              <Controller
                name="smtpServer.starttls"
                control={control}
                defaultValue="false"
                render={({ onChange, value }) => (
                  <Checkbox
                    id="kc-enable-start-tls"
                    data-testid="enable-start-tls"
                    label={t("enableStartTLS")}
                    ref={register}
                    isChecked={value === "true"}
                    onChange={(value) => onChange("" + value)}
                  />
                )}
              />
            </FormGroup>
            <FormGroup
              hasNoPaddingTop
              label={t("authentication")}
              fieldId="kc-authentication"
            >
              <Controller
                name="smtpServer.authentication"
                control={control}
                defaultValue="true"
                render={({ onChange, value }) => (
                  <Switch
                    id="kc-authentication"
                    data-testid="email-authentication-switch"
                    label={t("common:enabled")}
                    labelOff={t("common:disabled")}
                    isChecked={value === "true"}
                    onChange={(value) => {
                      onChange("" + value);
                      setAuthenticationEnabled(String(value));
                    }}
                  />
                )}
              />
            </FormGroup>
            {isAuthenticationEnabled === "true" && (
              <>
                <FormGroup
                  label={t("username")}
                  fieldId="kc-username"
                  isRequired
                  validated={errors.smtpServer?.user ? "error" : "default"}
                  helperTextInvalid={t("common:required")}
                >
                  <TextInput
                    type="text"
                    id="kc-username"
                    data-testid="username-input"
                    name="smtpServer.user"
                    ref={register({ required: true })}
                    placeholder="Login username"
                    validated={errors.smtpServer?.user ? "error" : "default"}
                  />
                </FormGroup>
                <FormGroup
                  label={t("password")}
                  fieldId="kc-username"
                  isRequired
                  validated={errors.smtpServer?.password ? "error" : "default"}
                  helperTextInvalid={t("common:required")}
                  labelIcon={
                    <HelpItem
                      helpText="realm-settings-help:password"
                      forLabel={t("password")}
                      forID="kc-password"
                    />
                  }
                >
                  <TextInput
                    type="password"
                    id="kc-password"
                    data-testid="password-input"
                    name="smtpServer.password"
                    ref={register({ required: true })}
                    placeholder="Login password"
                    validated={
                      errors.smtpServer?.password ? "error" : "default"
                    }
                  />
                </FormGroup>
              </>
            )}

            <ActionGroup>
              <Button
                variant="primary"
                type="submit"
                data-testid="email-tab-save"
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
    </>
  );
};
