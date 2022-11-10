import { useState } from "react";
import { useParams } from "react-router-dom";
import { Link, useNavigate } from "react-router-dom-v5-compat";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  FormGroup,
  PageSection,
  ValidatedOptions,
} from "@patternfly/react-core";

import { useRealm } from "../../context/realm-context/RealmContext";

import { ViewHeader } from "../../components/view-header/ViewHeader";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import type { IdentityProviderAddMapperParams } from "../routes/AddMapper";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
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
import type { AttributeForm } from "../../components/key-value-form/AttributeForm";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import useLocaleSort, { mapByKey } from "../../utils/useLocaleSort";

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
  const navigate = useNavigate();
  const localeSort = useLocaleSort();

  const { realm } = useRealm();
  const { adminClient } = useAdminClient();

  const { providerId, alias } = useParams<IdentityProviderAddMapperParams>();
  const { id } = useParams<IdentityProviderEditMapperParams>();

  const [mapperTypes, setMapperTypes] =
    useState<IdentityProviderMapperTypeRepresentation[]>();

  const [currentMapper, setCurrentMapper] =
    useState<IdentityProviderMapperTypeRepresentation>();

  const save = async (idpMapper: IdentityProviderMapperRepresentation) => {
    const mapper = convertFormValuesToObject(idpMapper);

    const identityProviderMapper = {
      ...mapper,
      config: {
        ...mapper.config,
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
        navigate(
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

  const [toggleDeleteMapperDialog, DeleteMapperConfirm] = useConfirmDialog({
    titleKey: "identity-providers:deleteProviderMapper",
    messageKey: t("identity-providers:deleteMapperConfirm", {
      mapper: currentMapper?.name,
    }),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.identityProviders.delMapper({
          alias: alias,
          id: id!,
        });
        addAlert(t("deleteMapperSuccess"), AlertVariant.success);
        navigate(
          toIdentityProvider({ providerId, alias, tab: "mappers", realm })
        );
      } catch (error) {
        addError("identity-providers:deleteErrorError", error);
      }
    },
  });

  useFetch(
    () =>
      Promise.all([
        id ? adminClient.identityProviders.findOneMapper({ alias, id }) : null,
        adminClient.identityProviders.findMapperTypes({ alias }),
      ]),
    ([mapper, mapperTypes]) => {
      const mappers = localeSort(Object.values(mapperTypes), mapByKey("name"));
      if (mapper) {
        setCurrentMapper(
          mappers.find(({ id }) => id === mapper.identityProviderMapper)
        );
        setupForm(mapper);
      } else {
        setCurrentMapper(mappers[0]);
      }

      setMapperTypes(mappers);
    },
    []
  );

  const setupForm = (mapper: IdentityProviderMapperRepresentation) => {
    convertToFormValues(mapper, form.setValue);
  };

  if (!mapperTypes || !currentMapper) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection variant="light">
      <DeleteMapperConfirm />
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
        dropdownItems={
          id
            ? [
                <DropdownItem key="delete" onClick={toggleDeleteMapperDialog}>
                  {t("common:delete")}
                </DropdownItem>,
              ]
            : undefined
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
            <KeycloakTextInput
              ref={register()}
              type="text"
              value={currentMapper.id}
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
        {currentMapper.properties && (
          <>
            <AddMapperForm
              form={form}
              id={id}
              mapperTypes={mapperTypes}
              updateMapperType={setCurrentMapper}
              mapperType={currentMapper}
            />
            <FormProvider {...form}>
              <DynamicComponents properties={currentMapper.properties!} />
            </FormProvider>
          </>
        )}

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
