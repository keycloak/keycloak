import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { ExpandableSection } from "@patternfly/react-core";
import { useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { SelectControl, TextControl } from "@keycloak/keycloak-ui-shared";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { JwksSettings } from "./JwksSettings";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";

import "./discovery-settings.css";

const PKCE_METHODS = ["plain", "S256"] as const;

type DiscoverySettingsProps = {
  readOnly: boolean;
  isOIDC: boolean;
};

const MetadataFields = ({ readOnly, isOIDC }: DiscoverySettingsProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext<IdentityProviderRepresentation>();

  const validateSignature = useWatch({
    control,
    name: "config.validateSignature",
  });
  const jwtAuthorizationGrantEnabled = useWatch({
    control,
    name: "config.jwtAuthorizationGrantEnabled",
  });
  const supportsClientAssertions = useWatch({
    control,
    name: "config.supportsClientAssertions",
  });
  const isParEnabled = useWatch({
    control,
    name: "config.pushedAuthorizationRequestsEnabled",
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
      <TextControl
        name="config.pushedAuthorizationRequestUrl"
        label={t("pushedAuthorizationRequestUrl")}
        labelIcon={t("pushedAuthorizationRequestUrlHelp")}
        type="url"
        readOnly={readOnly}
        rules={{
          required: isParEnabled === "true" ? t("required") : "",
        }}
      />
      <TextControl
        name="config.issuer"
        label={t("issuer")}
        readOnly={readOnly}
      />
      <DefaultSwitchControl
        name="config.validateDiscoveryIssuer"
        label={t("validateDiscoveryIssuer")}
        labelIcon={t("validateDiscoveryIssuerHelp")}
        stringify
      />
      {isOIDC && (
        <>
          <DefaultSwitchControl
            name="config.validateSignature"
            label={t("validateSignature")}
            labelIcon={t("validateSignatureHelp")}
            isDisabled={readOnly}
            stringify
          />
          {(validateSignature === "true" ||
            jwtAuthorizationGrantEnabled === "true" ||
            supportsClientAssertions == "true") && (
            <JwksSettings readOnly={readOnly} />
          )}
        </>
      )}
    </div>
  );
};

const ClientFields = ({ readOnly }: { readOnly: boolean }) => {
  const { t } = useTranslation();
  const { control } = useFormContext<IdentityProviderRepresentation>();

  const isPkceEnabled = useWatch({
    control,
    name: "config.pkceEnabled",
  });
  const isParRequired = useWatch({
    control,
    name: "config.pushedAuthorizationRequestsRequired",
  });
  const parUrl = useWatch({
    control,
    name: "config.pushedAuthorizationRequestUrl",
  });
  const isRequestObjectEnabled = useWatch({
    control,
    name: "config.requestObjectEnabled",
  });
  const isDPoPEnabled = useWatch({
    control,
    name: "config.dpopEnabled",
  });
  const asymmetricSigningAlgs =
    useServerInfo().cryptoInfo?.clientSignatureAsymmetricAlgorithms ?? [];

  return (
    <>
      <DefaultSwitchControl
        name="config.pkceEnabled"
        label={t("pkceEnabled")}
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
      <DefaultSwitchControl
        name="config.pushedAuthorizationRequestsEnabled"
        label={t("pushedAuthorizationRequestsEnabled")}
        labelIcon={t("pushedAuthorizationRequestsEnabledHelp")}
        // locked when the metadata requires it or has no endpoint for it
        isDisabled={isParRequired === "true" || (readOnly && !parUrl)}
        stringify
      />
      <DefaultSwitchControl
        name="config.requestObjectEnabled"
        label={t("requestObjectEnabled")}
        labelIcon={t("requestObjectEnabledHelp")}
        stringify
      />
      {isRequestObjectEnabled === "true" && (
        <SelectControl
          name="config.requestObjectSigningAlg"
          label={t("requestObjectSigningAlg")}
          labelIcon={t("requestObjectSigningAlgHelp")}
          controller={{
            defaultValue: "PS256",
          }}
          options={asymmetricSigningAlgs.map((option) => ({
            key: option,
            value: option,
          }))}
        />
      )}
      <DefaultSwitchControl
        name="config.dpopEnabled"
        label={t("dpopEnabled")}
        labelIcon={t("dpopEnabledHelp")}
        stringify
      />
      {isDPoPEnabled === "true" && (
        <SelectControl
          name="config.dpopSigningAlg"
          label={t("dpopSigningAlg")}
          labelIcon={t("dpopSigningAlgHelp")}
          controller={{
            defaultValue: "ES256",
          }}
          options={asymmetricSigningAlgs.map((option) => ({
            key: option,
            value: option,
          }))}
        />
      )}
    </>
  );
};

export const DiscoverySettings = ({
  readOnly,
  isOIDC,
}: DiscoverySettingsProps) => {
  const { t } = useTranslation();
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <div className="pf-v5-c-form pf-m-horizontal">
      {readOnly && (
        <ExpandableSection
          className="keycloak__discovery-settings__metadata"
          toggleText={isExpanded ? t("hideMetaData") : t("showMetaData")}
          onToggle={() => setIsExpanded(!isExpanded)}
          isExpanded={isExpanded}
        >
          <MetadataFields readOnly={readOnly} isOIDC={isOIDC} />
        </ExpandableSection>
      )}
      {!readOnly && <MetadataFields readOnly={readOnly} isOIDC={isOIDC} />}
      <ClientFields readOnly={readOnly} />
    </div>
  );
};
