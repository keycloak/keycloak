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
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { DefaultSwitchControl } from "../components/SwitchControl";
import { FormattedLink } from "../components/external-link/FormattedLink";
import { FixedButtonsGroup } from "../components/form/FixedButtonGroup";
import { FormAccess } from "../components/form/FormAccess";
import { KeyValueInput } from "../components/key-value-form/KeyValueInput";
import { useRealm } from "../context/realm-context/RealmContext";
import {
  addTrailingSlash,
  convertAttributeNameToForm,
  convertToFormValues,
} from "../util";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";
import { UIRealmRepresentation } from "./RealmSettingsTabs";
import { SIGNATURE_ALGORITHMS } from "../clients/add/SamlSignature";

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

  const setupForm = () => {
    convertToFormValues(realm, setValue);
    setValue(
      "unmanagedAttributePolicy",
      userProfileConfig.unmanagedAttributePolicy ||
        UNMANAGED_ATTRIBUTE_POLICIES[0],
    );
    if (realm.attributes?.["acr.loa.map"]) {
      const result = Object.entries(
        JSON.parse(realm.attributes["acr.loa.map"]),
      ).flatMap(([key, value]) => ({ key, value }));
      result.concat({ key: "", value: "" });
      setValue(
        convertAttributeNameToForm("attributes.acr.loa.map") as any,
        result,
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
          <TextControl name="displayName" label={t("displayName")} />
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
                helpText={t("acrToLoAMappingHelp")}
                fieldLabelId="acrToLoAMapping"
              />
            }
          >
            <KeyValueInput
              label={t("acrToLoAMapping")}
              name={convertAttributeNameToForm("attributes.acr.loa.map")}
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
                    )}realms/${realmName}/.well-known/openid-credential-issuer`}
                    title={t("oid4vcIssuerMetadata")}
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
