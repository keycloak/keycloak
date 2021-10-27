import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import {
  ExpandableSection,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { SwitchField } from "../component/SwitchField";
import { TextField } from "../component/TextField";

import "./discovery-settings.css";

type DescriptorSettingsProps = {
  readOnly: boolean;
};

const Fields = ({ readOnly }: DescriptorSettingsProps) => {
  const { t } = useTranslation("identity-providers");
  const { t: th } = useTranslation("identity-providers-help");

  const { register, control, errors } = useFormContext();
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
    defaultValue: "",
  });

  return (
    <div className="pf-c-form pf-m-horizontal">
      <FormGroup
        label={t("ssoServiceUrl")}
        labelIcon={
          <HelpItem
            helpText={th("ssoServiceUrl")}
            forLabel={t("ssoServiceUrl")}
            forID="kc-sso-service-url"
          />
        }
        fieldId="kc-sso-service-url"
        isRequired
        validated={
          errors.config?.authorizationUrl
            ? ValidatedOptions.error
            : ValidatedOptions.default
        }
        helperTextInvalid={t("common:required")}
      >
        <TextInput
          type="text"
          data-testid="sso-service-url"
          id="kc-sso-service-url"
          name="config.singleSignOnServiceUrl"
          ref={register({ required: true })}
          validated={
            errors.config?.singleSignOnServiceUrl
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          isReadOnly={readOnly}
        />
      </FormGroup>
      <FormGroup
        label={t("singleLogoutServiceUrl")}
        labelIcon={
          <HelpItem
            helpText={th("singleLogoutServiceUrl")}
            forLabel={t("singleLogoutServiceUrl")}
            forID="single-logout-service-url"
          />
        }
        fieldId="single-logout-service-url"
        validated={
          errors.config?.singleLogoutServiceUrl
            ? ValidatedOptions.error
            : ValidatedOptions.default
        }
        helperTextInvalid={t("common:required")}
      >
        <TextInput
          type="text"
          id="single-logout-service-url"
          name="config.singleLogoutServiceUrl"
          ref={register}
          isReadOnly={readOnly}
        />
      </FormGroup>
      <SwitchField
        field="config.backchannelSupported"
        label="backchannelLogout"
        isReadOnly={readOnly}
      />
      <FormGroup
        label={t("nameIdPolicyFormat")}
        labelIcon={
          <HelpItem
            helpText={th("nameIdPolicyFormat")}
            forLabel={t("nameIdPolicyFormat")}
            forID="kc-nameIdPolicyFormat"
          />
        }
        fieldId="kc-nameIdPolicyFormat"
        helperTextInvalid={t("common:required")}
      >
        <Controller
          name="config.nameIDPolicyFormat"
          defaultValue={t("persistent")}
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="kc-nameIdPolicyFormat"
              onToggle={(isExpanded) => setNamedPolicyDropdownOpen(isExpanded)}
              isOpen={namedPolicyDropdownOpen}
              onSelect={(_, value) => {
                onChange(value as string);
                setNamedPolicyDropdownOpen(false);
              }}
              selections={value}
              variant={SelectVariant.single}
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
            forLabel={t("principalType")}
            forID="kc-principalType"
          />
        }
        fieldId="kc-principalType"
        helperTextInvalid={t("common:required")}
      >
        <Controller
          name="config.principalType"
          defaultValue={t("subjectNameId")}
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="kc-principalType"
              onToggle={(isExpanded) =>
                setPrincipalTypeDropdownOpen(isExpanded)
              }
              isOpen={principalTypeDropdownOpen}
              onSelect={(_, value) => {
                onChange(value.toString());
                setPrincipalTypeDropdownOpen(false);
              }}
              selections={value}
              variant={SelectVariant.single}
            >
              <SelectOption
                data-testid="subjectNameId-option"
                value={t("subjectNameId")}
                isPlaceholder
              />
              <SelectOption
                data-testid="attributeName-option"
                value={t("attributeName")}
              />
              <SelectOption
                data-testid="attributeFriendlyName-option"
                value={t("attributeFriendlyName")}
              />
            </Select>
          )}
        ></Controller>
      </FormGroup>

      {principalType.includes("Attribute") && (
        <FormGroup
          label={t("principalAttribute")}
          labelIcon={
            <HelpItem
              helpText={th("principalAttribute")}
              forLabel={t("principalAttribute")}
              forID="principalAttribute"
            />
          }
          fieldId="principalAttribute"
        >
          <TextInput
            type="text"
            id="principalAttribute"
            name="config.principalAttribute"
            ref={register}
            isReadOnly={readOnly}
          />
        </FormGroup>
      )}
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
                forLabel={t("signatureAlgorithm")}
                forID="kc-signatureAlgorithm"
              />
            }
            fieldId="kc-signatureAlgorithm"
          >
            <Controller
              name="config.signatureAlgorithm"
              defaultValue="RSA_SHA256"
              control={control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="kc-signatureAlgorithm"
                  onToggle={(isExpanded) =>
                    setSignatureAlgorithmDropdownOpen(isExpanded)
                  }
                  isOpen={signatureAlgorithmDropdownOpen}
                  onSelect={(_, value) => {
                    onChange(value.toString());
                    setSignatureAlgorithmDropdownOpen(false);
                  }}
                  selections={value}
                  variant={SelectVariant.single}
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
                forLabel={t("samlSignatureKeyName")}
                forID="kc-samlSignatureKeyName"
              />
            }
            fieldId="kc-samlSignatureKeyName"
          >
            <Controller
              name="config.xmlSigKeyInfoKeyNameTransformer"
              defaultValue={t("keyID")}
              control={control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="kc-samlSignatureKeyName"
                  onToggle={(isExpanded) =>
                    setSamlSignatureKeyNameDropdownOpen(isExpanded)
                  }
                  isOpen={samlSignatureKeyNameDropdownOpen}
                  onSelect={(_, value) => {
                    onChange(value.toString());
                    setSamlSignatureKeyNameDropdownOpen(false);
                  }}
                  selections={value}
                  variant={SelectVariant.single}
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
          isReadOnly={readOnly}
        />
      )}
      <SwitchField
        field="config.signSpMetadata"
        label="signServiceProviderMetadata"
        isReadOnly={readOnly}
      />
      <SwitchField
        field="config.passSubject"
        label="passSubject"
        isReadOnly={readOnly}
      />

      <FormGroup
        label={t("allowedClockSkew")}
        labelIcon={
          <HelpItem
            helpText={th("allowedClockSkew")}
            forLabel={t("allowedClockSkew")}
            forID="allowedClockSkew"
          />
        }
        fieldId="allowedClockSkew"
        helperTextInvalid={t("common:required")}
      >
        <TextInput
          type="number"
          min="0"
          max="2147483"
          id="allowedClockSkew"
          name="config.allowedClockSkew"
          ref={register}
          isReadOnly={readOnly}
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
