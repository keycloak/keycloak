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
  isOIDC: boolean;
};

const Fields = ({ readOnly, isOIDC }: DiscoverySettingsProps) => {
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
      {isOIDC && (
        <TextControl
          name="config.logoutUrl"
          label={t("logoutUrl")}
          readOnly={readOnly}
        />
      )}
      <TextControl
        name="config.userInfoUrl"
        label={t("userInfoUrl")}
        readOnly={readOnly}
        rules={{
          required: isOIDC ? "" : t("required"),
        }}
      />
      <TextControl
        name="config.tokenIntrospectionUrl"
        label={t("tokenIntrospectionUrl")}
        type="url"
        readOnly={readOnly}
      />
      {isOIDC && (
        <TextControl
          name="config.issuer"
          label={t("issuer")}
          readOnly={readOnly}
        />
      )}
      {isOIDC && (
        <>
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
                labelIcon={t("useJwksUrlHelp")}
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
                  <TextAreaControl
                    name="config.publicKeySignatureVerifier"
                    label={t("validatingPublicKey")}
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

export const DiscoverySettings = ({
  readOnly,
  isOIDC,
}: DiscoverySettingsProps) => {
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
          <Fields readOnly={readOnly} isOIDC={isOIDC} />
        </ExpandableSection>
      )}
      {!readOnly && <Fields readOnly={readOnly} isOIDC={isOIDC} />}
    </>
  );
};
