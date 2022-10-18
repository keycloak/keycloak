import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Form,
  FormGroup,
  PageSection,
  ValidatedOptions,
  TextInput,
} from "@patternfly/react-core";
import {
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core/deprecated";
import { useState } from "react";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { HelpItem } from "@keycloak/keycloak-ui-shared";

import { convertFormValuesToObject, convertToFormValues } from "../../util";
import { useRealm } from "../../context/realm-context/RealmContext";
import { adminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";
import { useParams } from "../../utils/useParams";
import { useAlerts } from "../../components/alert/Alerts";

import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { ViewHeader } from "../../components/view-header/ViewHeader";

import { CustomUserFederationMapperRouteParams } from "../routes/CustomInstanceMapper";
import { toCustomUserFederation } from "../routes/CustomUserFederation";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";

export default function CustomInstanceMapperSettings() {
  const navigate = useNavigate();
  const { realm } = useRealm();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const form = useForm<ComponentRepresentation>();

  const [mapping, setMapping] = useState<ComponentRepresentation>();
  const [components, setComponents] = useState<ComponentTypeRepresentation[]>();
  const [isMapperDropdownOpen, setIsMapperDropdownOpen] = useState(false);

  const {
    providerId: parentProviderId,
    parentId,
    id,
  } = useParams<CustomUserFederationMapperRouteParams>();

  const provider = (
    useServerInfo().componentTypes?.[
      "org.keycloak.storage.UserStorageProvider"
    ] || []
  ).find((p: ComponentTypeRepresentation) => p.id === parentProviderId);

  // watch the value of the mapper type select
  const mapperType = useWatch({
    control: form.control,
    name: "providerId",
  });

  // load the apprpriate mapper type
  const mapper: ComponentTypeRepresentation | undefined = components?.find(
    (c) => c.id === mapperType,
  );

  useFetch(
    async () => {
      if (!provider) return {};

      const components = await adminClient.components.listSubComponents({
        id: parentId,
        type: provider.metadata.mapperType,
      });
      if (id) {
        const fetchedMapper = await adminClient.components.findOne({
          id,
        });
        return { components, fetchedMapper };
      }
      return { components };
    },
    ({ components, fetchedMapper }) => {
      setComponents(components);
      setMapping(fetchedMapper);

      if (fetchedMapper) setupForm(fetchedMapper);
    },
    [provider],
  );

  const setupForm = (mapper: ComponentRepresentation) => {
    convertToFormValues(mapper, form.setValue);
  };

  const save = async (instance: ComponentRepresentation) => {
    const component: ComponentRepresentation =
      convertFormValuesToObject(instance);
    const map = {
      ...component,
      config: Object.fromEntries(
        Object.entries(component.config || {}).map(([key, value]) => [
          key,
          Array.isArray(value) ? value : [value],
        ]),
      ),
      parentId,
      providerId: instance.providerId,
      providerType: provider?.metadata.mapperType,
    };

    try {
      if (!id) {
        await adminClient.components.create(map);
        navigate(
          toCustomUserFederation({
            realm,
            providerId: parentProviderId,
            id: parentId,
            tab: "mappers",
          }),
        );
      } else {
        await adminClient.components.update({ id: id }, map);
      }
      setupForm(map as ComponentRepresentation);
      addAlert(
        t(!id ? "mappingCreatedSuccess" : "mappingUpdatedSuccess"),
        AlertVariant.success,
      );
    } catch (error) {
      addError(!id ? "mappingCreatedError" : "mappingUpdatedError", error);
    }
  };

  if (!components) {
    return <KeycloakSpinner />;
  } else {
    return (
      <>
        <ViewHeader titleKey={mapping ? mapping.name! : t("createNewMapper")} />
        <PageSection variant="light" isFilled>
          <FormAccess role="manage-realm" isHorizontal>
            {id && (
              <FormGroup label={t("id")}>
                <TextInput isDisabled {...form.register("id")} />
              </FormGroup>
            )}
            <FormGroup
              label={t("name")}
              labelIcon={
                <HelpItem
                  helpText={t("userFederation.custom.mapper.name.help")}
                  fieldLabelId="name"
                />
              }
              isRequired
            >
              <TextInput
                isDisabled={!!id}
                isRequired
                validated={
                  form.formState.errors.name
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                {...form.register("name", { required: true })}
              />
            </FormGroup>
            {id ? (
              <FormGroup
                label={t("mapperType")}
                labelIcon={
                  <HelpItem
                    helpText={
                      mapper?.helpText
                        ? mapper.helpText
                        : t("userFederation.custom.mapper.type.help")
                    }
                    fieldLabelId="mapperType"
                  />
                }
                isRequired
              >
                <TextInput
                  isDisabled={!!id}
                  isRequired
                  {...form.register("providerId")}
                />
              </FormGroup>
            ) : (
              <FormGroup
                label={t("mapperType")}
                labelIcon={
                  <HelpItem
                    helpText={
                      mapper?.helpText
                        ? mapper.helpText
                        : t("userFederation.custom.mapper.type.help")
                    }
                    fieldLabelId="mapperType"
                  />
                }
                isRequired
              >
                <Controller
                  name="providerId"
                  defaultValue=""
                  control={form.control}
                  render={({ field }) => (
                    <Select
                      required
                      onToggle={() =>
                        setIsMapperDropdownOpen(!isMapperDropdownOpen)
                      }
                      isOpen={isMapperDropdownOpen}
                      onSelect={(_, value) => {
                        field.onChange(value as string);
                        setIsMapperDropdownOpen(false);
                      }}
                      selections={field.value}
                      variant={SelectVariant.typeahead}
                    >
                      {components.map((c) => (
                        <SelectOption key={c.id} value={c.id} />
                      ))}
                    </Select>
                  )}
                ></Controller>
              </FormGroup>
            )}
            <FormProvider {...form}>
              {!!mapperType && (
                <DynamicComponents properties={mapper?.properties!} />
              )}
            </FormProvider>
          </FormAccess>

          <Form onSubmit={form.handleSubmit(() => save(form.getValues()))}>
            <ActionGroup>
              <Button
                isDisabled={!form.formState.isDirty}
                variant="primary"
                type="submit"
              >
                {t("save")}
              </Button>
              <Button
                variant="link"
                onClick={() =>
                  navigate(
                    toCustomUserFederation({
                      realm,
                      providerId: parentProviderId,
                      id: parentId,
                      tab: "mappers",
                    }),
                  )
                }
              >
                {t("cancel")}
              </Button>
            </ActionGroup>
          </Form>
        </PageSection>
      </>
    );
  }
}
