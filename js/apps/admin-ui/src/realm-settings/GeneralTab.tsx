import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  UnmanagedAttributePolicy,
  UserProfileConfig,
} from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import {
  ActionGroup,
  Button,
  ClipboardCopy,
  FormGroup,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  Stack,
  StackItem,
  Switch,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem, TextControl } from "ui-shared";
import { adminClient } from "../admin-client";
import { FormattedLink } from "../components/external-link/FormattedLink";
import { FormAccess } from "../components/form/FormAccess";
import { KeyValueInput } from "../components/key-value-form/KeyValueInput";
import { useRealm } from "../context/realm-context/RealmContext";
import {
  addTrailingSlash,
  convertAttributeNameToForm,
  convertToFormValues,
} from "../util";
import { useFetch } from "../utils/useFetch";
import { UIRealmRepresentation } from "./RealmSettingsTabs";

type RealmSettingsGeneralTabProps = {
  realm: UIRealmRepresentation;
  save: (realm: UIRealmRepresentation) => void;
};

type ExtendedUIRealmRepresentation = UIRealmRepresentation & {
  attributes?: Record<string, any>;
};

type FormFields = Omit<RealmRepresentation, "groups">;

export const RealmSettingsGeneralTab = ({
  realm,
  save,
}: RealmSettingsGeneralTabProps) => {
  const { t } = useTranslation();
  const { realm: realmName } = useRealm();
  const form = useForm<FormFields>();
  const {
    control,
    handleSubmit,
    setValue,
    formState: { isDirty, errors },
  } = form;
  const [open, setOpen] = useState(false);

  const requireSslTypes = ["all", "external", "none"];

  const [userProfileConfig, setUserProfileConfig] =
    useState<UserProfileConfig>();
  const unmanagedAttributePolicies = [
    UnmanagedAttributePolicy.Disabled,
    UnmanagedAttributePolicy.Enabled,
    UnmanagedAttributePolicy.AdminView,
    UnmanagedAttributePolicy.AdminEdit,
  ];
  const [isUnmanagedAttributeOpen, setIsUnmanagedAttributeOpen] =
    useState(false);

  const setupForm = () => {
    convertToFormValues(realm, setValue);
    if ((realm as ExtendedUIRealmRepresentation).attributes?.["acr.loa.map"]) {
      const result = Object.entries(
        JSON.parse(
          (realm as ExtendedUIRealmRepresentation).attributes?.["acr.loa.map"],
        ),
      ).flatMap(([key, value]) => ({ key, value }));
      result.concat({ key: "", value: "" });
      setValue(
        convertAttributeNameToForm("attributes.acr.loa.map") as any,
        result,
      );
    }
  };

  useFetch(
    () => adminClient.users.getProfile({ realm: realmName }),
    (config) => setUserProfileConfig(config),
    [],
  );

  useEffect(setupForm, []);

  return (
    <PageSection variant="light">
      <FormProvider {...form}>
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg"
          onSubmit={handleSubmit((data) => {
            if (
              UnmanagedAttributePolicy.Disabled ===
              userProfileConfig?.unmanagedAttributePolicy
            ) {
              userProfileConfig.unmanagedAttributePolicy = undefined;
            }
            save({ ...data, upConfig: userProfileConfig });
          })}
        >
          <FormGroup
            label={t("realmId")}
            fieldId="kc-realm-id"
            isRequired
            validated={errors.realm ? "error" : "default"}
            helperTextInvalid={errors.realm?.message?.toString()}
          >
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
          </FormGroup>
          <TextControl name="displayName" label={t("displayName")} />
          <TextControl name="displayNameHtml" label={t("htmlDisplayName")} />
          <TextControl
            name={convertAttributeNameToForm("attributes.frontendUrl")}
            label={t("frontendUrl")}
            labelIcon={t("frontendUrlHelp")}
            type="url"
          />
          <FormGroup
            label={t("requireSsl")}
            fieldId="kc-require-ssl"
            labelIcon={
              <HelpItem
                helpText={t("requireSslHelp")}
                fieldLabelId="requireSsl"
              />
            }
          >
            <Controller
              name="sslRequired"
              defaultValue="none"
              control={control}
              render={({ field }) => (
                <Select
                  toggleId="kc-require-ssl"
                  onToggle={() => setOpen(!open)}
                  onSelect={(_, value) => {
                    field.onChange(value as string);
                    setOpen(false);
                  }}
                  selections={field.value}
                  variant={SelectVariant.single}
                  aria-label={t("requireSsl")}
                  isOpen={open}
                >
                  {requireSslTypes.map((sslType) => (
                    <SelectOption
                      selected={sslType === field.value}
                      key={sslType}
                      value={sslType}
                    >
                      {t(`sslType.${sslType}`)}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>
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
            <FormProvider {...form}>
              <KeyValueInput
                label={t("acrToLoAMapping")}
                name={convertAttributeNameToForm("attributes.acr.loa.map")}
              />
            </FormProvider>
          </FormGroup>
          <FormGroup
            hasNoPaddingTop
            label={t("userManagedAccess")}
            labelIcon={
              <HelpItem
                helpText={t("userManagedAccessHelp")}
                fieldLabelId="userManagedAccess"
              />
            }
            fieldId="kc-user-managed-access"
          >
            <Controller
              name="userManagedAccessAllowed"
              control={control}
              defaultValue={false}
              render={({ field }) => (
                <Switch
                  id="kc-user-managed-access"
                  data-testid="user-managed-access-switch"
                  label={t("on")}
                  labelOff={t("off")}
                  isChecked={field.value}
                  onChange={field.onChange}
                  aria-label={t("userManagedAccess")}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("unmanagedAttributes")}
            fieldId="kc-user-profile-unmanaged-attribute-policy"
            labelIcon={
              <HelpItem
                helpText={t("unmanagedAttributesHelpText")}
                fieldLabelId="unmanagedAttributes"
              />
            }
          >
            <Select
              toggleId="kc-user-profile-unmanaged-attribute-policy"
              onToggle={() =>
                setIsUnmanagedAttributeOpen(!isUnmanagedAttributeOpen)
              }
              onSelect={(_, value) => {
                if (userProfileConfig) {
                  userProfileConfig.unmanagedAttributePolicy =
                    value as UnmanagedAttributePolicy;
                  setUserProfileConfig(userProfileConfig);
                }
                setIsUnmanagedAttributeOpen(false);
              }}
              selections={userProfileConfig?.unmanagedAttributePolicy}
              variant={SelectVariant.single}
              isOpen={isUnmanagedAttributeOpen}
              aria-label={t("selectUnmanagedAttributePolicy")}
            >
              {unmanagedAttributePolicies.map((policy) => (
                <SelectOption key={policy} value={policy}>
                  {t(`unmanagedAttributePolicy.${policy}`)}
                </SelectOption>
              ))}
            </Select>
          </FormGroup>
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
                    adminClient.baseUrl,
                  )}realms/${realmName}/.well-known/openid-configuration`}
                  title={t("openIDEndpointConfiguration")}
                />
              </StackItem>
              <StackItem>
                <FormattedLink
                  href={`${addTrailingSlash(
                    adminClient.baseUrl,
                  )}realms/${realmName}/protocol/saml/descriptor`}
                  title={t("samlIdentityProviderMetadata")}
                />
              </StackItem>
            </Stack>
          </FormGroup>

          <ActionGroup>
            <Button
              variant="primary"
              type="submit"
              data-testid="general-tab-save"
              isDisabled={!isDirty}
            >
              {t("save")}
            </Button>
            <Button
              data-testid="general-tab-revert"
              variant="link"
              onClick={setupForm}
            >
              {t("revert")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </FormProvider>
    </PageSection>
  );
};
