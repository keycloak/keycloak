import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import type { KeyMetadataRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/keyMetadataRepresentation";
import { ActionGroup, Button } from "@patternfly/react-core";
import { useEffect, useMemo, useState } from "react";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import {
  FormSubmitButton,
  SelectControl,
  TextAreaControl,
  TextControl,
  useFetch,
} from "@keycloak/keycloak-ui-shared";

import { useAdminClient } from "../../admin-client";
import { getProtocolName } from "../../clients/utils";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import {
  ClientScopeDefaultOptionalType,
  allClientScopeTypes,
} from "../../components/client-scope/ClientScopeTypes";
import { FormAccess } from "../../components/form/FormAccess";
import { useRealm } from "../../context/realm-context/RealmContext";
import {
  useLoginProviders,
  useServerInfo,
} from "../../context/server-info/ServerInfoProvider";
import { convertAttributeNameToForm, convertToFormValues } from "../../util";
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";
import { toClientScopes } from "../routes/ClientScopes";
import { removeEmptyOid4vcAttributes } from "./oid4vciAttributes";

const OID4VC_PROTOCOL = "oid4vc";
const VC_FORMAT_JWT_VC = "jwt_vc";
const VC_FORMAT_SD_JWT = "dc+sd-jwt";

// Validation function for comma-separated lists
const validateCommaSeparatedList = (value: string | undefined) => {
  if (!value || value.trim() === "") {
    return true;
  }
  if (value.includes(", ") || value.includes(" ,")) {
    return "Comma-separated list must not contain spaces around commas";
  }
  const entries = value.split(",");
  const hasEmptyEntries = entries.some((entry) => entry.trim() === "");
  if (hasEmptyEntries) {
    return "Comma-separated list contains empty entries";
  }
  return true;
};

type ScopeFormProps = {
  clientScope?: ClientScopeRepresentation;
  save: (clientScope: ClientScopeDefaultOptionalType) => void;
};

export const ScopeForm = ({ clientScope, save }: ScopeFormProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const form = useForm<ClientScopeDefaultOptionalType>({ mode: "onChange" });
  const { control, handleSubmit, setValue, formState } = form;
  const { isDirty, isValid } = formState;
  const { realm } = useRealm();

  const providers = useLoginProviders();
  const serverInfo = useServerInfo();
  const isFeatureEnabled = useIsFeatureEnabled();
  const isDynamicScopesEnabled = isFeatureEnabled(Feature.DynamicScopes);

  // Get available signature algorithms from server info
  const signatureAlgorithms = useMemo(
    () =>
      serverInfo?.providers?.signature?.providers
        ? Object.keys(serverInfo.providers.signature.providers)
        : [],
    [serverInfo],
  );

  // Get available hash algorithms from server info
  const hashAlgorithms = serverInfo?.providers?.hash?.providers
    ? Object.keys(serverInfo.providers.hash.providers)
    : [];

  // Get available asymmetric signature algorithms from server info
  const asymmetricSigAlgOptions = useMemo(
    () => serverInfo?.cryptoInfo?.clientSignatureAsymmetricAlgorithms ?? [],
    [serverInfo],
  );

  // Fetch realm keys for signing_key_id dropdown
  const [realmKeys, setRealmKeys] = useState<KeyMetadataRepresentation[]>([]);

  useFetch(
    async () => {
      const keysMetadata = await adminClient.realms.getKeys({ realm });
      return keysMetadata.keys || [];
    },
    setRealmKeys,
    [],
  );

  // Prepare key options for SelectControl
  // Filter only active keys suitable for signing credentials
  const keyOptions = useMemo(() => {
    const options = [{ key: "", value: t("useDefaultKey") }];
    if (realmKeys && realmKeys.length > 0) {
      const keyOptions = realmKeys
        .filter(
          (key) =>
            key.kid &&
            key.status === "ACTIVE" &&
            key.algorithm &&
            signatureAlgorithms.includes(key.algorithm),
        )
        .map((key) => ({
          key: key.kid!,
          value: `${key.kid} (${key.algorithm})`,
        }));
      options.push(...keyOptions);
    }
    return options;
  }, [realmKeys, signatureAlgorithms, t]);

  const displayOnConsentScreen: string = useWatch({
    control,
    name: convertAttributeNameToForm("attributes.display.on.consent.screen"),
    defaultValue:
      clientScope?.attributes?.["display.on.consent.screen"] ?? "true",
  });

  const dynamicScope = useWatch({
    control,
    name: convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
      "attributes.is.dynamic.scope",
    ),
    defaultValue: "false",
  });

  const selectedProtocol = useWatch({
    control,
    name: "protocol",
  });

  const selectedFormat = useWatch({
    control,
    name: convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
      "attributes.vc.format",
    ),
    defaultValue: clientScope?.attributes?.["vc.format"] ?? VC_FORMAT_SD_JWT,
  });

  const isOid4vcProtocol = selectedProtocol === OID4VC_PROTOCOL;
  const isOid4vcEnabled = isFeatureEnabled(Feature.OpenId4VCI);
  const isNotSaml = selectedProtocol != "saml";

  const setDynamicRegex = (value: string, append: boolean) =>
    setValue(
      convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
        "attributes.dynamic.scope.regexp",
      ),
      append ? `${value}:*` : value,
      { shouldDirty: true }, // Mark the field as dirty when we modify the field
    );

  useEffect(() => {
    convertToFormValues(clientScope ?? {}, setValue);
  }, [clientScope, setValue]);

  /* Form-level validation handles correctness; here we only prune known optional
     OID4VC fields when empty. If new attributes are added, extend
     OID4VC_ATTRIBUTE_KEYS (and related validation) so they participate in cleanup. */
  const onSubmit = (values: ClientScopeDefaultOptionalType) => {
    const isOid4vc = values.protocol === OID4VC_PROTOCOL;
    const cleaned = isOid4vc ? removeEmptyOid4vcAttributes(values) : values;
    save(cleaned);
  };

  return (
    <FormAccess
      role="manage-clients"
      onSubmit={handleSubmit(onSubmit)}
      isHorizontal
    >
      <FormProvider {...form}>
        <TextControl
          name="name"
          label={t("name")}
          labelIcon={t("scopeNameHelp")}
          rules={{
            required: t("required"),
            onChange: (e) => {
              if (isDynamicScopesEnabled)
                setDynamicRegex(e.target.validated, true);
            },
          }}
        />
        {isDynamicScopesEnabled && (
          <>
            <DefaultSwitchControl
              name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                "attributes.is.dynamic.scope",
              )}
              label={t("dynamicScope")}
              labelIcon={t("dynamicScopeHelp")}
              onChange={(event, value) => {
                setDynamicRegex(
                  value ? form.getValues("name") || "" : "",
                  value,
                );
              }}
              stringify
            />
            {dynamicScope === "true" && (
              <TextControl
                name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                  "attributes.dynamic.scope.regexp",
                )}
                label={t("dynamicScopeFormat")}
                labelIcon={t("dynamicScopeFormatHelp")}
                isDisabled
              />
            )}
          </>
        )}
        <TextAreaControl
          name="description"
          label={t("description")}
          labelIcon={t("scopeDescriptionHelp")}
          rules={{
            maxLength: {
              value: 255,
              message: t("maxLength"),
            },
          }}
        />
        <SelectControl
          id="kc-type"
          name="type"
          label={t("type")}
          labelIcon={t("scopeTypeHelp")}
          controller={{ defaultValue: allClientScopeTypes[0] }}
          options={allClientScopeTypes.map((key) => ({
            key,
            value: t(`clientScopeType.${key}`),
          }))}
        />
        {!clientScope && (
          <SelectControl
            id="kc-protocol"
            name="protocol"
            label={t("protocol")}
            labelIcon={t("protocolHelp")}
            controller={{ defaultValue: providers[0] }}
            options={providers
              .filter((option) =>
                option === OID4VC_PROTOCOL ? isOid4vcEnabled : true,
              )
              .map((option) => ({
                key: option,
                value: getProtocolName(t, option),
              }))}
          />
        )}
        <DefaultSwitchControl
          name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
            "attributes.display.on.consent.screen",
          )}
          defaultValue={displayOnConsentScreen}
          label={t("displayOnConsentScreen")}
          labelIcon={t("displayOnConsentScreenHelp")}
          stringify
        />
        {displayOnConsentScreen === "true" && (
          <TextAreaControl
            name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
              "attributes.consent.screen.text",
            )}
            label={t("consentScreenText")}
            labelIcon={t("consentScreenTextHelp")}
          />
        )}
        <DefaultSwitchControl
          name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
            "attributes.include.in.token.scope",
          )}
          label={t("includeInTokenScope")}
          labelIcon={t("includeInTokenScopeHelp")}
          stringify
        />
        {isNotSaml && (
          <DefaultSwitchControl
            name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
              "attributes.include.in.openid.provider.metadata",
            )}
            defaultValue="true"
            label={t("includeInOpenIdProviderMetadata")}
            labelIcon={t("includeInOpenIdProviderMetadataHelp")}
            stringify
          />
        )}
        <TextControl
          name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
            "attributes.gui.order",
          )}
          label={t("guiOrder")}
          labelIcon={t("guiOrderHelp")}
          type="number"
          min={0}
        />

        {isOid4vcProtocol && isOid4vcEnabled && (
          <>
            <TextControl
              name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                "attributes.vc.credential_configuration_id",
              )}
              label={t("credentialConfigurationId")}
              labelIcon={t("credentialConfigurationIdHelp")}
            />
            <TextControl
              name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                "attributes.vc.credential_identifier",
              )}
              label={t("credentialIdentifier")}
              labelIcon={t("credentialIdentifierHelp")}
            />
            <TextControl
              name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                "attributes.vc.issuer_did",
              )}
              label={t("issuerDid")}
              labelIcon={t("issuerDidHelp")}
            />
            <TextControl
              name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                "attributes.vc.expiry_in_seconds",
              )}
              label={t("credentialLifetime")}
              labelIcon={t("credentialLifetimeHelp")}
              type="number"
              min={1}
              defaultValue={
                clientScope?.attributes?.["vc.expiry_in_seconds"] ?? "31536000"
              }
            />
            <SelectControl
              id="kc-vc-format"
              name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                "attributes.vc.format",
              )}
              label={t("supportedFormats")}
              labelIcon={t("supportedFormatsHelp")}
              controller={{ defaultValue: VC_FORMAT_SD_JWT }}
              options={[
                {
                  key: VC_FORMAT_SD_JWT,
                  value: `SD-JWT VC (${VC_FORMAT_SD_JWT})`,
                },
                {
                  key: VC_FORMAT_JWT_VC,
                  value: `JWT VC (${VC_FORMAT_JWT_VC})`,
                },
              ]}
            />
            <TextControl
              name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                "attributes.vc.credential_build_config.token_jws_type",
              )}
              label={t("tokenJwsType")}
              labelIcon={t("tokenJwsTypeHelp")}
              defaultValue={
                clientScope?.attributes?.[
                  "vc.credential_build_config.token_jws_type"
                ] ?? "JWS"
              }
            />
            {realmKeys && realmKeys.length > 0 && (
              <SelectControl
                id="kc-signing-key-id"
                name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                  "attributes.vc.signing_key_id",
                )}
                label={t("signingKeyId")}
                labelIcon={t("signingKeyIdHelp")}
                controller={{
                  defaultValue:
                    clientScope?.attributes?.["vc.signing_key_id"] ?? "",
                }}
                options={keyOptions}
              />
            )}
            {asymmetricSigAlgOptions.length > 0 && (
              <SelectControl
                id="kc-credential-signing-alg"
                name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                  "attributes.vc.credential_signing_alg",
                )}
                label={t("credentialSigningAlgorithm")}
                labelIcon={t("credentialSigningAlgorithmHelp")}
                controller={{
                  defaultValue:
                    clientScope?.attributes?.["vc.credential_signing_alg"] ??
                    "",
                }}
                options={asymmetricSigAlgOptions.map((alg) => ({
                  key: alg,
                  value: alg,
                }))}
              />
            )}
            {hashAlgorithms.length > 0 && (
              <SelectControl
                id="kc-hash-algorithm"
                name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                  "attributes.vc.credential_build_config.hash_algorithm",
                )}
                label={t("hashAlgorithm")}
                labelIcon={t("hashAlgorithmHelp")}
                controller={{
                  defaultValue:
                    clientScope?.attributes?.[
                      "vc.credential_build_config.hash_algorithm"
                    ] ?? "SHA-256",
                }}
                options={hashAlgorithms.map((alg) => ({
                  key: alg,
                  value: alg,
                }))}
              />
            )}
            <TextAreaControl
              name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                "attributes.vc.display",
              )}
              label={t("credentialDisplay")}
              labelIcon={t("credentialDisplayHelp")}
              rules={{
                validate: (value: string | undefined) => {
                  if (!value || value.trim() === "") {
                    return true;
                  }
                  try {
                    JSON.parse(value);
                    return true;
                  } catch {
                    return "Invalid JSON format";
                  }
                },
              }}
            />
            {(selectedFormat === VC_FORMAT_JWT_VC ||
              selectedFormat === VC_FORMAT_SD_JWT) && (
              <TextControl
                name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                  "attributes.vc.supported_credential_types",
                )}
                label={t("supportedCredentialTypes")}
                labelIcon={t("supportedCredentialTypesHelp")}
                rules={{
                  validate: validateCommaSeparatedList,
                }}
              />
            )}
            {selectedFormat === VC_FORMAT_SD_JWT && (
              <>
                <TextControl
                  name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                    "attributes.vc.verifiable_credential_type",
                  )}
                  label={t("verifiableCredentialType")}
                  labelIcon={t("verifiableCredentialTypeHelp")}
                />
                <TextControl
                  name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                    "attributes.vc.credential_build_config.sd_jwt.visible_claims",
                  )}
                  label={t("visibleClaims")}
                  labelIcon={t("visibleClaimsHelp")}
                  defaultValue={
                    clientScope?.attributes?.[
                      "vc.credential_build_config.sd_jwt.visible_claims"
                    ] ?? "id,iat,nbf,exp,jti"
                  }
                  rules={{
                    validate: validateCommaSeparatedList,
                  }}
                />
              </>
            )}
          </>
        )}

        <ActionGroup>
          <FormSubmitButton
            data-testid="save"
            formState={formState}
            disabled={!isDirty || !isValid}
          >
            {t("save")}
          </FormSubmitButton>
          <Button
            variant="link"
            component={(props) => (
              <Link {...props} to={toClientScopes({ realm })}></Link>
            )}
          >
            {t("cancel")}
          </Button>
        </ActionGroup>
      </FormProvider>
    </FormAccess>
  );
};
