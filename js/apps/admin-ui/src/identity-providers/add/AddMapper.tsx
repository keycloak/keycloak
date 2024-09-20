import type IdentityProviderMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperRepresentation";
import type { IdentityProviderMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperTypeRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { TextControl, useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  PageSection,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { FormAccess } from "../../components/form/FormAccess";
import type { AttributeForm } from "../../components/key-value-form/AttributeForm";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import useLocaleSort, { mapByKey } from "../../utils/useLocaleSort";
import { useParams } from "../../utils/useParams";
import {
  IdentityProviderEditMapperParams,
  toIdentityProviderEditMapper,
} from "../routes/EditMapper";
import { toIdentityProvider } from "../routes/IdentityProvider";
import { AddMapperForm } from "./AddMapperForm";

export type IdPMapperRepresentationWithAttributes =
  IdentityProviderMapperRepresentation & AttributeForm;

export type Role = RoleRepresentation & {
  clientId?: string;
};

export default function AddMapper() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();

  const form = useForm<IdPMapperRepresentationWithAttributes>({
    shouldUnregister: true,
  });
  const { handleSubmit } = form;
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const localeSort = useLocaleSort();

  const { realm } = useRealm();

  const { id, providerId, alias } =
    useParams<IdentityProviderEditMapperParams>();

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
          { ...identityProviderMapper, id },
        );
        addAlert(t("mapperSaveSuccess"), AlertVariant.success);
      } catch (error) {
        addError("mapperSaveError", error);
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
          }),
        );
      } catch (error) {
        addError("mapperCreateError", error);
      }
    }
  };

  const [toggleDeleteMapperDialog, DeleteMapperConfirm] = useConfirmDialog({
    titleKey: "deleteProviderMapper",
    messageKey: t("deleteMapperConfirm", {
      mapper: currentMapper?.name,
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.identityProviders.delMapper({
          alias: alias,
          id: id!,
        });
        addAlert(t("deleteMapperSuccess"), AlertVariant.success);
        navigate(
          toIdentityProvider({ providerId, alias, tab: "mappers", realm }),
        );
      } catch (error) {
        addError("deleteErrorIdentityProvider", error);
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
          mappers.find(({ id }) => id === mapper.identityProviderMapper),
        );
        setupForm(mapper);
      } else {
        setCurrentMapper(mappers[0]);
      }

      setMapperTypes(mappers);
    },
    [id],
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
                  {t("delete")}
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
        className="pf-v5-u-mt-lg"
      >
        <FormProvider {...form}>
          {id && (
            <TextControl
              name="id"
              label={t("id")}
              readOnly
              rules={{
                required: t("required"),
              }}
            />
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

              <DynamicComponents properties={currentMapper.properties!} />
            </>
          )}
        </FormProvider>
        <ActionGroup>
          <Button
            data-testid="new-mapper-save-button"
            variant="primary"
            type="submit"
          >
            {t("save")}
          </Button>
          <Button
            data-testid="new-mapper-cancel-button"
            variant="link"
            component={(props) => (
              <Link
                {...props}
                to={toIdentityProvider({
                  realm,
                  providerId,
                  alias: alias!,
                  tab: "mappers",
                })}
              />
            )}
          >
            {t("cancel")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
}
