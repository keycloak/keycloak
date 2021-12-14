import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import {
  ActionGroup,
  Button,
  FormGroup,
  NumberInput,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  Text,
  TextVariants,
} from "@patternfly/react-core";

import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { FormPanel } from "../components/scroll-form/FormPanel";
import { TimeSelector } from "../components/time-selector/TimeSelector";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { forHumans, interpolateTimespan } from "../util";

import "./RealmSettingsSection.css";

type RealmSettingsSessionsTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
  reset?: () => void;
};

export const RealmSettingsTokensTab = ({
  realm,
  reset,
  save,
}: RealmSettingsSessionsTabProps) => {
  const { t } = useTranslation("realm-settings");
  const serverInfo = useServerInfo();

  const [defaultSigAlgDrpdwnIsOpen, setDefaultSigAlgDrpdwnOpen] =
    useState(false);

  const allComponentTypes =
    serverInfo.componentTypes?.["org.keycloak.keys.KeyProvider"] ?? [];

  const esOptions = ["ES256", "ES384", "ES512"];

  const hmacAlgorithmOptions = allComponentTypes[2].properties[4].options;

  const javaKeystoreAlgOptions = allComponentTypes[3].properties[3].options;

  const defaultSigAlgOptions = esOptions.concat(
    hmacAlgorithmOptions!,
    javaKeystoreAlgOptions!
  );

  const form = useFormContext<RealmRepresentation>();
  const { control } = form;

  const offlineSessionMaxEnabled = useWatch({
    control,
    name: "offlineSessionMaxLifespanEnabled",
    defaultValue: realm.offlineSessionMaxLifespanEnabled,
  });

  return (
    <PageSection variant="light">
      <FormPanel
        title={t("realm-settings:general")}
        className="kc-sso-session-template"
      >
        <FormAccess
          isHorizontal
          role="manage-realm"
          onSubmit={form.handleSubmit(save)}
        >
          <FormGroup
            label={t("defaultSigAlg")}
            fieldId="kc-default-signature-algorithm"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:defaultSigAlg"
                fieldLabelId="realm-settings:algorithm"
              />
            }
          >
            <Controller
              name="defaultSignatureAlgorithm"
              defaultValue={"RS256"}
              control={form.control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="kc-default-sig-alg"
                  onToggle={() =>
                    setDefaultSigAlgDrpdwnOpen(!defaultSigAlgDrpdwnIsOpen)
                  }
                  onSelect={(_, value) => {
                    onChange(value.toString());
                    setDefaultSigAlgDrpdwnOpen(false);
                  }}
                  selections={[value.toString()]}
                  variant={SelectVariant.single}
                  aria-label={t("defaultSigAlg")}
                  isOpen={defaultSigAlgDrpdwnIsOpen}
                  data-testid="select-default-sig-alg"
                >
                  {defaultSigAlgOptions!.map((p, idx) => (
                    <SelectOption
                      selected={p === value}
                      key={`default-sig-alg-${idx}`}
                      value={p}
                    ></SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>
        </FormAccess>
      </FormPanel>
      <FormPanel
        title={t("realm-settings:refreshTokens")}
        className="kc-client-session-template"
      >
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg"
          onSubmit={form.handleSubmit(save)}
        >
          <FormGroup
            hasNoPaddingTop
            label={t("revokeRefreshToken")}
            fieldId="kc-revoke-refresh-token"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:revokeRefreshToken"
                fieldLabelId="realm-settings:revokeRefreshToken"
              />
            }
          >
            <Controller
              name="revokeRefreshToken"
              control={form.control}
              defaultValue={false}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-revoke-refresh-token"
                  data-testid="revoke-refresh-token-switch"
                  aria-label="revoke-refresh-token-switch"
                  label={t("common:enabled")}
                  labelOff={t("common:disabled")}
                  isChecked={value}
                  onChange={onChange}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("refreshTokenMaxReuse")}
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:refreshTokenMaxReuse"
                fieldLabelId="realm-settings:refreshTokenMaxReuse"
              />
            }
            fieldId="refreshTokenMaxReuse"
          >
            <Controller
              name="refreshTokenMaxReuse"
              defaultValue={0}
              control={form.control}
              render={({ onChange, value }) => (
                <NumberInput
                  type="text"
                  id="refreshTokenMaxReuseMs"
                  value={value}
                  onPlus={() => onChange(value + 1)}
                  onMinus={() => onChange(value - 1)}
                  onChange={(event) =>
                    onChange(Number((event.target as HTMLInputElement).value))
                  }
                />
              )}
            />
          </FormGroup>
        </FormAccess>
      </FormPanel>
      <FormPanel
        title={t("realm-settings:accessTokens")}
        className="kc-offline-session-template"
      >
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg"
          onSubmit={form.handleSubmit(save)}
        >
          <FormGroup
            label={t("accessTokenLifespan")}
            fieldId="accessTokenLifespan"
            helperText={`It is recommended for this value to be shorter than the SSO session idle timeout: ${interpolateTimespan(
              forHumans(realm.ssoSessionIdleTimeout!)
            )}`}
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:accessTokenLifespan"
                fieldLabelId="realm-settings:accessTokenLifespan"
              />
            }
          >
            <Controller
              name="accessTokenLifespan"
              defaultValue=""
              helperTextInvalid={t("common:required")}
              control={form.control}
              render={({ onChange, value }) => (
                <TimeSelector
                  validated={
                    value > realm.ssoSessionIdleTimeout! ? "warning" : "default"
                  }
                  className="kc-access-token-lifespan"
                  data-testid="access-token-lifespan-input"
                  aria-label="access-token-lifespan"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
                />
              )}
            />
          </FormGroup>

          <FormGroup
            label={t("accessTokenLifespanImplicitFlow")}
            fieldId="accessTokenLifespanImplicitFlow"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:accessTokenLifespanImplicitFlow"
                fieldLabelId="realm-settings:accessTokenLifespanImplicitFlow"
              />
            }
          >
            <Controller
              name="accessTokenLifespanForImplicitFlow"
              defaultValue=""
              control={form.control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-access-token-lifespan-implicit"
                  data-testid="access-token-lifespan-implicit-input"
                  aria-label="access-token-lifespan-implicit"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("clientLoginTimeout")}
            fieldId="clientLoginTimeout"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:clientLoginTimeout"
                fieldLabelId="realm-settings:clientLoginTimeout"
              />
            }
          >
            <Controller
              name="accessCodeLifespan"
              defaultValue=""
              control={form.control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-client-login-timeout"
                  data-testid="client-login-timeout-input"
                  aria-label="client-login-timeout"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
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
                defaultValue=""
                control={form.control}
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
        title={t("actionTokens")}
      >
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg"
          onSubmit={form.handleSubmit(save)}
        >
          <FormGroup
            label={t("userInitiatedActionLifespan")}
            id="kc-user-initiated-action-lifespan"
            fieldId="userInitiatedActionLifespan"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:userInitiatedActionLifespan"
                fieldLabelId="realm-settings:userInitiatedActionLifespan"
              />
            }
          >
            <Controller
              name="actionTokenGeneratedByUserLifespan"
              defaultValue={""}
              control={form.control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-user-initiated-action-lifespan"
                  data-testid="user-initiated-action-lifespan"
                  aria-label="user-initiated-action-lifespan"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("defaultAdminInitiated")}
            fieldId="defaultAdminInitiated"
            id="default-admin-initiated-label"
            labelIcon={
              <HelpItem
                helpText="realm-settings-help:defaultAdminInitiatedActionLifespan"
                fieldLabelId="realm-settings:defaultAdminInitiated"
              />
            }
          >
            <Controller
              name="actionTokenGeneratedByAdminLifespan"
              defaultValue={""}
              control={form.control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-default-admin-initiated"
                  data-testid="default-admin-initated-input"
                  aria-label="default-admin-initated-input"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
                />
              )}
            />
          </FormGroup>
          <Text
            className="kc-override-action-tokens-subtitle"
            component={TextVariants.h1}
          >
            {t("overrideActionTokens")}
          </Text>
          <FormGroup
            label={t("emailVerification")}
            fieldId="emailVerification"
            id="email-verification"
          >
            <Controller
              name="attributes.actionTokenGeneratedByUserLifespan-verify-email"
              defaultValue={""}
              control={form.control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-email-verification"
                  data-testid="email-verification-input"
                  aria-label="email-verification-input"
                  value={value}
                  onChange={(value: any) => onChange(value.toString())}
                  units={["minutes", "hours", "days"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("idpAccountEmailVerification")}
            fieldId="idpAccountEmailVerification"
            id="idp-acct-label"
          >
            <Controller
              name="attributes.actionTokenGeneratedByUserLifespan-idp-verify-account-via-email"
              defaultValue={""}
              control={form.control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-idp-email-verification"
                  data-testid="idp-email-verification-input"
                  aria-label="idp-email-verification"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("forgotPassword")}
            fieldId="forgotPassword"
            id="forgot-password-label"
          >
            <Controller
              name="attributes.actionTokenGeneratedByUserLifespan-reset-credentials"
              defaultValue={""}
              control={form.control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-forgot-pw"
                  data-testid="forgot-pw-input"
                  aria-label="forgot-pw-input"
                  value={value}
                  onChange={onChange}
                  units={["minutes", "hours", "days"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("executeActions")}
            fieldId="executeActions"
            id="execute-actions"
          >
            <Controller
              name="attributes.actionTokenGeneratedByUserLifespan-execute-actions"
              defaultValue={""}
              control={form.control}
              render={({ onChange, value }) => (
                <TimeSelector
                  className="kc-execute-actions"
                  data-testid="execute-actions-input"
                  aria-label="execute-actions-input"
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
              data-testid="tokens-tab-save"
              isDisabled={!form.formState.isDirty}
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
