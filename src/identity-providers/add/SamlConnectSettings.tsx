import React, { useEffect, useState } from "react";
import { useFormContext } from "react-hook-form";
import {
  FormGroup,
  Switch,
  TextInput,
  Title,
  ValidatedOptions,
} from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../context/auth/AdminClient";
import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";

import { JsonFileUpload } from "../../components/json-file-upload/JsonFileUpload";
import { useRealm } from "../../context/realm-context/RealmContext";
import { DescriptorSettings } from "./DescriptorSettings";
import { getBaseUrl } from "../../util";

type Result = IdentityProviderRepresentation & {
  error: string;
};

export const SamlConnectSettings = () => {
  const { t } = useTranslation("identity-providers");
  const id = "saml";

  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const { setValue, register, errors } = useFormContext();

  const [descriptor, setDescriptor] = useState(true);

  const [entityUrl, setEntityUrl] = useState("");
  const [descriptorUrl, setDescriptorUrl] = useState("");
  const [discovering, setDiscovering] = useState(false);
  const [discoveryResult, setDiscoveryResult] = useState<Result>();

  const defaultEntityUrl = `${getBaseUrl(adminClient)}realms/${realm}`;

  const setupForm = (result: IdentityProviderRepresentation) => {
    Object.entries(result).map(([key, value]) =>
      setValue(`config.${key}`, value)
    );
  };

  useEffect(() => {
    if (!discovering) {
      return;
    }

    setDiscovering(!!entityUrl);

    if (!entityUrl) {
      return;
    }

    (async () => {
      let result;
      try {
        result = await adminClient.identityProviders.importFromUrl({
          providerId: id,
          fromUrl: entityUrl,
        });
      } catch (error) {
        result = { error };
      }

      setDiscoveryResult(result as Result);
      setupForm(result);
      setDiscovering(false);
    })();
  }, [discovering]);

  const fileUpload = async (obj: object) => {
    const formData = new FormData();
    formData.append("providerId", id);
    formData.append("file", new Blob([JSON.stringify(obj)]));

    try {
      const response = await fetch(
        `${getBaseUrl(
          adminClient
        )}admin/realms/${realm}/identity-provider/import-config`,
        {
          method: "POST",
          body: formData,
          headers: {
            Authorization: `bearer ${await adminClient.getAccessToken()}`,
          },
        }
      );
      const result = await response.json();
      setupForm(result);
    } catch (error: any) {
      setDiscoveryResult({ error });
    }
  };

  return (
    <>
      <Title headingLevel="h4" size="xl" className="kc-form-panel__title">
        {t("samlSettings")}
      </Title>

      <FormGroup
        label={t("serviceProviderEntityId")}
        fieldId="kc-service-provider-entity-id"
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:serviceProviderEntityId"
            forLabel={t("serviceProviderEntityId")}
            forID="kc-service-provider-entity-id"
          />
        }
      >
        <TextInput
          type="text"
          name="config.entityId"
          data-testid="serviceProviderEntityId"
          id="kc-service-provider-entity-id"
          value={entityUrl || defaultEntityUrl}
          onChange={setEntityUrl}
          ref={register()}
        />
      </FormGroup>

      <FormGroup
        label={t("useEntityDescriptor")}
        fieldId="kc-use-entity-descriptor"
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:useEntityDescriptor"
            forLabel={t("useEntityDescriptor")}
            forID="kc-use-entity-descriptor-switch"
          />
        }
      >
        <Switch
          id="kc-use-entity-descriptor-switch"
          label={t("common:on")}
          data-testid="useEntityDescriptor"
          labelOff={t("common:off")}
          isChecked={descriptor}
          onChange={setDescriptor}
        />
      </FormGroup>

      {descriptor && (
        <FormGroup
          label={t("samlEntityDescriptor")}
          fieldId="kc-saml-entity-descriptor"
          labelIcon={
            <HelpItem
              helpText="identity-providers-help:samlEntityDescriptor"
              forLabel={t("samlEntityDescriptor")}
              forID="kc-saml-entity-descriptor"
            />
          }
        >
          <TextInput
            type="text"
            name="samlEntityDescriptor"
            data-testid="samlEntityDescriptor"
            id="kc-saml-entity-descriptor"
            value={descriptorUrl}
            onChange={setDescriptorUrl}
            ref={register()}
            validated={
              errors.samlEntityDescriptor
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
          />
        </FormGroup>
      )}
      {!descriptor && (
        <FormGroup
          label={t("importConfig")}
          fieldId="kc-import-config"
          labelIcon={
            <HelpItem
              helpText="identity-providers-help:importConfig"
              forLabel={t("importConfig")}
              forID="kc-import-config"
            />
          }
          validated={discoveryResult?.error ? "error" : "default"}
          helperTextInvalid={discoveryResult?.error.toString()}
        >
          <JsonFileUpload
            id="kc-import-config"
            helpText="identity-providers-help:jsonFileUpload"
            hideDefaultPreview
            unWrap
            validated={discoveryResult?.error ? "error" : "default"}
            onChange={(value) => fileUpload(value)}
          />
        </FormGroup>
      )}

      {descriptor && discoveryResult && !discoveryResult.error && (
        <DescriptorSettings readOnly={true} />
      )}
      {!descriptor && <DescriptorSettings readOnly={false} />}
    </>
  );
};
