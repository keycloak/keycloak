import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  UnmanagedAttributePolicy,
  UserProfileConfig,
} from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import {
  FormErrorText,
  HelpItem,
  KeycloakSpinner,
  SelectControl,
  TextControl,
  useAlerts,
  useEnvironment,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  ClipboardCopy,
  FormGroup,
  PageSection,
  Stack,
  StackItem,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { DefaultSwitchControl } from "../components/SwitchControl";
import { FormattedLink } from "../components/external-link/FormattedLink";
import { FixedButtonsGroup } from "../components/form/FixedButtonGroup";
import { FormAccess } from "../components/form/FormAccess";
import { RealmLoAMapping } from "../components/realm-loa-mapping/RealmLoAMapping";
import { useRealm } from "../context/realm-context/RealmContext";
import {
  addTrailingSlash,
  convertAttributeNameToForm,
  convertToFormValues,
} from "../util";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";
import { UIRealmRepresentation } from "./RealmSettingsTabs";
import { SIGNATURE_ALGORITHMS } from "../clients/add/SamlSignature";
import type { RealmLoAMappingType } from "../components/realm-loa-mapping/RealmLoAMapping";
import {
  deleteRealmSsfQueuedEvents,
  useSsfTransmitterDisableConfirmDialog,
} from "./ssf/SsfTransmitterDisableConfirmDialog";

type RealmSettingsGeneralTabProps = {
  realm: UIRealmRepresentation;
  save: (realm: UIRealmRepresentation) => Promise<void>;
};

export const RealmSettingsGeneralTab = ({
  realm,
  save,
}: RealmSettingsGeneralTabProps) => {
  const { adminClient } = useAdminClient();

  const { realm: realmName } = useRealm();
  const [userProfileConfig, setUserProfileConfig] =
    useState<UserProfileConfig>();

  useFetch(
    () => adminClient.users.getProfile({ realm: realmName }),
    (config) => setUserProfileConfig(config),
    [],
  );

  if (!userProfileConfig) {
    return <KeycloakSpinner />;
  }

  return (
    <RealmSettingsGeneralTabForm
      realm={realm}
      save={save}
      userProfileConfig={userProfileConfig}
    />
  );
};

type RealmSettingsGeneralTabFormProps = {
  realm: UIRealmRepresentation;
  save: (realm: UIRealmRepresentation) => Promise<void>;
  userProfileConfig: UserProfileConfig;
};

type FormFields = Omit<RealmRepresentation, "groups"> & {
  unmanagedAttributePolicy: UnmanagedAttributePolicy;
};

const REQUIRE_SSL_TYPES = ["all", "external", "none"];

const UNMANAGED_ATTRIBUTE_POLICIES = [
  UnmanagedAttributePolicy.Disabled,
  UnmanagedAttributePolicy.Enabled,
  UnmanagedAttributePolicy.AdminView,
  UnmanagedAttributePolicy.AdminEdit,
];

function RealmSettingsGeneralTabForm({
  realm,
  save,
  userProfileConfig,
}: RealmSettingsGeneralTabFormProps) {
  const {
    environment: { serverBaseUrl },
  } = useEnvironment();

  const { t } = useTranslation();
  const { realm: realmName } = useRealm();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const form = useForm<FormFields>();
  const {
    control,
    handleSubmit,
    setValue,
    formState: { errors },
  } = form;
  const isFeatureEnabled = useIsFeatureEnabled();
  const isOrganizationsEnabled = isFeatureEnabled(Feature.Organizations);
  const isAdminPermissionsV2Enabled = isFeatureEnabled(
    Feature.AdminFineGrainedAuthzV2,
  );
  const isOpenid4vciEnabled = isFeatureEnabled(Feature.OpenId4VCI);
  const isStepUpAuthenticationSaml = isFeatureEnabled(
    Feature.StepUpAuthenticationSaml,
  );
  const isScimApiEnabled = isFeatureEnabled(Feature.ScimApi);

  const isSsfEnabled = isFeatureEnabled(Feature.Ssf);

  const ssfTransmitterEnabled = useWatch({
    control,
    name: convertAttributeNameToForm<FormFields>(
      "attributes.ssf.transmitterEnabled",
    ) as any,
  });

  // Disabling the transmitter at the realm level has cascading effects
  // (silent receiver pause, queued events deferred or dead-lettered after
  // outbox-pending-max-age, all SSF endpoints 404). Surface those to the
  // admin so the off transition is a deliberate choice rather than an
  // accidental flip. The toggle's onChange below opens this dialog when
  // going from on to off; cancelling reverts the field back to "true".
  const [toggleSsfDisableDialog, SsfDisableConfirm] =
    useSsfTransmitterDisableConfirmDialog({
      onConfirm: () => {
        // No-op: the inner Controller already flipped the field to "false"
        // when the admin clicked the switch; confirming just lets that
        // value stand until the user hits the form's Save button.
      },
      onCancel: () => {
        setValue(
          convertAttributeNameToForm<FormFields>(
            "attributes.ssf.transmitterEnabled",
          ) as any,
          "true",
        );
      },
    });

  const setupForm = () => {
    convertToFormValues(realm, setValue);
    setValue(
      "unmanagedAttributePolicy",
      userProfileConfig.unmanagedAttributePolicy ||
        UNMANAGED_ATTRIBUTE_POLICIES[0],
    );
    if (realm.attributes?.["acr.loa.map"]) {
      const acrLoaMap = Object.entries(
        JSON.parse(realm.attributes["acr.loa.map"]),
      ).flatMap(([acr, loa]) => ({ acr, loa }) as RealmLoAMappingType);

      if (isStepUpAuthenticationSaml && realm.attributes["acr.uri.map"]) {
        const acrUriMap = JSON.parse(realm.attributes["acr.uri.map"]);
        acrLoaMap.forEach((row) => (row.uri = acrUriMap?.[row.acr]));
      }

      setValue(
        convertAttributeNameToForm("attributes.acr.loa.map") as any,
        acrLoaMap,
      );
    }
  };

  useEffect(setupForm, []);

  const onSubmit = handleSubmit(
    async ({ unmanagedAttributePolicy, ...data }) => {
      const upConfig = { ...userProfileConfig };

      if (unmanagedAttributePolicy === UnmanagedAttributePolicy.Disabled) {
        delete upConfig.unmanagedAttributePolicy;
      } else {
        upConfig.unmanagedAttributePolicy = unmanagedAttributePolicy;
      }

      // Detect a true -> false transition on the SSF Transmitter realm
      // toggle so we can drop queued events as part of the same save
      // flow. Compare the persisted previous state to the new form
      // value — captured before save() so the comparison is well-defined
      // regardless of when the actual write happens.
      const wasSsfTransmitterEnabled =
        realm.attributes?.["ssf.transmitterEnabled"] === "true";
      const isSsfTransmitterEnabledAfter =
        ssfTransmitterEnabled?.toString() === "true";

      if (wasSsfTransmitterEnabled && !isSsfTransmitterEnabledAfter) {
        // Cleanup runs BEFORE save while the SSF admin resource is
        // still reachable. Once save() persists transmitterEnabled=false,
        // SsfAdminRealmResourceProviderFactory gates /ssf/* off and the
        // DELETE would 404. Best-effort: a cleanup failure surfaces as
        // a toast but doesn't block the disable — outbox-pending-max-age
        // backstops any leftover PENDING rows.
        try {
          await deleteRealmSsfQueuedEvents(adminClient, realmName);
          addAlert(t("ssfTransmitterDisableEventsCleared"));
        } catch (error) {
          addError("ssfTransmitterDisableEventsClearFailed", error);
        }
      }

      await save({ ...data, upConfig });
    },
  );

  return (
    <PageSection variant="light">
      <FormProvider {...form}>
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg"
          onSubmit={onSubmit}
        >
          <FormGroup label={t("realmName")} fieldId="kc-realm-id" isRequired>
            <Controller
              name="realm"
              control={control}
              rules={{
                required: { value: true, message: t("required") },
              }}
              defaultValue=""
              render={({ field }) => (
                <ClipboardCopy
                  data-testid="realmName"
                  onChange={field.onChange}
                >
                  {field.value}
                </ClipboardCopy>
              )}
            />
            {errors.realm && (
              <FormErrorText
                data-testid="realm-id-error"
                message={errors.realm.message as string}
              />
            )}
          </FormGroup>
          <TextControl
            name="displayName"
            label={t("displayName")}
            labelIcon={t("realmDisplayNameHelp")}
          />
          <TextControl name="displayNameHtml" label={t("htmlDisplayName")} />
          <TextControl
            name={convertAttributeNameToForm("attributes.frontendUrl")}
            type="url"
            label={t("frontendUrl")}
            labelIcon={t("frontendUrlHelp")}
          />
          <SelectControl
            name="sslRequired"
            label={t("requireSsl")}
            labelIcon={t("requireSslHelp")}
            controller={{
              defaultValue: "none",
            }}
            options={REQUIRE_SSL_TYPES.map((sslType) => ({
              key: sslType,
              value: t(`sslType.${sslType}`),
            }))}
          />
          <FormGroup
            label={t("acrToLoAMapping")}
            fieldId="acrToLoAMapping"
            labelIcon={
              <HelpItem
                helpText={
                  isStepUpAuthenticationSaml
                    ? t("acrToLoAMappingRealmSamlHelp")
                    : t("acrToLoAMappingHelp")
                }
                fieldLabelId="acrToLoAMapping"
              />
            }
          >
            <RealmLoAMapping
              label={t("acrToLoAMapping")}
              name={convertAttributeNameToForm("attributes.acr.loa.map")}
              uri={isStepUpAuthenticationSaml}
            />
          </FormGroup>
          <DefaultSwitchControl
            name="userManagedAccessAllowed"
            label={t("userManagedAccess")}
            labelIcon={t("userManagedAccessHelp")}
          />
          {isOrganizationsEnabled && (
            <DefaultSwitchControl
              name="organizationsEnabled"
              label={t("organizationsEnabled")}
              labelIcon={t("organizationsEnabledHelp")}
            />
          )}
          {isAdminPermissionsV2Enabled && (
            <DefaultSwitchControl
              name="adminPermissionsEnabled"
              label={t("adminPermissionsEnabled")}
              labelIcon={t("adminPermissionsEnabledHelp")}
            />
          )}
          {isOpenid4vciEnabled && (
            <DefaultSwitchControl
              name="verifiableCredentialsEnabled"
              label={t("verifiableCredentialsEnabled")}
              labelIcon={t("verifiableCredentialsEnabledHelp")}
            />
          )}
          {isScimApiEnabled && (
            <DefaultSwitchControl
              name="scimApiEnabled"
              label={t("scimApiEnabled")}
              labelIcon={t("scimApiEnabledHelp")}
            />
          )}
          {isSsfEnabled && (
            <>
              <DefaultSwitchControl
                name={convertAttributeNameToForm<FormFields>(
                  "attributes.ssf.transmitterEnabled",
                )}
                label={t("ssfTransmitterEnabled")}
                labelIcon={t("ssfTransmitterEnabledHelp")}
                stringify
                onChange={(_e, checked) => {
                  // Off transition only — surface the consequences before
                  // the admin commits the form save. Cancelling reverts.
                  if (!checked) {
                    toggleSsfDisableDialog();
                  }
                }}
              />
              <SsfDisableConfirm />
            </>
          )}
          <SelectControl
            name="unmanagedAttributePolicy"
            label={t("unmanagedAttributes")}
            labelIcon={t("unmanagedAttributesHelpText")}
            controller={{
              defaultValue: UNMANAGED_ATTRIBUTE_POLICIES[0],
            }}
            options={UNMANAGED_ATTRIBUTE_POLICIES.map((policy) => ({
              key: policy,
              value: t(`unmanagedAttributePolicy.${policy}`),
            }))}
          />
          <SelectControl
            name={convertAttributeNameToForm<FormFields>(
              "attributes.saml.signature.algorithm",
            )}
            label={t("signatureAlgorithmIdentityProviderMetadata")}
            labelIcon={t("signatureAlgorithmIdentityProviderMetadataHelp")}
            controller={{
              defaultValue: "",
            }}
            options={[
              { key: "", value: t("choose") },
              ...SIGNATURE_ALGORITHMS.map((v) => ({ key: v, value: v })),
            ]}
          />
          <FormGroup
            label={t("endpoints")}
            labelIcon={
              <HelpItem
                helpText={t("endpointsHelp")}
                fieldLabelId="endpoints"
              />
            }
            fieldId="kc-endpoints"
          >
            <Stack>
              <StackItem>
                <FormattedLink
                  href={`${addTrailingSlash(
                    serverBaseUrl,
                  )}realms/${realmName}/.well-known/openid-configuration`}
                  title={t("openIDEndpointConfiguration")}
                />
              </StackItem>
              <StackItem>
                <FormattedLink
                  href={`${addTrailingSlash(
                    serverBaseUrl,
                  )}realms/${realmName}/protocol/saml/descriptor`}
                  title={t("samlIdentityProviderMetadata")}
                />
              </StackItem>
              {isOpenid4vciEnabled && realm.verifiableCredentialsEnabled && (
                <StackItem>
                  <FormattedLink
                    href={`${addTrailingSlash(
                      serverBaseUrl,
                    )}.well-known/openid-credential-issuer/realms/${realmName}`}
                    title={t("oid4vcIssuerMetadata")}
                  />
                </StackItem>
              )}
              {isScimApiEnabled && realm.scimApiEnabled && (
                <StackItem>
                  <FormattedLink
                    href={`${addTrailingSlash(
                      serverBaseUrl,
                    )}realms/${realmName}/scim/v2`}
                    title={t("SCIM Endpoint")}
                  />
                </StackItem>
              )}
              {isSsfEnabled && ssfTransmitterEnabled?.toString() === "true" && (
                <StackItem>
                  <FormattedLink
                    href={`${addTrailingSlash(
                      serverBaseUrl,
                    )}realms/${realmName}/.well-known/ssf-configuration`}
                    title={t("ssfConfigurationMetadata")}
                  />
                </StackItem>
              )}
            </Stack>
          </FormGroup>
          <FixedButtonsGroup
            name="realmSettingsGeneralTab"
            reset={setupForm}
            isSubmit
          />
        </FormAccess>
      </FormProvider>
    </PageSection>
  );
}
