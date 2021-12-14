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
import { Controller, useForm, useWatch } from "react-hook-form";
import { useAlerts } from "../../../components/alert/Alerts";
import { useTranslation } from "react-i18next";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { FormAccess } from "../../../components/form-access/FormAccess";

import { LdapMapperUserAttribute } from "./LdapMapperUserAttribute";
import { LdapMapperMsadUserAccount } from "./LdapMapperMsadUserAccount";
import { LdapMapperFullNameAttribute } from "./LdapMapperFullNameAttribute";

import { LdapMapperHardcodedLdapRole } from "./LdapMapperHardcodedLdapRole";
import { LdapMapperHardcodedLdapGroup } from "./LdapMapperHardcodedLdapGroup";
import { LdapMapperHardcodedLdapAttribute } from "./LdapMapperHardcodedLdapAttribute";
import { LdapMapperHardcodedAttribute } from "./LdapMapperHardcodedAttribute";

import { LdapMapperRoleGroup } from "./LdapMapperRoleGroup";
import { useRealm } from "../../../context/realm-context/RealmContext";

export default function LdapMapperDetails() {
  const form = useForm<ComponentRepresentation>();
  const [mapping, setMapping] = useState<ComponentRepresentation>();

  const adminClient = useAdminClient();
  const { id, mapperId } = useParams<{ id: string; mapperId: string }>();
  const history = useHistory();
  const { realm } = useRealm();
  const { t } = useTranslation("user-federation");
  const { addAlert, addError } = useAlerts();

  const [isMapperDropdownOpen, setIsMapperDropdownOpen] = useState(false);

  useFetch(
    async () => {
      if (mapperId && mapperId !== "new") {
        const fetchedMapper = await adminClient.components.findOne({
          id: mapperId,
        });
        if (!fetchedMapper) {
          throw new Error(t("common:notFound"));
        }
        return fetchedMapper;
      }
    },
    (fetchedMapper) => {
      setMapping(fetchedMapper);
      if (fetchedMapper) {
        setupForm(fetchedMapper);
      }
    },
    []
  );

  const setupForm = (mapper: ComponentRepresentation) => {
    convertToFormValues(mapper, form.setValue);
  };

  const save = async (mapper: ComponentRepresentation) => {
    const map = convertFormValuesToObject(mapper);

    try {
      if (mapperId) {
        if (mapperId === "new") {
          await adminClient.components.create(map);
          history.push(
            `/${realm}/user-federation/ldap/${mapper!.parentId}/mappers`
          );
        } else {
          await adminClient.components.update({ id: mapperId }, map);
        }
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
                    <SelectOption
                      key={0}
                      value="msad-user-account-control-mapper"
                    />
                    <SelectOption
                      key={1}
                      value="msad-lds-user-account-control-mapper"
                    />
                    <SelectOption key={2} value="group-ldap-mapper" />
                    <SelectOption key={3} value="user-attribute-ldap-mapper" />
                    <SelectOption key={4} value="role-ldap-mapper" />
                    <SelectOption key={5} value="hardcoded-attribute-mapper" />
                    <SelectOption key={6} value="hardcoded-ldap-role-mapper" />
                    <SelectOption key={7} value="certificate-ldap-mapper" />
                    <SelectOption key={8} value="full-name-ldap-mapper" />
                    <SelectOption key={9} value="hardcoded-ldap-group-mapper" />
                    <SelectOption
                      key={10}
                      value="hardcoded-ldap-attribute-mapper"
                    />
                  </Select>
                )}
              ></Controller>
            </FormGroup>
          )}
          {/* When loading existing mappers, load forms based on providerId aka mapper type */}
          {mapping
            ? (mapping.providerId! === "certificate-ldap-mapper" ||
                mapping.providerId! === "user-attribute-ldap-mapper") && (
                <LdapMapperUserAttribute
                  form={form}
                  mapperType={mapping.providerId}
                />
              )
            : ""}
          {mapping
            ? mapping.providerId! === "msad-user-account-control-mapper" && (
                <LdapMapperMsadUserAccount form={form} />
              )
            : ""}
          {/* msad-lds-user-account-control-mapper does not need a component 
              because it is just id, name, and mapper type*/}
          {mapping
            ? mapping.providerId! === "full-name-ldap-mapper" && (
                <LdapMapperFullNameAttribute form={form} />
              )
            : ""}
          {mapping
            ? mapping.providerId! === "hardcoded-ldap-role-mapper" && (
                <LdapMapperHardcodedLdapRole form={form} />
              )
            : ""}
          {mapping
            ? mapping.providerId! === "hardcoded-ldap-group-mapper" && (
                <LdapMapperHardcodedLdapGroup form={form} />
              )
            : ""}
          {mapping
            ? mapping.providerId! === "hardcoded-ldap-attribute-mapper" && (
                <LdapMapperHardcodedLdapAttribute form={form} />
              )
            : ""}
          {mapping
            ? mapping.providerId! === "hardcoded-attribute-mapper" && (
                <LdapMapperHardcodedAttribute form={form} />
              )
            : ""}
          {mapping
            ? (mapping.providerId! === "role-ldap-mapper" ||
                mapping.providerId! === "group-ldap-mapper") && (
                <LdapMapperRoleGroup form={form} type={mapping.providerId} />
              )
            : ""}
          {/* When creating new mappers, load forms based on dropdown selection */}
          {isNew && mapperType
            ? mapperType === "certificate-ldap-mapper" && (
                <LdapMapperUserAttribute form={form} mapperType={mapperType} />
              )
            : ""}
          {isNew && mapperType
            ? mapperType === "user-attribute-ldap-mapper" && (
                <LdapMapperUserAttribute form={form} mapperType={mapperType} />
              )
            : ""}
          {isNew && mapperType
            ? mapperType === "msad-user-account-control-mapper" && (
                <LdapMapperMsadUserAccount form={form} />
              )
            : ""}
          {isNew && mapperType
            ? mapperType === "full-name-ldap-mapper" && (
                <LdapMapperFullNameAttribute form={form} />
              )
            : ""}
          {isNew && mapperType
            ? mapperType === "hardcoded-ldap-role-mapper" && (
                <LdapMapperHardcodedLdapRole form={form} />
              )
            : ""}
          {isNew && mapperType
            ? mapperType === "hardcoded-ldap-group-mapper" && (
                <LdapMapperHardcodedLdapGroup form={form} />
              )
            : ""}
          {isNew && mapperType
            ? mapperType === "hardcoded-ldap-attribute-mapper" && (
                <LdapMapperHardcodedLdapAttribute form={form} />
              )
            : ""}
          {isNew && mapperType
            ? mapperType === "hardcoded-attribute-mapper" && (
                <LdapMapperHardcodedAttribute form={form} />
              )
            : ""}
          {isNew && mapperType
            ? mapperType === "role-ldap-mapper" && (
                <LdapMapperRoleGroup form={form} type={mapperType} />
              )
            : ""}
          {isNew && mapperType
            ? mapperType === "group-ldap-mapper" && (
                <LdapMapperRoleGroup form={form} type={mapperType} />
              )
            : ""}
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
