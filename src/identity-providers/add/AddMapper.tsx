import React, { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Controller, useFieldArray, useForm } from "react-hook-form";
import {
  ActionGroup,
  AlertVariant,
  Button,
  FormGroup,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";

import { useRealm } from "../../context/realm-context/RealmContext";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import {
  AttributesForm,
  KeyValueType,
} from "../../components/attribute-form/AttributeForm";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import type { IdentityProviderAddMapperParams } from "../routes/AddMapper";
import _ from "lodash";
import { AssociatedRolesModal } from "../../realm-roles/AssociatedRolesModal";
import type { RoleRepresentation } from "../../model/role-model";
import { useAlerts } from "../../components/alert/Alerts";
import type { IdentityProviderEditMapperParams } from "../routes/EditMapper";
import { convertToFormValues } from "../../util";
import { toIdentityProvider } from "../routes/IdentityProvider";
import type IdentityProviderMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperRepresentation";

type IdPMapperRepresentationWithAttributes =
  IdentityProviderMapperRepresentation & {
    attributes: KeyValueType[];
  };

export const AddMapper = () => {
  const { t } = useTranslation("identity-providers");

  const form = useForm<IdPMapperRepresentationWithAttributes>();
  const { handleSubmit, control, register, errors } = form;
  const { addAlert, addError } = useAlerts();

  const { realm } = useRealm();
  const adminClient = useAdminClient();

  const { providerId, alias } = useParams<IdentityProviderAddMapperParams>();
  const { id } = useParams<IdentityProviderEditMapperParams>();

  const isSAMLorOIDC = providerId === "saml" || providerId === "oidc";

  const [mapperTypes, setMapperTypes] =
    useState<Record<string, IdentityProviderMapperRepresentation>>();
  const [mapperType, setMapperType] = useState("advancedAttributeToRole");
  const [currentMapper, setCurrentMapper] =
    useState<IdentityProviderMapperRepresentation>();
  const [roles, setRoles] = useState<RoleRepresentation[]>([]);

  const [rolesModalOpen, setRolesModalOpen] = useState(false);

  const save = async (idpMapper: IdentityProviderMapperRepresentation) => {
    if (id) {
      const updatedMapper = {
        ...idpMapper,
        identityProviderAlias: alias!,
        id: id,
        name: currentMapper?.name!,
        config: {
          ...idpMapper.config,
          attributes: JSON.stringify(idpMapper.config?.attributes!),
        },
      };
      try {
        await adminClient.identityProviders.updateMapper(
          {
            id: id!,
            alias: alias!,
          },
          updatedMapper
        );
        addAlert(t("mapperSaveSuccess"), AlertVariant.success);
      } catch (error) {
        addError(t("mapperSaveError"), error);
      }
    } else {
      try {
        await adminClient.identityProviders.createMapper({
          identityProviderMapper: {
            ...idpMapper,
            identityProviderAlias: alias,
            config: {
              ...idpMapper.config,
              attributes: JSON.stringify(idpMapper.config.attributes),
            },
          },
          alias: alias!,
        });
        addAlert(t("mapperCreateSuccess"), AlertVariant.success);
      } catch (error) {
        addError(t("mapperCreateError"), error);
      }
    }
  };

  const { append, remove, fields } = useFieldArray({
    control: form.control,
    name: "config.attributes",
  });

  useFetch(
    () =>
      Promise.all([
        id ? adminClient.identityProviders.findOneMapper({ alias, id }) : null,
        adminClient.identityProviders.findMapperTypes({ alias }),
        !id ? adminClient.roles.find() : null,
      ]),
    ([mapper, mapperTypes, roles]) => {
      if (mapper) {
        setCurrentMapper(mapper);
        setupForm(mapper);
      }

      setMapperTypes(mapperTypes);

      if (roles) {
        setRoles(roles);
      }
    },
    []
  );

  const setupForm = (mapper: IdentityProviderMapperRepresentation) => {
    form.reset();
    Object.entries(mapper).map(([key, value]) => {
      if (key === "config") {
        if (mapper.config?.["are-attribute-values-regex"]) {
          form.setValue(
            "config.are-attribute-values-regex",
            value["are-attribute-values-regex"][0]
          );
        }

        if (mapper.config?.attributes) {
          form.setValue("config.attributes", JSON.parse(value.attributes));
        }

        if (mapper.config?.role) {
          form.setValue("config.role", value.role[0]);
        }

        convertToFormValues(value, "config", form.setValue);
      }

      form.setValue(key, value);
    });
  };

  const syncModes = ["inherit", "import", "legacy", "force"];
  const [syncModeOpen, setSyncModeOpen] = useState(false);
  const [mapperTypeOpen, setMapperTypeOpen] = useState(false);
  const [selectedRole, setSelectedRole] = useState<RoleRepresentation[]>([]);

  const toggleModal = () => {
    setRolesModalOpen(!rolesModalOpen);
  };

  return (
    <PageSection variant="light">
      <ViewHeader
        className="kc-add-mapper-title"
        titleKey={
          id
            ? t("editIdPMapper", {
                providerId: providerId.toUpperCase(),
              })
            : t("addIdPMapper", {
                providerId: providerId.toUpperCase(),
              })
        }
        divider
      />
      <AssociatedRolesModal
        onConfirm={(role) => setSelectedRole(role)}
        allRoles={roles}
        open={rolesModalOpen}
        omitComposites
        isRadio
        isMapperId
        toggleDialog={toggleModal}
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
        <FormGroup
          label={t("common:name")}
          labelIcon={
            <HelpItem
              id="name-help-icon"
              helpText="identity-providers-help:addIdpMapperName"
              forLabel={t("common:name")}
              forID={t(`common:helpLabel`, { label: t("common:name") })}
            />
          }
          fieldId="kc-name"
          isRequired
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
          helperTextInvalid={t("common:required")}
        >
          <TextInput
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
              forLabel={t("syncModeOverride")}
              forID={t(`common:helpLabel`, { label: t("syncModeOverride") })}
            />
          }
          fieldId="syncMode"
        >
          <Controller
            name="config.syncMode"
            defaultValue={syncModes[0]}
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
              helpText={
                mapperType === "attributeImporter" &&
                (providerId === "oidc" || providerId === "keycloak-oidc")
                  ? `identity-providers-help:oidcAttributeImporter`
                  : `identity-providers-help:${mapperType}`
              }
              forLabel={t("mapperType")}
              forID={t(`common:helpLabel`, { label: t("mapperType") })}
            />
          }
          fieldId="identityProviderMapper"
        >
          <Controller
            name="identityProviderMapper"
            defaultValue={
              providerId === "saml"
                ? "saml-advanced-role-idp-mapper"
                : "oidc-advanced-role-idp-mapper"
            }
            control={control}
            render={({ onChange, value }) => (
              <Select
                toggleId="identityProviderMapper"
                data-testid="idp-mapper-select"
                isDisabled={!!id}
                required
                direction="down"
                onToggle={() => setMapperTypeOpen(!mapperTypeOpen)}
                onSelect={(e, value) => {
                  const theMapper =
                    mapperTypes &&
                    Object.values(mapperTypes).find(
                      (item) =>
                        item.name?.toLowerCase() ===
                        value.toString().toLowerCase()
                    );

                  setMapperType(_.camelCase(value.toString()));
                  onChange(theMapper?.id);
                  setMapperTypeOpen(false);
                }}
                selections={
                  mapperTypes &&
                  Object.values(mapperTypes).find(
                    (item) => item.id?.toLowerCase() === value
                  )?.name
                }
                variant={SelectVariant.single}
                aria-label={t("syncMode")}
                isOpen={mapperTypeOpen}
              >
                {mapperTypes &&
                  Object.values(mapperTypes).map((option) => (
                    <SelectOption
                      selected={option === value}
                      datatest-id={option.id}
                      key={option.name}
                      value={option.name?.toUpperCase()}
                    >
                      {t(`mapperTypes.${_.camelCase(option.name)}`)}
                    </SelectOption>
                  ))}
              </Select>
            )}
          />
        </FormGroup>
        {isSAMLorOIDC ? (
          <>
            {" "}
            <FormGroup
              label={t("common:attributes")}
              labelIcon={
                <HelpItem
                  helpText="identity-providers-help:attributes"
                  forLabel={t("attributes")}
                  forID={t(`common:helpLabel`, { label: t("attributes") })}
                />
              }
              fieldId="kc-gui-order"
            >
              <AttributesForm
                form={form}
                inConfig
                array={{ fields, append, remove }}
              />
            </FormGroup>
            <FormGroup
              label={t("regexAttributeValues")}
              labelIcon={
                <HelpItem
                  helpText="identity-providers-help:regexAttributeValues"
                  forLabel={t("regexAttributeValues")}
                  forID={t(`common:helpLabel`, {
                    label: t("regexAttributeValues"),
                  })}
                />
              }
              fieldId="regexAttributeValues"
            >
              <Controller
                name="config.are-attribute-values-regex"
                control={control}
                defaultValue="false"
                render={({ onChange, value }) => (
                  <Switch
                    id="regexAttributeValues"
                    data-testid="regex-attribute-values-switch"
                    label={t("common:on")}
                    labelOff={t("common:off")}
                    isChecked={value === "true"}
                    onChange={(value) => onChange("" + value)}
                  />
                )}
              />
            </FormGroup>
            <FormGroup
              label={t("common:role")}
              labelIcon={
                <HelpItem
                  id="name-help-icon"
                  helpText="identity-providers-help:role"
                  forLabel={t("identity-providers-help:role")}
                  forID={t(`identity-providers:helpLabel`, {
                    label: t("role"),
                  })}
                />
              }
              fieldId="kc-role"
              validated={
                errors.config?.role
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              helperTextInvalid={t("common:required")}
            >
              <TextInput
                ref={register()}
                type="text"
                id="kc-role"
                data-testid="mapper-role-input"
                name="config.role"
                value={selectedRole[0]?.name}
                validated={
                  errors.config?.role
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
              />
              <Button
                data-testid="select-role-button"
                onClick={() => toggleModal()}
              >
                {t("selectRole")}
              </Button>
            </FormGroup>{" "}
          </>
        ) : (
          <>
            <FormGroup
              label={t("userSessionAttribute")}
              labelIcon={
                <HelpItem
                  id="user-session-attribute-help-icon"
                  helpText="identity-providers-help:userSessionAttribute"
                  forLabel={t("userSessionAttribute")}
                  forID={t(`common:helpLabel`, {
                    label: t("userSessionAttribute"),
                  })}
                />
              }
              fieldId="kc-user-session-attribute"
              isRequired
              validated={
                errors.name ? ValidatedOptions.error : ValidatedOptions.default
              }
              helperTextInvalid={t("common:required")}
            >
              <TextInput
                ref={register({ required: true })}
                type="text"
                id="kc-attribute"
                data-testid="user-session-attribute"
                name="config.attribute"
                validated={
                  errors.name
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
              />
            </FormGroup>
            <FormGroup
              label={t("userSessionAttributeValue")}
              labelIcon={
                <HelpItem
                  id="user-session-attribute-value-help-icon"
                  helpText="identity-providers-help:userSessionAttributeValue"
                  forLabel={t("userSessionAttributeValue")}
                  forID={t(`common:helpLabel`, {
                    label: t("userSessionAttributeValue"),
                  })}
                />
              }
              fieldId="kc-user-session-attribute-value"
              isRequired
              validated={
                errors.name ? ValidatedOptions.error : ValidatedOptions.default
              }
              helperTextInvalid={t("common:required")}
            >
              <TextInput
                ref={register({ required: true })}
                type="text"
                data-testid="user-session-attribute-value"
                id="kc-user-session-attribute-value"
                name="config.attribute-value"
                validated={
                  errors.name
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
              />
            </FormGroup>
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
};
