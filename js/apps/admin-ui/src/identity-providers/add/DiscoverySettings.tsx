import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { ExpandableSection } from "@patternfly/react-core";
import { useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  SelectControl,
  TextAreaControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { DefaultSwitchControl } from "../../components/SwitchControl";

import "./discovery-settings.css";

const PKCE_METHODS = ["plain", "S256"] as const;

type DiscoverySettingsProps = {
  readOnly: boolean;
};

const Fields = ({ readOnly }: DiscoverySettingsProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext<IdentityProviderRepresentation>();

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
    <div className="pf-v5-c-form pf-m-horizontal">
      <TextControl
        name="config.authorizationUrl"
        label={t("authorizationUrl")}
        type="url"
        readOnly={readOnly}
        rules={{
          required: t("required"),
        }}
      />
      <TextControl
        name="config.tokenUrl"
        label={t("tokenUrl")}
        type="url"
        readOnly={readOnly}
        rules={{
          required: t("required"),
        }}
      />
      <TextControl
        name="config.logoutUrl"
        label={t("logoutUrl")}
        readOnly={readOnly}
      />
      <TextControl
        name="config.userInfoUrl"
        label={t("userInfoUrl")}
        readOnly={readOnly}
      />
      <TextControl
        name="config.issuer"
        label={t("issuer")}
        readOnly={readOnly}
      />
      <DefaultSwitchControl
        name="config.validateSignature"
        label={t("validateSignature")}
        isDisabled={readOnly}
        stringify
      />
      {validateSignature === "true" && (
        <>
          <DefaultSwitchControl
            name="config.useJwksUrl"
            label={t("useJwksUrl")}
            isDisabled={readOnly}
            stringify
          />
          {useJwks === "true" ? (
            <TextAreaControl
              name="config.jwksUrl"
              label={t("jwksUrl")}
              readOnly={readOnly}
            />
          ) : (
            <>
              <TextControl
                name="config.publicKeySignatureVerifier"
                label="validatingPublicKey"
              />
              <TextControl
                name="config.publicKeySignatureVerifierKeyId"
                label={t("validatingPublicKeyId")}
                readOnly={readOnly}
              />
            </>
          )}
        </>
      )}
      <DefaultSwitchControl
        name="config.pkceEnabled"
        label={t("pkceEnabled")}
        isDisabled={readOnly}
        stringify
      />
      {isPkceEnabled === "true" && (
        <SelectControl
          name="config.pkceMethod"
          label={t("pkceMethod")}
          labelIcon={t("pkceMethodHelp")}
          controller={{
            defaultValue: PKCE_METHODS[0],
          }}
          options={PKCE_METHODS.map((option) => ({
            key: option,
            value: t(option),
          }))}
        />
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
