import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  ExpandableSection,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { FormGroupField } from "../component/FormGroupField";
import { SwitchField } from "../component/SwitchField";
import { TextField } from "../component/TextField";

import "./discovery-settings.css";

const PKCE_METHODS = ["plain", "S256"] as const;

type DiscoverySettingsProps = {
  readOnly: boolean;
};

const Fields = ({ readOnly }: DiscoverySettingsProps) => {
  const { t } = useTranslation();
  const [pkceMethodOpen, setPkceMethodOpen] = useState(false);
  const {
    register,
    control,
    formState: { errors },
  } = useFormContext<IdentityProviderRepresentation>();

  const validateSignature = useWatch({
    control,
    name: "config.validateSignature",
  });
  const useJwks = useWatch({
    control,
    name: "config.useJwksUrl",
  });
  const isPkceEnabled = useWatch({
    control,
    name: "config.pkceEnabled",
  });

  return (
    <div className="pf-c-form pf-m-horizontal">
      <FormGroup
        label={t("authorizationUrl")}
        fieldId="kc-authorization-url"
        isRequired
        validated={
          errors.config?.authorizationUrl
            ? ValidatedOptions.error
            : ValidatedOptions.default
        }
        helperTextInvalid={t("required")}
      >
        <KeycloakTextInput
          type="url"
          data-testid="authorizationUrl"
          id="kc-authorization-url"
          validated={
            errors.config?.authorizationUrl
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          isReadOnly={readOnly}
          {...register("config.authorizationUrl", { required: true })}
        />
      </FormGroup>

      <FormGroup
        label={t("tokenUrl")}
        fieldId="tokenUrl"
        isRequired
        validated={
          errors.config?.tokenUrl
            ? ValidatedOptions.error
            : ValidatedOptions.default
        }
        helperTextInvalid={t("required")}
      >
        <KeycloakTextInput
          type="url"
          id="tokenUrl"
          data-testid="tokenUrl"
          validated={
            errors.config?.tokenUrl
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          isReadOnly={readOnly}
          {...register("config.tokenUrl", { required: true })}
        />
      </FormGroup>
      <TextField
        field="config.logoutUrl"
        label="logoutUrl"
        isReadOnly={readOnly}
      />
      <TextField
        field="config.userInfoUrl"
        label="userInfoUrl"
        isReadOnly={readOnly}
      />
      <TextField field="config.issuer" label="issuer" isReadOnly={readOnly} />
      <SwitchField
        field="config.validateSignature"
        label="validateSignature"
        isReadOnly={readOnly}
      />
      {validateSignature === "true" && (
        <>
          <SwitchField
            field="config.useJwksUrl"
            label="useJwksUrl"
            data-testid="useJwksUrl"
            isReadOnly={readOnly}
          />
          {useJwks === "true" ? (
            <TextField
              field="config.jwksUrl"
              label="jwksUrl"
              isReadOnly={readOnly}
            />
          ) : (
            <>
              <FormGroupField label="validatingPublicKey">
                <KeycloakTextArea
                  data-testid="validatingPublicKey"
                  aria-label={t("validatingPublicKey")}
                  {...register("config.publicKeySignatureVerifier")}
                />
              </FormGroupField>
              <TextField
                field="config.publicKeySignatureVerifierKeyId"
                label="validatingPublicKeyId"
                isReadOnly={readOnly}
              />
            </>
          )}
        </>
      )}
      <SwitchField
        field="config.pkceEnabled"
        label="pkceEnabled"
        isReadOnly={readOnly}
      />
      {isPkceEnabled === "true" && (
        <FormGroup
          className="pf-u-pb-3xl"
          label={t("pkceMethod")}
          labelIcon={
            <HelpItem
              helpText={t("pkceMethodHelp")}
              fieldLabelId="pkceMethod"
            />
          }
          fieldId="pkceMethod"
        >
          <Controller
            name="config.pkceMethod"
            defaultValue={PKCE_METHODS[0]}
            control={control}
            render={({ field }) => (
              <Select
                toggleId="pkceMethod"
                required
                direction="down"
                onToggle={() => setPkceMethodOpen(!pkceMethodOpen)}
                onSelect={(_, value) => {
                  field.onChange(value as string);
                  setPkceMethodOpen(false);
                }}
                selections={t(`${field.value}`)}
                variant={SelectVariant.single}
                aria-label={t("pkceMethod")}
                isOpen={pkceMethodOpen}
              >
                {PKCE_METHODS.map((option) => (
                  <SelectOption
                    selected={option === field.value}
                    key={option}
                    value={option}
                  >
                    {t(`${option}`)}
                  </SelectOption>
                ))}
              </Select>
            )}
          />
        </FormGroup>
      )}
    </div>
  );
};

export const DiscoverySettings = ({ readOnly }: DiscoverySettingsProps) => {
  const { t } = useTranslation();
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <>
      {readOnly && (
        <ExpandableSection
          className="keycloak__discovery-settings__metadata"
          toggleText={isExpanded ? t("hideMetaData") : t("showMetaData")}
          onToggle={() => setIsExpanded(!isExpanded)}
          isExpanded={isExpanded}
        >
          <Fields readOnly={readOnly} />
        </ExpandableSection>
      )}
      {!readOnly && <Fields readOnly={readOnly} />}
    </>
  );
};
