import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext, UseFormMethods } from "react-hook-form";
import {
  ActionGroup,
  Button,
  Checkbox,
  FormGroup,
  PageSection,
  Switch,
  TextInput,
} from "@patternfly/react-core";

import RealmRepresentation from "keycloak-admin/lib/defs/realmRepresentation";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { FormPanel } from "../components/scroll-form/FormPanel";

import "./RealmSettingsSection.css";
import { emailRegexPattern } from "../util";

export type UserFormProps = {
  form: UseFormMethods<RealmRepresentation>;
};

type RealmSettingsEmailTabProps = {
  save: (realm: RealmRepresentation) => void;
  reset: () => void;
};

export const RealmSettingsEmailTab = ({
  save,
  reset,
}: RealmSettingsEmailTabProps) => {
  const { t } = useTranslation("realm-settings");
  const [isAuthenticationEnabled, setAuthenticationEnabled] = useState("");
  const { register, control, handleSubmit, errors } = useFormContext();

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
              validated={
                errors.attributes?.from?.type === "pattern"
                  ? "error"
                  : "default"
              }
              helperTextInvalid={t("users:emailInvalid")}
            >
              <TextInput
                type="email"
                id="kc-sender-email-address"
                data-testid="sender-email-address"
                name="attributes.from"
                ref={register({
                  pattern: emailRegexPattern,
                  required: true,
                })}
                placeholder="Sender email address"
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
                name="attributes.fromDisplayName"
                ref={register}
                placeholder="Display name for Sender email address"
              />
            </FormGroup>
            <FormGroup
              label={t("replyTo")}
              fieldId="kc-reply-to"
              validated={
                errors.attributes?.replyTo?.type === "pattern"
                  ? "error"
                  : "default"
              }
              helperTextInvalid={t("users:emailInvalid")}
            >
              <TextInput
                type="email"
                id="kc-reply-to"
                name="attributes.replyTo"
                ref={register({
                  pattern: emailRegexPattern,
                })}
                placeholder="Reply to email address"
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
                name="attributes.replyToDisplayName"
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
                name="attributes.envelopeFrom"
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
            <FormGroup label={t("host")} fieldId="kc-host" isRequired>
              <TextInput
                type="text"
                id="kc-host"
                name="attributes.host"
                ref={register({ required: true })}
                placeholder="SMTP host"
              />
            </FormGroup>
            <FormGroup label={t("port")} fieldId="kc-port">
              <TextInput
                type="text"
                id="kc-port"
                name="attributes.port"
                ref={register}
                placeholder="SMTP port (defaults to 25)"
              />
            </FormGroup>
            <FormGroup label={t("encryption")} fieldId="kc-html-display-name">
              <Controller
                name="attributes.enableSsl"
                control={control}
                defaultValue="false"
                render={({ onChange, value }) => (
                  <Checkbox
                    id="kc-enable-ssl"
                    data-testid="enable-ssl"
                    name="attributes.enableSsl"
                    label={t("enableSSL")}
                    ref={register}
                    isChecked={value === "true"}
                    onChange={(value) => onChange("" + value)}
                  />
                )}
              />
              <Controller
                name="attributes.enableStartTls"
                control={control}
                defaultValue="false"
                render={({ onChange, value }) => (
                  <Checkbox
                    id="kc-enable-start-tls"
                    data-testid="enable-start-tls"
                    name="attributes.startTls"
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
                name="attributes.authentication"
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
                  isRequired={isAuthenticationEnabled === "true"}
                >
                  <TextInput
                    type="text"
                    id="kc-username"
                    data-testid="username-input"
                    name="attributes.loginUsername"
                    ref={register({ required: true })}
                    placeholder="Login username"
                  />
                </FormGroup>
                <FormGroup
                  label={t("password")}
                  fieldId="kc-username"
                  isRequired={isAuthenticationEnabled === "true"}
                  labelIcon={
                    <HelpItem
                      helpText="realm-settings-help:frontendUrl"
                      forLabel={t("password")}
                      forID="kc-password"
                    />
                  }
                >
                  <TextInput
                    type="password"
                    id="kc-password"
                    data-testid="password-input"
                    name="attributes.loginPassword"
                    ref={register}
                    placeholder="Login password"
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
