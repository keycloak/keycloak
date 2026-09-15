import type IdentityProviderRepresentation from "libs/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import type IdentityProviderMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperRepresentation";
import type { IdentityProviderMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperTypeRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  FormGroup,
  TextInput,
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
import { GroupResourceContext } from "../../context/group-resource/GroupResourceContext";

export type IdPMapperRepresentationWithAttributes =
  IdentityProviderMapperRepresentation & AttributeForm;

export type Role = RoleRepresentation & {
  clientId?: string;
};

const MAPPER_SYNC_RETRIES = 10;
const MAPPER_SYNC_DELAY_MS = 300;

const sleep = (durationMs: number) =>
  new Promise((resolve) => setTimeout(resolve, durationMs));

export default function AddMapper() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();

  const form = useForm<IdPMapperRepresentationWithAttributes>();
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

  const [idp, setIdp] = useState<IdentityProviderRepresentation>();
  const [isSaving, setIsSaving] = useState(false);

  const waitForMapperToBeAvailable = async (mapperId?: string) => {
    if (!mapperId) {
      return false;
    }

    for (let attempt = 0; attempt < MAPPER_SYNC_RETRIES; attempt++) {
      try {
        const mapper = await adminClient.identityProviders.findOneMapper({
          alias,
          id: mapperId,
        });
        if (mapper?.id === mapperId) {
          return true;
        }
      } catch {
        // The mapper can still be in-flight right after creation.
      }

      await sleep(MAPPER_SYNC_DELAY_MS);
    }

    return false;
  };

  const save = async (idpMapper: IdentityProviderMapperRepresentation) => {
    if (isSaving) {
      return;
    }

    setIsSaving(true);
    const mapper = convertFormValuesToObject(idpMapper);

    const identityProviderMapper = {
      ...mapper,
      config: {
        ...mapper.config,
      },
      identityProviderAlias: alias!,
    };

    try {
      if (id) {
        await adminClient.identityProviders.updateMapper(
          {
            id: id!,
            alias: alias!,
          },
          { ...identityProviderMapper, id },
        );
        addAlert(t("mapperSaveSuccess"), AlertVariant.success);
      } else {
        const createdMapper = await adminClient.identityProviders.createMapper({
          identityProviderMapper,
          alias: alias!,
        });

        const mapperAvailable = await waitForMapperToBeAvailable(
          createdMapper.id,
        );
        addAlert(t("mapperCreateSuccess"), AlertVariant.success);

        if (createdMapper.id && mapperAvailable) {
          void navigate(
            toIdentityProviderEditMapper({
              realm,
              alias,
              providerId: providerId,
              id: createdMapper.id,
            }),
          );
        } else {
          void navigate(
            toIdentityProvider({
              providerId,
              alias,
              tab: "mappers",
              realm,
            }),
          );
        }
      }
    } catch (error) {
      addError(id ? "mapperSaveError" : "mapperCreateError", error);
    } finally {
      setIsSaving(false);
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
        void navigate(
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
        adminClient.identityProviders.findOne({ alias }),
      ]),
    ([mapper, mapperTypes, idp]) => {
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
      setIdp(idp);
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
            <>
              <FormGroup label={t("id")} fieldId="id">
                <TextInput name="id" readOnly value={id} />
              </FormGroup>
              <FormGroup label={t("name")} fieldId="name">
                <TextInput
                  name="name"
                  readOnly
                  value={form.getValues("name")}
                />
              </FormGroup>
            </>
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
              <GroupResourceContext
                value={
                  idp?.organizationId
                    ? adminClient.organizations.groups(idp.organizationId)
                    : adminClient.groups
                }
              >
                <DynamicComponents properties={currentMapper.properties!} />
              </GroupResourceContext>
            </>
          )}
        </FormProvider>
        <ActionGroup>
          <Button
            data-testid="new-mapper-save-button"
            variant="primary"
            type="submit"
            isDisabled={isSaving}
            isLoading={isSaving}
          >
            {t("save")}
          </Button>
          <Button
            data-testid="new-mapper-cancel-button"
            variant="link"
            isDisabled={isSaving}
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
