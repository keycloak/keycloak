import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  HelpItem,
  KeycloakSelect,
  SelectVariant,
  ScrollForm,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  FormGroup,
  FormHelperText,
  HelperText,
  HelperTextItem,
  NumberInput,
  SelectOption,
  Switch,
  Text,
  TextInput,
  TextVariants,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../components/form/FormAccess";
import { FixedButtonsGroup } from "../components/form/FixedButtonGroup";
import { convertAttributeNameToForm } from "../util";
import {
  TimeSelector,
  toHumanFormat,
} from "../components/time-selector/TimeSelector";
import { TimeSelectorControl } from "../components/time-selector/TimeSelectorControl";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useWhoAmI } from "../context/whoami/WhoAmI";
import { beerify, sortProviders } from "../util";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";

import "./realm-settings-section.css";

type RealmSettingsSessionsTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export const RealmSettingsTokensTab = ({
  realm,
  save,
}: RealmSettingsSessionsTabProps) => {
  const { t } = useTranslation();
  const { addAlert } = useAlerts();
  const serverInfo = useServerInfo();
  const isFeatureEnabled = useIsFeatureEnabled();
  const { whoAmI } = useWhoAmI();

  const [defaultSigAlgDrpdwnIsOpen, setDefaultSigAlgDrpdwnOpen] =
    useState(false);

  const defaultSigAlgOptions = sortProviders(
    serverInfo.providers!["signature"].providers,
  );

  const { control, register, reset, formState, handleSubmit } =
    useFormContext<RealmRepresentation>();

  // Show a global error notification if validation fails
  const onError = () => {
    addAlert(t("oid4vciFormValidationError"), AlertVariant.danger);
  };

  const offlineSessionMaxEnabled = useWatch({
    control,
    name: "offlineSessionMaxLifespanEnabled",
    defaultValue: realm.offlineSessionMaxLifespanEnabled,
  });

  const ssoSessionIdleTimeout = useWatch({
    control,
    name: "ssoSessionIdleTimeout",
    defaultValue: 36000,
  });

  const revokeRefreshToken = useWatch({
    control,
    name: "revokeRefreshToken",
    defaultValue: false,
  });

  const sections = [
    {
      title: t("general"),
      panel: (
        <FormAccess
          isHorizontal
          role="manage-realm"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("defaultSigAlg")}
            fieldId="kc-default-signature-algorithm"
            labelIcon={
              <HelpItem
                helpText={t("defaultSigAlgHelp")}
                fieldLabelId="algorithm"
              />
            }
          >
            <Controller
              name="defaultSignatureAlgorithm"
              defaultValue={"RS256"}
              control={control}
              render={({ field }) => (
                <KeycloakSelect
                  toggleId="kc-default-sig-alg"
                  onToggle={() =>
                    setDefaultSigAlgDrpdwnOpen(!defaultSigAlgDrpdwnIsOpen)
                  }
                  onSelect={(value) => {
                    field.onChange(value.toString());
                    setDefaultSigAlgDrpdwnOpen(false);
                  }}
                  selections={field.value?.toString()}
                  variant={SelectVariant.single}
                  aria-label={t("defaultSigAlg")}
                  isOpen={defaultSigAlgDrpdwnIsOpen}
                  data-testid="select-default-sig-alg"
                >
                  {defaultSigAlgOptions!.map((p, idx) => (
                    <SelectOption
                      selected={p === field.value}
                      key={`default-sig-alg-${idx}`}
                      value={p}
                    >
                      {p}
                    </SelectOption>
                  ))}
                </KeycloakSelect>
              )}
            />
          </FormGroup>

          {isFeatureEnabled(Feature.DeviceFlow) && (
            <>
              <FormGroup
                label={t("oAuthDeviceCodeLifespan")}
                fieldId="oAuthDeviceCodeLifespan"
                labelIcon={
                  <HelpItem
                    helpText={t("oAuthDeviceCodeLifespanHelp")}
                    fieldLabelId="oAuthDeviceCodeLifespan"
                  />
                }
              >
                <Controller
                  name="oauth2DeviceCodeLifespan"
                  defaultValue={0}
                  control={control}
                  render={({ field }) => (
                    <TimeSelector
                      id="oAuthDeviceCodeLifespan"
                      data-testid="oAuthDeviceCodeLifespan"
                      value={field.value || 0}
                      onChange={field.onChange}
                      units={["minute", "hour", "day"]}
                    />
                  )}
                />
              </FormGroup>
              <FormGroup
                label={t("oAuthDevicePollingInterval")}
                fieldId="oAuthDevicePollingInterval"
                labelIcon={
                  <HelpItem
                    helpText={t("oAuthDevicePollingIntervalHelp")}
                    fieldLabelId="oAuthDevicePollingInterval"
                  />
                }
              >
                <Controller
                  name="oauth2DevicePollingInterval"
                  defaultValue={0}
                  control={control}
                  render={({ field }) => (
                    <NumberInput
                      id="oAuthDevicePollingInterval"
                      value={field.value}
                      min={0}
                      onPlus={() => field.onChange(Number(field?.value) + 1)}
                      onMinus={() =>
                        field.onChange(
                          Number(field?.value) > 0
                            ? Number(field?.value) - 1
                            : 0,
                        )
                      }
                      onChange={(event) => {
                        const newValue = Number(event.currentTarget.value);
                        field.onChange(!isNaN(newValue) ? newValue : 0);
                      }}
                      placeholder={t("oAuthDevicePollingInterval")}
                    />
                  )}
                />
              </FormGroup>
              <FormGroup
                label={t("shortVerificationUri")}
                fieldId="shortVerificationUri"
                labelIcon={
                  <HelpItem
                    helpText={t("shortVerificationUriTooltipHelp")}
                    fieldLabelId="shortVerificationUri"
                  />
                }
              >
                <TextInput
                  id="shortVerificationUri"
                  placeholder={t("shortVerificationUri")}
                  {...register("attributes.shortVerificationUri")}
                />
              </FormGroup>
              <FormGroup
                label={t("parRequestUriLifespan")}
                fieldId="parRequestUriLifespan"
                labelIcon={
                  <HelpItem
                    helpText={t("parRequestUriLifespanHelp")}
                    fieldLabelId="parRequestUriLifespan"
                  />
                }
              >
                <Controller
                  name="attributes.parRequestUriLifespan"
                  control={control}
                  render={({ field }) => (
                    <TimeSelector
                      id="parRequestUriLifespan"
                      className="par-request-uri-lifespan"
                      data-testid="par-request-uri-lifespan-input"
                      aria-label="par-request-uri-lifespan"
                      value={field.value}
                      onChange={field.onChange}
                    />
                  )}
                />
              </FormGroup>
            </>
          )}
        </FormAccess>
      ),
    },
    {
      title: t("refreshTokens"),
      panel: (
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-v5-u-mt-lg"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            hasNoPaddingTop
            label={t("revokeRefreshToken")}
            fieldId="kc-revoke-refresh-token"
            labelIcon={
              <HelpItem
                helpText={t("revokeRefreshTokenHelp")}
                fieldLabelId="revokeRefreshToken"
              />
            }
          >
            <Controller
              name="revokeRefreshToken"
              control={control}
              defaultValue={false}
              render={({ field }) => (
                <Switch
                  id="kc-revoke-refresh-token"
                  data-testid="revoke-refresh-token-switch"
                  aria-label={t("revokeRefreshToken")}
                  label={t("enabled")}
                  labelOff={t("disabled")}
                  isChecked={field.value}
                  onChange={field.onChange}
                />
              )}
            />
          </FormGroup>
          {revokeRefreshToken && (
            <FormGroup
              label={t("refreshTokenMaxReuse")}
              labelIcon={
                <HelpItem
                  helpText={t("refreshTokenMaxReuseHelp")}
                  fieldLabelId="refreshTokenMaxReuse"
                />
              }
              fieldId="refreshTokenMaxReuse"
            >
              <Controller
                name="refreshTokenMaxReuse"
                defaultValue={0}
                control={control}
                render={({ field }) => (
                  <NumberInput
                    type="text"
                    id="refreshTokenMaxReuseMs"
                    value={field.value}
                    onPlus={() => field.onChange(field.value! + 1)}
                    onMinus={() => field.onChange(field.value! - 1)}
                    onChange={(event) =>
                      field.onChange(
                        Number((event.target as HTMLInputElement).value),
                      )
                    }
                  />
                )}
              />
            </FormGroup>
          )}
        </FormAccess>
      ),
    },
    {
      title: t("accessTokens"),
      panel: (
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-v5-u-mt-lg"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("accessTokenLifespan")}
            fieldId="accessTokenLifespan"
            labelIcon={
              <HelpItem
                helpText={t("accessTokenLifespanHelp")}
                fieldLabelId="accessTokenLifespan"
              />
            }
          >
            <Controller
              name="accessTokenLifespan"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  validated={
                    field.value! > ssoSessionIdleTimeout!
                      ? "warning"
                      : "default"
                  }
                  className="kc-access-token-lifespan"
                  data-testid="access-token-lifespan-input"
                  aria-label="access-token-lifespan"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
            <FormHelperText>
              <HelperText>
                <HelperTextItem>
                  {t("recommendedSsoTimeout", {
                    time: toHumanFormat(ssoSessionIdleTimeout!, whoAmI.locale),
                  })}
                </HelperTextItem>
              </HelperText>
            </FormHelperText>
          </FormGroup>

          <FormGroup
            label={t("accessTokenLifespanImplicitFlow")}
            fieldId="accessTokenLifespanImplicitFlow"
            labelIcon={
              <HelpItem
                helpText={t("accessTokenLifespanImplicitFlow")}
                fieldLabelId="accessTokenLifespanImplicitFlow"
              />
            }
          >
            <Controller
              name="accessTokenLifespanForImplicitFlow"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-access-token-lifespan-implicit"
                  data-testid="access-token-lifespan-implicit-input"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("clientLoginTimeout")}
            fieldId="clientLoginTimeout"
            labelIcon={
              <HelpItem
                helpText={t("clientLoginTimeoutHelp")}
                fieldLabelId="clientLoginTimeout"
              />
            }
          >
            <Controller
              name="accessCodeLifespan"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-client-login-timeout"
                  data-testid="client-login-timeout-input"
                  aria-label="client-login-timeout"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
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
        </FormAccess>
      ),
    },
    {
      title: t("actionTokens"),
      panel: (
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-v5-u-mt-lg"
          onSubmit={handleSubmit(save)}
        >
          <FormGroup
            label={t("userInitiatedActionLifespan")}
            id="kc-user-initiated-action-lifespan"
            fieldId="userInitiatedActionLifespan"
            labelIcon={
              <HelpItem
                helpText={t("userInitiatedActionLifespanHelp")}
                fieldLabelId="userInitiatedActionLifespan"
              />
            }
          >
            <Controller
              name="actionTokenGeneratedByUserLifespan"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-user-initiated-action-lifespan"
                  data-testid="user-initiated-action-lifespan"
                  aria-label="user-initiated-action-lifespan"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
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
                helpText={t("defaultAdminInitiatedActionLifespanHelp")}
                fieldLabelId="defaultAdminInitiated"
              />
            }
          >
            <Controller
              name="actionTokenGeneratedByAdminLifespan"
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-default-admin-initiated"
                  data-testid="default-admin-initated-input"
                  aria-label="default-admin-initated-input"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
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
            labelIcon={
              <HelpItem
                helpText={t("emailVerificationHelp")}
                fieldLabelId="emailVerification"
              />
            }
          >
            <Controller
              name={`attributes.${beerify(
                "actionTokenGeneratedByUserLifespan.verify-email",
              )}`}
              defaultValue=""
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-email-verification"
                  data-testid="email-verification-input"
                  value={field.value}
                  onChange={(value) => field.onChange(value.toString())}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("idpAccountEmailVerification")}
            fieldId="idpAccountEmailVerification"
            id="idp-acct-label"
            labelIcon={
              <HelpItem
                helpText={t("idpAccountEmailVerificationHelp")}
                fieldLabelId="idpAccountEmailVerification"
              />
            }
          >
            <Controller
              name={`attributes.${beerify(
                "actionTokenGeneratedByUserLifespan.idp-verify-account-via-email",
              )}`}
              defaultValue={""}
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-idp-email-verification"
                  data-testid="idp-email-verification-input"
                  value={field.value}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("forgotPassword")}
            fieldId="forgotPassword"
            id="forgot-password-label"
            labelIcon={
              <HelpItem
                helpText={t("forgotPasswordHelp")}
                fieldLabelId="forgotPassword"
              />
            }
          >
            <Controller
              name={`attributes.${beerify(
                "actionTokenGeneratedByUserLifespan.reset-credentials",
              )}`}
              defaultValue={""}
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-forgot-pw"
                  data-testid="forgot-pw-input"
                  value={field.value}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("executeActions")}
            fieldId="executeActions"
            id="execute-actions"
            labelIcon={
              <HelpItem
                helpText={t("executeActionsHelp")}
                fieldLabelId="executeActions"
              />
            }
          >
            <Controller
              name={`attributes.${beerify(
                "actionTokenGeneratedByUserLifespan.execute-actions",
              )}`}
              defaultValue={""}
              control={control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-execute-actions"
                  data-testid="execute-actions-input"
                  value={field.value}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
          {!isFeatureEnabled(Feature.OpenId4VCI) && (
            <FixedButtonsGroup
              name="tokens-tab"
              isSubmit
              isDisabled={!formState.isDirty}
              reset={() => reset(realm)}
            />
          )}
        </FormAccess>
      ),
    },
    {
      title: t("oid4vciAttributes"),
      isHidden: !isFeatureEnabled(Feature.OpenId4VCI),
      panel: (
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-v5-u-mt-lg"
          onSubmit={handleSubmit(save, onError)}
        >
          <TimeSelectorControl
            name={convertAttributeNameToForm(
              "attributes.vc.c-nonce-lifetime-seconds",
            )}
            label={t("oid4vciNonceLifetime")}
            labelIcon={t("oid4vciNonceLifetimeHelp")}
            controller={{
              defaultValue: 60,
              rules: { min: 30 },
            }}
            min={30}
            units={["second", "minute", "hour"]}
          />
          <TimeSelectorControl
            name={convertAttributeNameToForm(
              "attributes.preAuthorizedCodeLifespanS",
            )}
            label={t("preAuthorizedCodeLifespan")}
            labelIcon={t("preAuthorizedCodeLifespanHelp")}
            controller={{
              defaultValue: 30,
              rules: { min: 30 },
            }}
            min={30}
            units={["second", "minute", "hour"]}
          />
          <FixedButtonsGroup
            name="tokens-tab"
            isSubmit
            isDisabled={!formState.isDirty}
            reset={() => reset(realm)}
          />
        </FormAccess>
      ),
    },
  ];

  return (
    <ScrollForm
      label={t("jumpToSection")}
      className="pf-v5-u-px-lg pf-v5-u-pb-lg"
      sections={sections}
    />
  );
};
