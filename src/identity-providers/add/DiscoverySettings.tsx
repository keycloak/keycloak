import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useFormContext, useWatch } from "react-hook-form";
import {
  ExpandableSection,
  FormGroup,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";

import { SwitchField } from "../component/SwitchField";
import { TextField } from "../component/TextField";

import "./discovery-settings.css";

type DiscoverySettingsProps = {
  readOnly: boolean;
};

const Fields = ({ readOnly }: DiscoverySettingsProps) => {
  const { t } = useTranslation("identity-providers");
  const { register, control, errors } = useFormContext();

  const validateSignature = useWatch({
    control: control,
    name: "config.validateSignature",
  });
  const useJwks = useWatch({
    control: control,
    name: "config.useJwksUrl",
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
        <TextInput
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
        <TextInput
          type="text"
          id="tokenUrl"
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
