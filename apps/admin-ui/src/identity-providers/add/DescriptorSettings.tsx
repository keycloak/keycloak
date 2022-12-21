import {
  ExpandableSection,
  FormGroup,
  NumberInput,
  Select,
  SelectOption,
  SelectVariant,
  ValidatedOptions,
} from "@patternfly/react-core";
import IdentityProviderRepresentation from "libs/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { SwitchField } from "../component/SwitchField";
import { TextField } from "../component/TextField";

import "./discovery-settings.css";

type DescriptorSettingsProps = {
  readOnly: boolean;
};

const Fields = ({ readOnly }: DescriptorSettingsProps) => {
  const { t } = useTranslation("identity-providers");
  const { t: th } = useTranslation("identity-providers-help");

  const {
    register,
    control,
    formState: { errors },
  } = useFormContext<IdentityProviderRepresentation>();
  const [namedPolicyDropdownOpen, setNamedPolicyDropdownOpen] = useState(false);
  const [principalTypeDropdownOpen, setPrincipalTypeDropdownOpen] =
    useState(false);
  const [signatureAlgorithmDropdownOpen, setSignatureAlgorithmDropdownOpen] =
    useState(false);
  const [
    samlSignatureKeyNameDropdownOpen,
    setSamlSignatureKeyNameDropdownOpen,
  ] = useState(false);

  const wantAuthnSigned = useWatch({
    control,
    name: "config.wantAuthnRequestsSigned",
  });

  const validateSignature = useWatch({
    control,
    name: "config.validateSignature",
  });

  const principalType = useWatch({
    control,
    name: "config.principalType",
  });

  return (
    <div className="pf-c-form pf-m-horizontal">
      <FormGroup
        label={t("serviceProviderEntityId")}
        fieldId="kc-saml-service-provider-entity-id"
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:serviceProviderEntityId"
            fieldLabelId="identity-providers:serviceProviderEntityId"
          />
        }
      >
        <KeycloakTextInput
          data-testid="serviceProviderEntityId"
          id="kc-saml-service-provider-entity-id"
          {...register("config.entityId")}
        />
      </FormGroup>
      <FormGroup
        label={t("identityProviderEntityId")}
        fieldId="kc-identity-provider-entity-id"
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:identityProviderEntityId"
            fieldLabelId="identity-providers:identityProviderEntityId"
          />
        }
      >
        <KeycloakTextInput
          data-testid="identityProviderEntityId"
          id="kc-identity-provider-entity-id"
          {...register("config.idpEntityId")}
        />
      </FormGroup>
      <FormGroup
        label={t("ssoServiceUrl")}
        labelIcon={
          <HelpItem
            helpText={th("ssoServiceUrl")}
            fieldLabelId="identity-providers:ssoServiceUrl"
          />
        }
        fieldId="kc-sso-service-url"
        isRequired
        validated={
          errors.config?.singleSignOnServiceUrl
            ? ValidatedOptions.error
            : ValidatedOptions.default
        }
        helperTextInvalid={t("common:required")}
      >
        <KeycloakTextInput
          type="url"
          data-testid="sso-service-url"
          id="kc-sso-service-url"
          validated={
            errors.config?.singleSignOnServiceUrl
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          isReadOnly={readOnly}
          {...register("config.singleSignOnServiceUrl", { required: true })}
        />
      </FormGroup>
      <FormGroup
        label={t("singleLogoutServiceUrl")}
        labelIcon={
          <HelpItem
            helpText={th("singleLogoutServiceUrl")}
            fieldLabelId="identity-providers:singleLogoutServiceUrl"
          />
        }
        fieldId="single-logout-service-url"
        data-testid="single-logout-service-url"
        validated={
          errors.config?.singleLogoutServiceUrl
            ? ValidatedOptions.error
            : ValidatedOptions.default
        }
        helperTextInvalid={t("common:required")}
      >
        <KeycloakTextInput
          type="url"
          id="single-logout-service-url"
          isReadOnly={readOnly}
          {...register("config.singleLogoutServiceUrl")}
        />
      </FormGroup>
      <SwitchField
        field="config.backchannelSupported"
        label="backchannelLogout"
        data-testid="backchannelLogout"
        isReadOnly={readOnly}
      />
      <FormGroup
        label={t("nameIdPolicyFormat")}
        labelIcon={
          <HelpItem
            helpText={th("nameIdPolicyFormat")}
            fieldLabelId="identity-providers:nameIdPolicyFormat"
          />
        }
        fieldId="kc-nameIdPolicyFormat"
        helperTextInvalid={t("common:required")}
      >
        <Controller
          name="config.nameIDPolicyFormat"
          defaultValue={t("persistent")}
          control={control}
          render={({ field }) => (
            <Select
              toggleId="kc-nameIdPolicyFormat"
              onToggle={(isExpanded) => setNamedPolicyDropdownOpen(isExpanded)}
              isOpen={namedPolicyDropdownOpen}
              onSelect={(_, value) => {
                field.onChange(value as string);
                setNamedPolicyDropdownOpen(false);
              }}
              selections={field.value}
              variant={SelectVariant.single}
              isDisabled={readOnly}
            >
              <SelectOption
                data-testid="persistent-option"
                value={"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"}
                isPlaceholder
              >
                {t("persistent")}
              </SelectOption>
              <SelectOption
                data-testid="transient-option"
                value="urn:oasis:names:tc:SAML:2.0:nameid-format:transient"
              >
                {t("transient")}
              </SelectOption>
              <SelectOption
                data-testid="email-option"
                value="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"
              >
                {t("email")}
              </SelectOption>
              <SelectOption
                data-testid="kerberos-option"
                value="urn:oasis:names:tc:SAML:2.0:nameid-format:kerberos"
              >
                {t("kerberos")}
              </SelectOption>

              <SelectOption
                data-testid="x509-option"
                value="urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName"
              >
                {t("x509")}
              </SelectOption>

              <SelectOption
                data-testid="windowsDomainQN-option"
                value="urn:oasis:names:tc:SAML:1.1:nameid-format:WindowsDomainQualifiedName"
              >
                {t("windowsDomainQN")}
              </SelectOption>

              <SelectOption
                data-testid="unspecified-option"
                value={"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified"}
              >
                {t("unspecified")}
              </SelectOption>
            </Select>
          )}
        ></Controller>
      </FormGroup>

      <FormGroup
        label={t("principalType")}
        labelIcon={
          <HelpItem
            helpText={th("principalType")}
            fieldLabelId="identity-providers:principalType"
          />
        }
        fieldId="kc-principalType"
        helperTextInvalid={t("common:required")}
      >
        <Controller
          name="config.principalType"
          defaultValue={t("subjectNameId")}
          control={control}
          render={({ field }) => (
            <Select
              toggleId="kc-principalType"
              onToggle={(isExpanded) =>
                setPrincipalTypeDropdownOpen(isExpanded)
              }
              isOpen={principalTypeDropdownOpen}
              onSelect={(_, value) => {
                field.onChange(value.toString());
                setPrincipalTypeDropdownOpen(false);
              }}
              selections={field.value}
              variant={SelectVariant.single}
              isDisabled={readOnly}
            >
              <SelectOption
                data-testid="subjectNameId-option"
                value="SUBJECT"
                isPlaceholder
              >
                {t("subjectNameId")}
              </SelectOption>
              <SelectOption
                data-testid="attributeName-option"
                value="ATTRIBUTE"
              >
                {t("attributeName")}
              </SelectOption>
              <SelectOption
                data-testid="attributeFriendlyName-option"
                value="FRIENDLY_ATTRIBUTE"
              >
                {t("attributeFriendlyName")}
              </SelectOption>
            </Select>
          )}
        ></Controller>
      </FormGroup>

      {principalType?.includes("ATTRIBUTE") && (
        <FormGroup
          label={t("principalAttribute")}
          labelIcon={
            <HelpItem
              helpText={th("principalAttribute")}
              fieldLabelId="identity-providers:principalAttribute"
            />
          }
          fieldId="principalAttribute"
        >
          <KeycloakTextInput
            id="principalAttribute"
            data-testid="principalAttribute"
            isReadOnly={readOnly}
            {...register("config.principalAttribute")}
          />
        </FormGroup>
      )}
      <SwitchField
        field="config.allowCreate"
        label="allowCreate"
        isReadOnly={readOnly}
      />

      <SwitchField
        field="config.postBindingResponse"
        label="httpPostBindingResponse"
        isReadOnly={readOnly}
      />

      <SwitchField
        field="config.postBindingAuthnRequest"
        label="httpPostBindingAuthnRequest"
        isReadOnly={readOnly}
      />

      <SwitchField
        field="config.postBindingLogout"
        label="httpPostBindingLogout"
        isReadOnly={readOnly}
      />

      <SwitchField
        field="config.wantAuthnRequestsSigned"
        label="wantAuthnRequestsSigned"
        isReadOnly={readOnly}
      />

      {wantAuthnSigned === "true" && (
        <>
          <FormGroup
            label={t("signatureAlgorithm")}
            labelIcon={
              <HelpItem
                helpText={th("signatureAlgorithm")}
                fieldLabelId="identity-providers:signatureAlgorithm"
              />
            }
            fieldId="kc-signatureAlgorithm"
          >
            <Controller
              name="config.signatureAlgorithm"
              defaultValue="RSA_SHA256"
              control={control}
              render={({ field }) => (
                <Select
                  toggleId="kc-signatureAlgorithm"
                  onToggle={(isExpanded) =>
                    setSignatureAlgorithmDropdownOpen(isExpanded)
                  }
                  isOpen={signatureAlgorithmDropdownOpen}
                  onSelect={(_, value) => {
                    field.onChange(value.toString());
                    setSignatureAlgorithmDropdownOpen(false);
                  }}
                  selections={field.value}
                  variant={SelectVariant.single}
                  isDisabled={readOnly}
                >
                  <SelectOption value="RSA_SHA1" />
                  <SelectOption value="RSA_SHA256" isPlaceholder />
                  <SelectOption value="RSA_SHA256_MGF1" />
                  <SelectOption value="RSA_SHA512" />
                  <SelectOption value="RSA_SHA512_MGF1" />
                  <SelectOption value="DSA_SHA1" />
                </Select>
              )}
            ></Controller>
          </FormGroup>
          <FormGroup
            label={t("samlSignatureKeyName")}
            labelIcon={
              <HelpItem
                helpText={th("samlSignatureKeyName")}
                fieldLabelId="identity-providers:samlSignatureKeyName"
              />
            }
            fieldId="kc-samlSignatureKeyName"
          >
            <Controller
              name="config.xmlSigKeyInfoKeyNameTransformer"
              defaultValue={t("keyID")}
              control={control}
              render={({ field }) => (
                <Select
                  toggleId="kc-samlSignatureKeyName"
                  onToggle={(isExpanded) =>
                    setSamlSignatureKeyNameDropdownOpen(isExpanded)
                  }
                  isOpen={samlSignatureKeyNameDropdownOpen}
                  onSelect={(_, value) => {
                    field.onChange(value.toString());
                    setSamlSignatureKeyNameDropdownOpen(false);
                  }}
                  selections={field.value}
                  variant={SelectVariant.single}
                  isDisabled={readOnly}
                >
                  <SelectOption value="NONE" />
                  <SelectOption value={t("keyID")} isPlaceholder />
                  <SelectOption value={t("certSubject")} />
                </Select>
              )}
            ></Controller>
          </FormGroup>
        </>
      )}

      <SwitchField
        field="config.wantAssertionsSigned"
        label="wantAssertionsSigned"
        isReadOnly={readOnly}
      />

      <SwitchField
        field="config.wantAssertionsEncrypted"
        label="wantAssertionsEncrypted"
        isReadOnly={readOnly}
      />
      <SwitchField
        field="config.forceAuthn"
        label="forceAuthentication"
        isReadOnly={readOnly}
      />

      <SwitchField
        field="config.validateSignature"
        label="validateSignature"
        isReadOnly={readOnly}
      />
      {validateSignature === "true" && (
        <TextField
          field="config.signingCertificate"
          label="validatingX509Certs"
          data-testid="validatingX509Certs"
          isReadOnly={readOnly}
        />
      )}
      <SwitchField
        field="config.signSpMetadata"
        label="signServiceProviderMetadata"
        data-testid="signServiceProviderMetadata"
        isReadOnly={readOnly}
      />
      <SwitchField
        field="config.loginHint"
        label="passSubject"
        data-testid="passSubject"
        isReadOnly={readOnly}
      />

      <FormGroup
        label={t("allowedClockSkew")}
        labelIcon={
          <HelpItem
            helpText={th("allowedClockSkew")}
            fieldLabelId="identity-providers:allowedClockSkew"
          />
        }
        fieldId="allowedClockSkew"
        helperTextInvalid={t("common:required")}
      >
        <Controller
          name="config.allowedClockSkew"
          defaultValue={0}
          control={control}
          render={({ field }) => {
            const v = Number(field.value);
            return (
              <NumberInput
                data-testid="allowedClockSkew"
                inputName="allowedClockSkew"
                min={0}
                max={2147483}
                value={v}
                readOnly
                onPlus={() => field.onChange(v + 1)}
                onMinus={() => field.onChange(v - 1)}
                onChange={(event) => {
                  const value = Number(
                    (event.target as HTMLInputElement).value
                  );
                  field.onChange(value < 0 ? 0 : value);
                }}
              />
            );
          }}
        />
      </FormGroup>

      <FormGroup
        label={t("attributeConsumingServiceIndex")}
        labelIcon={
          <HelpItem
            helpText={th("attributeConsumingServiceIndex")}
            fieldLabelId="identity-providers:attributeConsumingServiceIndex"
          />
        }
        fieldId="attributeConsumingServiceIndex"
        helperTextInvalid={t("common:required")}
      >
        <Controller
          name="config.attributeConsumingServiceIndex"
          defaultValue={0}
          control={control}
          render={({ field }) => {
            const v = Number(field.value);
            return (
              <NumberInput
                data-testid="attributeConsumingServiceIndex"
                inputName="attributeConsumingServiceIndex"
                min={0}
                max={2147483}
                value={v}
                readOnly
                onPlus={() => field.onChange(v + 1)}
                onMinus={() => field.onChange(v - 1)}
                onChange={(event) => {
                  const value = Number(
                    (event.target as HTMLInputElement).value
                  );
                  field.onChange(value < 0 ? 0 : value);
                }}
              />
            );
          }}
        />
      </FormGroup>

      <FormGroup
        label={t("attributeConsumingServiceName")}
        labelIcon={
          <HelpItem
            helpText={th("attributeConsumingServiceName")}
            fieldLabelId="identity-providers:attributeConsumingServiceName"
          />
        }
        fieldId="attributeConsumingServiceName"
        helperTextInvalid={t("common:required")}
      >
        <KeycloakTextInput
          id="attributeConsumingServiceName"
          data-testid="attributeConsumingServiceName"
          isReadOnly={readOnly}
          {...register("config.attributeConsumingServiceName")}
        />
      </FormGroup>
    </div>
  );
};

export const DescriptorSettings = ({ readOnly }: DescriptorSettingsProps) => {
  const { t } = useTranslation("identity-providers");
  const [isExpanded, setIsExpanded] = useState(false);

  return readOnly ? (
    <ExpandableSection
      className="keycloak__discovery-settings__metadata"
      toggleText={isExpanded ? t("hideMetaData") : t("showMetaData")}
      onToggle={(isOpen) => setIsExpanded(isOpen)}
      isExpanded={isExpanded}
    >
      <Fields readOnly={readOnly} />
    </ExpandableSection>
  ) : (
    <Fields readOnly={readOnly} />
  );
};
