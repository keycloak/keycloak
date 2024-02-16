import type IdentityProviderMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperRepresentation";
import type { IdentityProviderMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperTypeRepresentation";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import type { IdPMapperRepresentationWithAttributes } from "./AddMapper";

type AddMapperFormProps = {
  mapperTypes: IdentityProviderMapperRepresentation[];
  mapperType: IdentityProviderMapperTypeRepresentation;
  id: string;
  updateMapperType: (
    mapperType: IdentityProviderMapperTypeRepresentation,
  ) => void;
  form: UseFormReturn<IdPMapperRepresentationWithAttributes>;
};

export const AddMapperForm = ({
  mapperTypes,
  mapperType,
  form,
  id,
  updateMapperType,
}: AddMapperFormProps) => {
  const { t } = useTranslation();

  const {
    control,
    register,
    formState: { errors },
  } = form;

  const [mapperTypeOpen, setMapperTypeOpen] = useState(false);

  const syncModes = ["inherit", "import", "legacy", "force"];
  const [syncModeOpen, setSyncModeOpen] = useState(false);

  return (
    <>
      <FormGroup
        label={t("name")}
        labelIcon={
          <HelpItem helpText={t("addIdpMapperNameHelp")} fieldLabelId="name" />
        }
        fieldId="kc-name"
        isRequired
        validated={
          errors.name ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={t("required")}
      >
        <KeycloakTextInput
          id="kc-name"
          isDisabled={!!id}
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
          {...register("name", { required: true })}
        />
      </FormGroup>
      <FormGroup
        label={t("syncModeOverride")}
        isRequired
        labelIcon={
          <HelpItem
            helpText={t("syncModeOverrideHelp")}
            fieldLabelId="syncModeOverride"
          />
        }
        fieldId="syncMode"
      >
        <Controller
          name="config.syncMode"
          defaultValue={syncModes[0].toUpperCase()}
          control={control}
          render={({ field }) => (
            <Select
              toggleId="syncMode"
              datatest-id="syncmode-select"
              required
              direction="down"
              onToggle={() => setSyncModeOpen(!syncModeOpen)}
              onSelect={(_, value) => {
                field.onChange(value.toString().toUpperCase());
                setSyncModeOpen(false);
              }}
              selections={t(`syncModes.${field.value.toLowerCase()}`)}
              variant={SelectVariant.single}
              aria-label={t("syncMode")}
              isOpen={syncModeOpen}
            >
              {syncModes.map((option) => (
                <SelectOption
                  selected={option === field.value}
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
          <HelpItem helpText={mapperType.helpText} fieldLabelId="mapperType" />
        }
        fieldId="identityProviderMapper"
      >
        <Controller
          name="identityProviderMapper"
          defaultValue={mapperTypes[0].id}
          control={control}
          render={({ field }) => (
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
                field.onChange(mapperType.id);
                setMapperTypeOpen(false);
              }}
              selections={mapperType.name}
              variant={SelectVariant.single}
              aria-label={t("mapperType")}
              isOpen={mapperTypeOpen}
            >
              {mapperTypes.map((option) => (
                <SelectOption
                  selected={option === field.value}
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
