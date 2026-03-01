import type IdentityProviderMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperRepresentation";
import type { IdentityProviderMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperTypeRepresentation";
import {
  HelpItem,
  KeycloakSelect,
  SelectControl,
  SelectVariant,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { FormGroup, SelectOption } from "@patternfly/react-core";
import { useState } from "react";
import { Controller, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";
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

  const { control } = form;

  const [mapperTypeOpen, setMapperTypeOpen] = useState(false);

  const syncModes = ["inherit", "import", "legacy", "force"];

  return (
    <>
      <TextControl
        name="name"
        label={t("name")}
        labelIcon={t("addIdpMapperNameHelp")}
        readOnly={!!id}
        rules={{
          required: t("required"),
        }}
      />
      <SelectControl
        name="config.syncMode"
        label={t("syncModeOverride")}
        labelIcon={t("syncModeOverrideHelp")}
        options={syncModes.map((option) => ({
          key: option.toUpperCase(),
          value: t(`syncModes.${option}`),
        }))}
        controller={{ defaultValue: syncModes[0].toUpperCase() }}
      />
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
            <KeycloakSelect
              toggleId="identityProviderMapper"
              data-testid="idp-mapper-select"
              isDisabled={!!id}
              onToggle={() => setMapperTypeOpen(!mapperTypeOpen)}
              onSelect={(value) => {
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
                  data-testid={option.id}
                  key={option.name}
                  value={option}
                >
                  {option.name}
                </SelectOption>
              ))}
            </KeycloakSelect>
          )}
        />
      </FormGroup>
    </>
  );
};
