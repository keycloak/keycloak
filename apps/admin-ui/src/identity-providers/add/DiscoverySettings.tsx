import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import {
  ExpandableSection,
  FormGroup,
  ValidatedOptions,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import { SwitchField } from "../component/SwitchField";
import { TextField } from "../component/TextField";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { HelpItem } from "../../components/help-enabler/HelpItem";

import "./discovery-settings.css";

const PKCE_METHODS = ["plain", "S256"] as const;

type DiscoverySettingsProps = {
  readOnly: boolean;
};

const Fields = ({ readOnly }: DiscoverySettingsProps) => {
  const { t } = useTranslation("identity-providers");
  const [pkceMethodOpen, setPkceMethodOpen] = useState(false);
  const {
    register,
    control,
    formState: { errors },
  } = useFormContext();

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
        helperTextInvalid={t("common:required")}
      >
        <KeycloakTextInput
          type="text"
          data-testid="authorizationUrl"
          id="kc-authorization-url"
          name="config.authorizationUrl"
          ref={register({ required: true })}
          validated={
            errors.config?.authorizationUrl
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          isReadOnly={readOnly}
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
        helperTextInvalid={t("common:required")}
      >
        <KeycloakTextInput
          type="text"
          id="tokenUrl"
          data-testid="tokenUrl"
          name="config.tokenUrl"
          ref={register({ required: true })}
          validated={
            errors.config?.tokenUrl
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          isReadOnly={readOnly}
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
          {useJwks === "true" && (
            <TextField
              field="config.jwksUrl"
              label="jwksUrl"
              isReadOnly={readOnly}
            />
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
              helpText="identity-providers-help:pkceMethod"
              fieldLabelId="identity-providers:pkceMethod"
            />
          }
          fieldId="pkceMethod"
        >
          <Controller
            name="config.pkceMethod"
            defaultValue={PKCE_METHODS[0]}
            control={control}
            render={({ onChange, value }) => (
              <Select
                toggleId="pkceMethod"
                required
                direction="down"
                onToggle={() => setPkceMethodOpen(!pkceMethodOpen)}
                onSelect={(_, value) => {
                  onChange(value as string);
                  setPkceMethodOpen(false);
                }}
                selections={t(`${value}`)}
                variant={SelectVariant.single}
                aria-label={t("pkceMethod")}
                isOpen={pkceMethodOpen}
              >
                {PKCE_METHODS.map((option) => (
                  <SelectOption
                    selected={option === value}
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
  const { t } = useTranslation("identity-providers");
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
