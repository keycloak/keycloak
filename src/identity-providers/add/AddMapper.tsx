import React, { useState } from "react";
import { Link, useHistory, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";
import {
  ActionGroup,
  AlertVariant,
  Button,
  FormGroup,
  PageSection,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";

import { useRealm } from "../../context/realm-context/RealmContext";

import { ViewHeader } from "../../components/view-header/ViewHeader";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import type { IdentityProviderAddMapperParams } from "../routes/AddMapper";
import type { RoleRepresentation } from "../../model/role-model";
import { useAlerts } from "../../components/alert/Alerts";
import {
  IdentityProviderEditMapperParams,
  toIdentityProviderEditMapper,
} from "../routes/EditMapper";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import { toIdentityProvider } from "../routes/IdentityProvider";
import type IdentityProviderMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperRepresentation";
import type { IdentityProviderMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperTypeRepresentation";
import { AddMapperForm } from "./AddMapperForm";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import type { AttributeForm } from "../../components/attribute-form/AttributeForm";

export type IdPMapperRepresentationWithAttributes =
  IdentityProviderMapperRepresentation & AttributeForm;

export type Role = RoleRepresentation & {
  clientId?: string;
};

export default function AddMapper() {
  const { t } = useTranslation("identity-providers");

  const form = useForm<IdPMapperRepresentationWithAttributes>({
    shouldUnregister: false,
  });
  const { handleSubmit, register, errors } = form;
  const { addAlert, addError } = useAlerts();
  const history = useHistory();

  const { realm } = useRealm();
  const adminClient = useAdminClient();

  const { providerId, alias } = useParams<IdentityProviderAddMapperParams>();
  const { id } = useParams<IdentityProviderEditMapperParams>();

  const [mapperTypes, setMapperTypes] =
    useState<Record<string, IdentityProviderMapperTypeRepresentation>>();
  const [mapperType, setMapperType] = useState<string>();

  const [currentMapper, setCurrentMapper] =
    useState<IdentityProviderMapperRepresentation>();

  const save = async (idpMapper: IdentityProviderMapperRepresentation) => {
    const mapper = convertFormValuesToObject(
      idpMapper
    ) as IdentityProviderMapperRepresentation;
    const attributes = JSON.stringify(idpMapper.config.attributes ?? []);
    const claims = JSON.stringify(idpMapper.config.claims ?? []);

    const identityProviderMapper = {
      ...mapper,
      config: {
        ...mapper.config,
        attributes,
        claims,
      },
      identityProviderAlias: alias!,
    };

    if (id) {
      try {
        await adminClient.identityProviders.updateMapper(
          {
            id: id!,
            alias: alias!,
          },
          { ...identityProviderMapper, name: currentMapper?.name! }
        );
        addAlert(t("mapperSaveSuccess"), AlertVariant.success);
      } catch (error) {
        addError(t("mapperSaveError"), error);
      }
    } else {
      try {
        const createdMapper = await adminClient.identityProviders.createMapper({
          identityProviderMapper,
          alias: alias!,
        });

        addAlert(t("mapperCreateSuccess"), AlertVariant.success);
        history.push(
          toIdentityProviderEditMapper({
            realm,
            alias,
            providerId: providerId,
            id: createdMapper.id,
          })
        );
      } catch (error) {
        addError(t("mapperCreateError"), error);
      }
    }
  };

  useFetch(
    () =>
      Promise.all([
        id ? adminClient.identityProviders.findOneMapper({ alias, id }) : null,
        adminClient.identityProviders.findMapperTypes({ alias }),
      ]),
    ([mapper, mapperTypes]) => {
      if (mapper) {
        setCurrentMapper(mapper);
        setupForm(mapper);
        setMapperType(mapper.identityProviderMapper!);
      } else {
        setMapperType(Object.keys(mapperTypes)[0]);
      }

      setMapperTypes(mapperTypes);
    },
    []
  );

  const setupForm = (mapper: IdentityProviderMapperRepresentation) => {
    convertToFormValues(mapper, form.setValue);
    form.setValue("config.attributes", JSON.parse(mapper.config.attributes));
    form.setValue("config.claims", JSON.parse(mapper.config.claims));
  };

  if (!mapperTypes) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection variant="light">
      <ViewHeader
        className="kc-add-mapper-title"
        titleKey={
          id
            ? t("editIdPMapper", {
                providerId:
                  providerId[0].toUpperCase() + providerId.substring(1),
              })
            : t("addIdPMapper", {
                providerId:
                  providerId[0].toUpperCase() + providerId.substring(1),
              })
        }
        divider
      />
      <FormAccess
        role="manage-identity-providers"
        isHorizontal
        onSubmit={handleSubmit(save)}
        className="pf-u-mt-lg"
      >
        {id && (
          <FormGroup
            label={t("common:id")}
            fieldId="kc-mapper-id"
            validated={
              errors.name ? ValidatedOptions.error : ValidatedOptions.default
            }
            helperTextInvalid={t("common:required")}
          >
            <TextInput
              ref={register()}
              type="text"
              value={currentMapper?.id}
              datatest-id="name-input"
              id="kc-name"
              name="name"
              isDisabled={!!id}
              validated={
                errors.name ? ValidatedOptions.error : ValidatedOptions.default
              }
            />
          </FormGroup>
        )}
        <AddMapperForm
          form={form}
          id={id}
          mapperTypes={mapperTypes}
          updateMapperType={setMapperType}
          mapperType={mapperType!}
        />
        <FormProvider {...form}>
          {mapperType && mapperTypes[mapperType].properties && (
            <DynamicComponents
              properties={mapperTypes[mapperType].properties!}
            />
          )}
        </FormProvider>
        <ActionGroup>
          <Button
            data-testid="new-mapper-save-button"
            variant="primary"
            type="submit"
          >
            {t("common:save")}
          </Button>
          <Button
            variant="link"
            component={(props) => (
              <Link
                {...props}
                to={toIdentityProvider({
                  realm,
                  providerId,
                  alias: alias!,
                  tab: "settings",
                })}
              />
            )}
          >
            {t("common:cancel")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
}
