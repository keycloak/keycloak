import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, UseFormMethods } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  ValidatedOptions,
} from "@patternfly/react-core";

import type { IdentityProviderMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperTypeRepresentation";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import type IdentityProviderMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperRepresentation";
import type { IdPMapperRepresentationWithAttributes } from "./AddMapper";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

type AddMapperFormProps = {
  mapperTypes: IdentityProviderMapperRepresentation[];
  mapperType: IdentityProviderMapperTypeRepresentation;
  id: string;
  updateMapperType: (
    mapperType: IdentityProviderMapperTypeRepresentation
  ) => void;
  form: UseFormMethods<IdPMapperRepresentationWithAttributes>;
};

export const AddMapperForm = ({
  mapperTypes,
  mapperType,
  form,
  id,
  updateMapperType,
}: AddMapperFormProps) => {
  const { t } = useTranslation("identity-providers");

  const { control, register, errors } = form;

  const [mapperTypeOpen, setMapperTypeOpen] = useState(false);

  const syncModes = ["inherit", "import", "legacy", "force"];
  const [syncModeOpen, setSyncModeOpen] = useState(false);

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
        <KeycloakTextInput
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
            helpText={mapperType.helpText}
            fieldLabelId="identity-providers:mapperType"
          />
        }
        fieldId="identityProviderMapper"
      >
        <Controller
          name="identityProviderMapper"
          defaultValue={mapperTypes[0].id}
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="identityProviderMapper"
              data-testid="idp-mapper-select"
              isDisabled={!!id}
              required
              onToggle={() => setMapperTypeOpen(!mapperTypeOpen)}
              onSelect={(_, value) => {
                const mapperType =
                  value as IdentityProviderMapperTypeRepresentation;
                updateMapperType(mapperType);
                onChange(mapperType.id);
                setMapperTypeOpen(false);
              }}
              selections={mapperType.name}
              variant={SelectVariant.single}
              aria-label={t("mapperType")}
              isOpen={mapperTypeOpen}
            >
              {mapperTypes.map((option) => (
                <SelectOption
                  selected={option === value}
                  datatest-id={option.id}
                  key={option.name}
                  value={option}
                >
                  {option.name}
                </SelectOption>
              ))}
            </Select>
          )}
        />
      </FormGroup>
    </>
  );
};
