import React, { useState } from "react";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Form,
  FormGroup,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { convertFormValuesToObject, convertToFormValues } from "../../../util";
import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { useAdminClient, useFetch } from "../../../context/auth/AdminClient";
import { ViewHeader } from "../../../components/view-header/ViewHeader";
import { useHistory, useParams } from "react-router-dom";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import { useAlerts } from "../../../components/alert/Alerts";
import { useTranslation } from "react-i18next";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { FormAccess } from "../../../components/form-access/FormAccess";

import type ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import { DynamicComponents } from "../../../components/dynamic/DynamicComponents";
import { useRealm } from "../../../context/realm-context/RealmContext";
import { KeycloakSpinner } from "../../../components/keycloak-spinner/KeycloakSpinner";
import { toUserFederationLdap } from "../../routes/UserFederationLdap";

export default function LdapMapperDetails() {
  const form = useForm<ComponentRepresentation>();
  const [mapping, setMapping] = useState<ComponentRepresentation>();
  const [components, setComponents] = useState<ComponentTypeRepresentation[]>();

  const adminClient = useAdminClient();
  const { id, mapperId } = useParams<{ id: string; mapperId: string }>();
  const history = useHistory();
  const { realm } = useRealm();
  const { t } = useTranslation("user-federation");
  const { addAlert, addError } = useAlerts();

  const [isMapperDropdownOpen, setIsMapperDropdownOpen] = useState(false);

  useFetch(
    async () => {
      const components = await adminClient.components.listSubComponents({
        id,
        type: "org.keycloak.storage.ldap.mappers.LDAPStorageMapper",
      });
      if (mapperId && mapperId !== "new") {
        const fetchedMapper = await adminClient.components.findOne({
          id: mapperId,
        });
        return { components, fetchedMapper };
      }
      return { components };
    },
    ({ components, fetchedMapper }) => {
      setMapping(fetchedMapper);
      setComponents(components);
      if (mapperId !== "new" && !fetchedMapper)
        throw new Error(t("common:notFound"));

      if (fetchedMapper) setupForm(fetchedMapper);
    },
    []
  );

  const setupForm = (mapper: ComponentRepresentation) => {
    convertToFormValues(mapper, form.setValue);
  };

  const save = async (mapper: ComponentRepresentation) => {
    const component: ComponentRepresentation =
      convertFormValuesToObject(mapper);
    const map = {
      ...component,
      config: Object.entries(component.config || {}).reduce(
        (result, [key, value]) => {
          result[key] = Array.isArray(value) ? value : [value];
          return result;
        },
        {} as Record<string, string | string[]>
      ),
    };

    try {
      if (mapperId === "new") {
        await adminClient.components.create(map);
        history.push(
          toUserFederationLdap({ realm, id: mapper.parentId!, tab: "mappers" })
        );
      } else {
        await adminClient.components.update({ id: mapperId }, map);
      }
      setupForm(map as ComponentRepresentation);
      addAlert(
        t(
          mapperId === "new"
            ? "common:mappingCreatedSuccess"
            : "common:mappingUpdatedSuccess"
        ),
        AlertVariant.success
      );
    } catch (error) {
      addError(
        mapperId === "new"
          ? "common:mappingCreatedError"
          : "common:mappingUpdatedError",
        error
      );
    }
  };

  const mapperType = useWatch({
    control: form.control,
    name: "providerId",
  });

  const isNew = mapperId === "new";

  if (!components) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <ViewHeader
        titleKey={mapping ? mapping.name! : t("common:createNewMapper")}
      />
      <PageSection variant="light" isFilled>
        <FormAccess role="manage-realm" isHorizontal>
          {!isNew && (
            <FormGroup label={t("common:id")} fieldId="kc-ldap-mapper-id">
              <TextInput
                isDisabled
                type="text"
                id="kc-ldap-mapper-id"
                data-testid="ldap-mapper-id"
                name="id"
                ref={form.register}
              />
            </FormGroup>
          )}
          <FormGroup
            label={t("common:name")}
            labelIcon={
              <HelpItem
                helpText="user-federation-help:nameHelp"
                fieldLabelId="name"
              />
            }
            fieldId="kc-ldap-mapper-name"
            isRequired
          >
            <TextInput
              isDisabled={!isNew}
              isRequired
              type="text"
              id="kc-ldap-mapper-name"
              data-testid="ldap-mapper-name"
              name="name"
              ref={form.register({ required: true })}
              validated={
                form.errors.name
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
            />
            <TextInput
              hidden
              defaultValue={isNew ? id : mapping ? mapping.parentId : ""}
              type="text"
              id="kc-ldap-parentId"
              data-testid="ldap-mapper-parentId"
              name="parentId"
              ref={form.register}
            />
            <TextInput
              hidden
              defaultValue="org.keycloak.storage.ldap.mappers.LDAPStorageMapper"
              type="text"
              id="kc-ldap-provider-type"
              data-testid="ldap-mapper-provider-type"
              name="providerType"
              ref={form.register}
            />
          </FormGroup>
          {!isNew ? (
            <FormGroup
              label={t("common:mapperType")}
              labelIcon={
                <HelpItem
                  helpText="user-federation-help:mapperTypeHelp"
                  fieldLabelId="mapperType"
                />
              }
              fieldId="kc-ldap-mapper-type"
              isRequired
            >
              <TextInput
                isDisabled={!isNew}
                isRequired
                type="text"
                id="kc-ldap-mapper-type"
                data-testid="ldap-mapper-type-fld"
                name="providerId"
                ref={form.register}
              />
            </FormGroup>
          ) : (
            <FormGroup
              label={t("common:mapperType")}
              labelIcon={
                <HelpItem
                  helpText="user-federation-help:mapperTypeHelp"
                  fieldLabelId="mapperType"
                />
              }
              fieldId="kc-providerId"
              isRequired
            >
              <Controller
                name="providerId"
                defaultValue=""
                control={form.control}
                data-testid="ldap-mapper-type-select"
                render={({ onChange, value }) => (
                  <Select
                    toggleId="kc-providerId"
                    required
                    onToggle={() =>
                      setIsMapperDropdownOpen(!isMapperDropdownOpen)
                    }
                    isOpen={isMapperDropdownOpen}
                    onSelect={(_, value) => {
                      onChange(value as string);
                      setIsMapperDropdownOpen(false);
                    }}
                    selections={value}
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
              <DynamicComponents
                properties={
                  components.find((c) => c.id === mapperType)?.properties!
                }
              />
            )}
          </FormProvider>
        </FormAccess>

        <Form onSubmit={form.handleSubmit(() => save(form.getValues()))}>
          <ActionGroup>
            <Button
              isDisabled={!form.formState.isDirty}
              variant="primary"
              type="submit"
              data-testid="ldap-mapper-save"
            >
              {t("common:save")}
            </Button>
            <Button
              variant="link"
              onClick={() =>
                isNew
                  ? history.goBack()
                  : history.push(
                      `/${realm}/user-federation/ldap/${
                        mapping!.parentId
                      }/mappers`
                    )
              }
              data-testid="ldap-mapper-cancel"
            >
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </Form>
      </PageSection>
    </>
  );
}
