import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, UseFormMethods } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import _ from "lodash";
import type IdentityProviderMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperRepresentation";
import type { IdentityProviderAddMapperParams } from "../routes/AddMapper";
import { useParams } from "react-router-dom";
import type { IdPMapperRepresentationWithAttributes } from "./AddMapper";

type AddMapperFormProps = {
  mapperTypes?: Record<string, IdentityProviderMapperRepresentation>;
  mapperType: string;
  id: string;
  updateMapperType: (mapperType: string) => void;
  form: UseFormMethods<IdPMapperRepresentationWithAttributes>;
  formValues: IdPMapperRepresentationWithAttributes;
  isSocialIdP: boolean;
};

export const AddMapperForm = ({
  mapperTypes,
  mapperType,
  form,
  id,
  updateMapperType,
  formValues,
  isSocialIdP,
}: AddMapperFormProps) => {
  const { t } = useTranslation("identity-providers");

  const { control, register, errors } = form;

  const [mapperTypeOpen, setMapperTypeOpen] = useState(false);

  const syncModes = ["inherit", "import", "legacy", "force"];
  const [syncModeOpen, setSyncModeOpen] = useState(false);
  const { providerId } = useParams<IdentityProviderAddMapperParams>();

  return (
    <>
      <FormGroup
        label={t("common:name")}
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:addIdpMapperName"
            fieldLabelId="name"
          />
        }
        fieldId="kc-name"
        isRequired
        validated={
          errors.name ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={t("common:required")}
      >
        <TextInput
          ref={register({ required: true })}
          type="text"
          datatest-id="name-input"
          id="kc-name"
          name="name"
          isDisabled={!!id}
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
        />
      </FormGroup>
      <FormGroup
        label={t("syncModeOverride")}
        isRequired
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:syncModeOverride"
            fieldLabelId="identity-providers:syncModeOverride"
          />
        }
        fieldId="syncMode"
      >
        <Controller
          name="config.syncMode"
          defaultValue={syncModes[0].toUpperCase()}
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="syncMode"
              datatest-id="syncmode-select"
              required
              direction="down"
              onToggle={() => setSyncModeOpen(!syncModeOpen)}
              onSelect={(_, value) => {
                onChange(value.toString().toUpperCase());
                setSyncModeOpen(false);
              }}
              selections={t(`syncModes.${value.toLowerCase()}`)}
              variant={SelectVariant.single}
              aria-label={t("syncMode")}
              isOpen={syncModeOpen}
            >
              {syncModes.map((option) => (
                <SelectOption
                  selected={option === value}
                  key={option}
                  data-testid={option}
                  value={option.toUpperCase()}
                >
                  {t(`syncModes.${option}`)}
                </SelectOption>
              ))}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("mapperType")}
        labelIcon={
          <HelpItem
            helpText={
              formValues.identityProviderMapper ===
                "saml-user-attribute-idp-mapper" &&
              (providerId === "oidc" ||
                providerId === "keycloak-oidc" ||
                isSocialIdP)
                ? `identity-providers-help:oidcAttributeImporter`
                : `identity-providers-help:${mapperType}`
            }
            fieldLabelId="identity-providers:mapperType"
          />
        }
        fieldId="identityProviderMapper"
      >
        <Controller
          name="identityProviderMapper"
          defaultValue={
            isSocialIdP
              ? `${providerId.toLowerCase()}-user-attribute-mapper`
              : providerId === "saml"
              ? "saml-advanced-role-idp-mapper"
              : "hardcoded-user-session-attribute-idp-mapper"
          }
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="identityProviderMapper"
              data-testid="idp-mapper-select"
              isDisabled={!!id}
              required
              direction="down"
              onToggle={() => setMapperTypeOpen(!mapperTypeOpen)}
              onSelect={(e, value) => {
                const theMapper =
                  mapperTypes &&
                  Object.values(mapperTypes).find(
                    (item) =>
                      item.name?.toLowerCase() ===
                      value.toString().toLowerCase()
                  );

                updateMapperType(_.camelCase(value.toString()));
                onChange(theMapper?.id);
                setMapperTypeOpen(false);
              }}
              selections={
                mapperTypes &&
                Object.values(mapperTypes).find(
                  (item) => item.id?.toLowerCase() === value
                )?.name
              }
              variant={SelectVariant.single}
              aria-label={t("syncMode")}
              isOpen={mapperTypeOpen}
            >
              {mapperTypes &&
                Object.values(mapperTypes).map((option) => (
                  <SelectOption
                    selected={option === value}
                    datatest-id={option.id}
                    key={option.name}
                    value={option.name?.toUpperCase()}
                  >
                    {t(`mapperTypes.${_.camelCase(option.name)}`)}
                  </SelectOption>
                ))}
            </Select>
          )}
        />
      </FormGroup>
    </>
  );
};
