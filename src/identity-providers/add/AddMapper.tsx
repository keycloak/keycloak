import React, { useMemo, useState } from "react";
import { Link, useHistory, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Controller, FormProvider, useForm } from "react-hook-form";
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
import { AttributeInput } from "../../components/attribute-input/AttributeInput";
import type { AttributeForm } from "../../components/attribute-form/AttributeForm";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import type { IdentityProviderAddMapperParams } from "../routes/AddMapper";
import { AssociatedRolesModal } from "../../realm-roles/AssociatedRolesModal";
import type { RoleRepresentation } from "../../model/role-model";
import { useAlerts } from "../../components/alert/Alerts";
import {
  IdentityProviderEditMapperParams,
  toIdentityProviderEditMapper,
} from "../routes/EditMapper";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import { toIdentityProvider } from "../routes/IdentityProvider";
import type IdentityProviderMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperRepresentation";
import { AddMapperForm } from "./AddMapperForm";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { groupBy } from "lodash";

export type IdPMapperRepresentationWithAttributes =
  IdentityProviderMapperRepresentation & AttributeForm;

export type Role = RoleRepresentation & {
  clientId?: string;
};

export default function AddMapper() {
  const { t } = useTranslation("identity-providers");

  const form = useForm<IdPMapperRepresentationWithAttributes>();
  const { handleSubmit, control, register, errors } = form;
  const { addAlert, addError } = useAlerts();
  const history = useHistory();

  const { realm } = useRealm();
  const adminClient = useAdminClient();

  const { providerId, alias } = useParams<IdentityProviderAddMapperParams>();
  const { id } = useParams<IdentityProviderEditMapperParams>();

  const serverInfo = useServerInfo();
  const identityProviders = useMemo(
    () => groupBy(serverInfo.identityProviders, "groupName"),
    [serverInfo]
  );

  const isSocialIdP = useMemo(
    () =>
      identityProviders["Social"]
        .map((item) => item.id)
        .includes(providerId.toLowerCase()),
    [identityProviders, providerId]
  );

  const [mapperTypes, setMapperTypes] =
    useState<Record<string, IdentityProviderMapperRepresentation>>();
  const [mapperType, setMapperType] = useState(
    isSocialIdP
      ? "attributeImporter"
      : providerId === "saml"
      ? "advancedAttributeToRole"
      : "hardcodedUserSessionAttribute"
  );

  const [currentMapper, setCurrentMapper] =
    useState<IdentityProviderMapperRepresentation>();

  const [rolesModalOpen, setRolesModalOpen] = useState(false);

  const save = async (idpMapper: IdentityProviderMapperRepresentation) => {
    const attributes = JSON.stringify(idpMapper.config?.attributes ?? []);
    const mapper = convertFormValuesToObject(idpMapper);

    if (id) {
      const updatedMapper = {
        ...mapper,
        config: {
          attributes,
        },
        identityProviderAlias: alias!,
        id: id,
        name: currentMapper?.name!,
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
        const createdMapper = await adminClient.identityProviders.createMapper({
          identityProviderMapper: {
            ...mapper,
            identityProviderAlias: alias,
            config: {
              attributes,
            },
          },
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
      }

      setMapperTypes(mapperTypes);
    },
    []
  );

  const setupForm = (mapper: IdentityProviderMapperRepresentation) => {
    form.reset();
    convertToFormValues(mapper, form.setValue);
    form.setValue("config.attributes", JSON.parse(mapper.config.attributes));
  };

  const targetOptions = ["local", "brokerId", "brokerUsername"];
  const [targetOptionsOpen, setTargetOptionsOpen] = useState(false);
  const [selectedRole, setSelectedRole] = useState<Role[]>([]);

  const formValues = form.getValues();

  const isSAMLAdvancedAttrToRole =
    formValues.identityProviderMapper === "saml-advanced-role-idp-mapper";

  const isOIDCclaimToRole =
    formValues.identityProviderMapper === "oidc-role-idp-mapper";

  const isOIDCAdvancedClaimToRole =
    formValues.identityProviderMapper === "oidc-advanced-role-idp-mapper";

  const isSAMLAttributeImporter =
    formValues.identityProviderMapper === "saml-user-attribute-idp-mapper";

  const isOIDCAttributeImporter =
    formValues.identityProviderMapper === "oidc-user-attribute-idp-mapper";

  const isHardcodedAttribute =
    formValues.identityProviderMapper === "hardcoded-attribute-idp-mapper";

  const isHardcodedRole =
    formValues.identityProviderMapper === "oidc-hardcoded-role-idp-mapper";

  const isHardcodedUserSessionAttribute =
    formValues.identityProviderMapper ===
    "hardcoded-user-session-attribute-idp-mapper";

  const isSAMLAttributeToRole =
    formValues.identityProviderMapper === "saml-role-idp-mapper";

  const isSAMLUsernameTemplateImporter =
    formValues.identityProviderMapper === "saml-username-idp-mapper";

  const isOIDCUsernameTemplateImporter =
    formValues.identityProviderMapper === "oidc-username-idp-mapper";

  const isSocialAttributeImporter = useMemo(
    () => formValues.identityProviderMapper?.includes("user-attribute-mapper"),
    [formValues.identityProviderMapper]
  );
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
      {rolesModalOpen && (
        <AssociatedRolesModal
          id={id}
          onConfirm={(role) => setSelectedRole(role)}
          omitComposites
          isRadio
          isMapperId
          toggleDialog={toggleModal}
        />
      )}
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
          formValues={formValues}
          mapperType={mapperType}
          isSocialIdP={isSocialIdP}
        />
        <>
          {(isSAMLAdvancedAttrToRole || isOIDCAdvancedClaimToRole) && (
            <>
              <FormGroup
                label={
                  isSAMLAdvancedAttrToRole
                    ? t("common:attributes")
                    : t("claims")
                }
                labelIcon={
                  <HelpItem
                    helpText={
                      isSAMLAdvancedAttrToRole
                        ? "identity-providers-help:attributes"
                        : "identity-providers-help:claims"
                    }
                    fieldLabelId={
                      isSAMLAdvancedAttrToRole ? "attributes" : "claims"
                    }
                  />
                }
                fieldId="kc-gui-order"
              >
                <FormProvider {...form}>
                  <AttributeInput name="config.attributes" />
                </FormProvider>
              </FormGroup>
              <FormGroup
                label={
                  isSAMLAdvancedAttrToRole
                    ? t("regexAttributeValues")
                    : t("regexClaimValues")
                }
                labelIcon={
                  <HelpItem
                    helpText="identity-providers-help:regexAttributeValues"
                    fieldLabelId="identity-providers:regexAttributeValues"
                  />
                }
                fieldId="regexAttributeValues"
              >
                <Controller
                  name={
                    isOIDCAdvancedClaimToRole
                      ? "config.are.claim.values.regex"
                      : "config.are.attribute.values.regex"
                  }
                  control={control}
                  defaultValue="false"
                  render={({ onChange, value }) => (
                    <Switch
                      id="regexValues"
                      data-testid="regex-values-switch"
                      label={t("common:on")}
                      labelOff={t("common:off")}
                      isChecked={value === "true"}
                      onChange={(value) => onChange("" + value)}
                    />
                  )}
                />
              </FormGroup>
            </>
          )}
          {(isSAMLUsernameTemplateImporter ||
            isOIDCUsernameTemplateImporter) && (
            <>
              <FormGroup
                label={t("template")}
                labelIcon={
                  <HelpItem
                    helpText="identity-providers-help:template"
                    fieldLabelId="identity-providers:template"
                  />
                }
                fieldId="kc-user-session-attribute"
                validated={
                  errors.name
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                helperTextInvalid={t("common:required")}
              >
                <TextInput
                  ref={register()}
                  type="text"
                  id="kc-template"
                  data-testid="template"
                  name="config.template"
                  defaultValue={currentMapper?.config.template}
                  validated={
                    errors.name
                      ? ValidatedOptions.error
                      : ValidatedOptions.default
                  }
                />
              </FormGroup>
              <FormGroup
                label={t("target")}
                labelIcon={
                  <HelpItem
                    helpText="identity-providers-help:target"
                    fieldLabelId="identity-providers:target"
                  />
                }
                fieldId="kc-target"
                validated={
                  errors.name
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                helperTextInvalid={t("common:required")}
              >
                <Controller
                  name="config.target"
                  defaultValue={currentMapper?.config.target}
                  control={control}
                  render={({ onChange, value }) => (
                    <Select
                      toggleId="target"
                      datatest-id="target-select"
                      id="target-dropdown"
                      placeholderText={t("realm-settings:placeholderText")}
                      direction="down"
                      onToggle={() => setTargetOptionsOpen(!targetOptionsOpen)}
                      onSelect={(_, value) => {
                        onChange(t(`targetOptions.${value}`));
                        setTargetOptionsOpen(false);
                      }}
                      selections={value}
                      variant={SelectVariant.single}
                      aria-label={t("target")}
                      isOpen={targetOptionsOpen}
                    >
                      {targetOptions.map((option) => (
                        <SelectOption
                          selected={option === value}
                          key={option}
                          data-testid={option}
                          value={option}
                        >
                          {t(`targetOptions.${option}`)}
                        </SelectOption>
                      ))}
                    </Select>
                  )}
                />
              </FormGroup>
            </>
          )}

          {(isHardcodedAttribute || isHardcodedUserSessionAttribute) && (
            <>
              <FormGroup
                label={
                  isHardcodedUserSessionAttribute
                    ? t("userSessionAttribute")
                    : t("userAttribute")
                }
                labelIcon={
                  <HelpItem
                    helpText="identity-providers-help:userSessionAttribute"
                    fieldLabelId="identity-providers:userSessionAttribute"
                  />
                }
                fieldId="kc-user-session-attribute"
                validated={
                  errors.name
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                helperTextInvalid={t("common:required")}
              >
                <TextInput
                  ref={register()}
                  type="text"
                  defaultValue={currentMapper?.config.attribute}
                  id="kc-attribute"
                  data-testid={
                    isHardcodedUserSessionAttribute
                      ? "user-session-attribute"
                      : "user-attribute"
                  }
                  name="config.attribute"
                  validated={
                    errors.name
                      ? ValidatedOptions.error
                      : ValidatedOptions.default
                  }
                />
              </FormGroup>
              <FormGroup
                label={
                  isHardcodedUserSessionAttribute
                    ? t("userSessionAttributeValue")
                    : t("userAttributeValue")
                }
                labelIcon={
                  <HelpItem
                    helpText="identity-providers-help:userAttributeValue"
                    fieldLabelId={
                      isHardcodedUserSessionAttribute
                        ? "identity-providers:userSessionAttributeValue"
                        : "identity-providers:userAttributeValue"
                    }
                  />
                }
                fieldId="kc-user-session-attribute-value"
                validated={
                  errors.name
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                helperTextInvalid={t("common:required")}
              >
                <TextInput
                  ref={register()}
                  type="text"
                  defaultValue={currentMapper?.config["attribute.value"]}
                  data-testid={
                    isHardcodedUserSessionAttribute
                      ? "user-session-attribute-value"
                      : "user-attribute-value"
                  }
                  id="kc-user-session-attribute-value"
                  name="config.attribute.value"
                  validated={
                    errors.name
                      ? ValidatedOptions.error
                      : ValidatedOptions.default
                  }
                />
              </FormGroup>
            </>
          )}
          {(isSAMLAttributeImporter ||
            isOIDCAttributeImporter ||
            isOIDCclaimToRole) && (
            <>
              {isSAMLAttributeImporter ? (
                <>
                  <FormGroup
                    label={t("mapperAttributeName")}
                    labelIcon={
                      <HelpItem
                        helpText="identity-providers-help:attributeName"
                        fieldLabelId="identity-providers:mapperAttributeName"
                      />
                    }
                    fieldId="kc-attribute-name"
                    validated={
                      errors.name
                        ? ValidatedOptions.error
                        : ValidatedOptions.default
                    }
                    helperTextInvalid={t("common:required")}
                  >
                    <TextInput
                      ref={register()}
                      type="text"
                      defaultValue={currentMapper?.config["attribute.name"]}
                      id="kc-attribute-name"
                      data-testid="attribute-name"
                      name="config.attribute.name"
                      validated={
                        errors.name
                          ? ValidatedOptions.error
                          : ValidatedOptions.default
                      }
                    />
                  </FormGroup>
                  <FormGroup
                    label={t("mapperAttributeFriendlyName")}
                    labelIcon={
                      <HelpItem
                        helpText="identity-providers-help:friendlyName"
                        fieldLabelId="identity-providers:mapperAttributeFriendlyName"
                      />
                    }
                    fieldId="kc-friendly-name"
                    validated={
                      errors.name
                        ? ValidatedOptions.error
                        : ValidatedOptions.default
                    }
                    helperTextInvalid={t("common:required")}
                  >
                    <TextInput
                      ref={register()}
                      type="text"
                      defaultValue={
                        currentMapper?.config["attribute.friendly.name"]
                      }
                      data-testid="attribute-friendly-name"
                      id="kc-attribute-friendly-name"
                      name="config.attribute.friendly.name"
                      validated={
                        errors.name
                          ? ValidatedOptions.error
                          : ValidatedOptions.default
                      }
                    />
                  </FormGroup>
                </>
              ) : (
                <FormGroup
                  label={t("claim")}
                  labelIcon={
                    <HelpItem
                      helpText="identity-providers-help:claim"
                      fieldLabelId="identity-providers:claim"
                    />
                  }
                  fieldId="kc-friendly-name"
                  validated={
                    errors.name
                      ? ValidatedOptions.error
                      : ValidatedOptions.default
                  }
                  helperTextInvalid={t("common:required")}
                >
                  <TextInput
                    ref={register()}
                    type="text"
                    defaultValue={currentMapper?.config["claim"]}
                    data-testid="claim"
                    id="kc-claim"
                    name={"config.claim"}
                    validated={
                      errors.name
                        ? ValidatedOptions.error
                        : ValidatedOptions.default
                    }
                  />
                </FormGroup>
              )}
              <FormGroup
                label={
                  isOIDCclaimToRole
                    ? t("claimValue")
                    : t("mapperUserAttributeName")
                }
                labelIcon={
                  <HelpItem
                    helpText={
                      isOIDCclaimToRole
                        ? "identity-providers-help:claimValue"
                        : "identity-providers-help:userAttributeName"
                    }
                    fieldLabelId="identity-providers:mapperUserAttributeName"
                  />
                }
                fieldId="kc-user-attribute-name"
                validated={
                  errors.name
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                helperTextInvalid={t("common:required")}
              >
                <TextInput
                  ref={register()}
                  type="text"
                  defaultValue={
                    isOIDCclaimToRole
                      ? currentMapper?.config["claim.value"]
                      : currentMapper?.config["attribute.value"]
                  }
                  data-testid={
                    isOIDCclaimToRole ? "claim.value" : "user-attribute-name"
                  }
                  id={
                    isOIDCclaimToRole
                      ? "kc-claim-value"
                      : "kc-user-attribute-name"
                  }
                  name={
                    isOIDCclaimToRole ? "config.claim" : "config.user.attribute"
                  }
                  validated={
                    errors.name
                      ? ValidatedOptions.error
                      : ValidatedOptions.default
                  }
                />
              </FormGroup>
            </>
          )}

          {isSocialAttributeImporter && (
            <>
              <FormGroup
                label={t("socialProfileJSONFieldPath")}
                labelIcon={
                  <HelpItem
                    helpText="identity-providers-help:socialProfileJSONFieldPath"
                    fieldLabelId="identity-providers:socialProfileJSONFieldPath"
                  />
                }
                fieldId="kc-social-profile-JSON-field-path"
                validated={
                  errors.name
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                helperTextInvalid={t("common:required")}
              >
                <TextInput
                  ref={register()}
                  type="text"
                  defaultValue={currentMapper?.config.attribute}
                  id="kc-social-profile-JSON-field-path"
                  data-testid={"social-profile-JSON-field-path"}
                  name="config.jsonField"
                  validated={
                    errors.config?.role
                      ? ValidatedOptions.error
                      : ValidatedOptions.default
                  }
                />
              </FormGroup>
              <FormGroup
                label={t("mapperUserAttributeName")}
                labelIcon={
                  <HelpItem
                    helpText="identity-providers-help:socialUserAttributeName"
                    fieldLabelId="identity-providers:mapperUserAttributeName"
                  />
                }
                fieldId="kc-user-session-attribute-value"
                validated={
                  errors.name
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                helperTextInvalid={t("common:required")}
              >
                <TextInput
                  ref={register()}
                  type="text"
                  defaultValue={currentMapper?.config.userAttribute}
                  data-testid={"user-attribute-name"}
                  id="kc-user-session-attribute-name"
                  name="config.userAttribute"
                  validated={
                    errors.name
                      ? ValidatedOptions.error
                      : ValidatedOptions.default
                  }
                />
              </FormGroup>
            </>
          )}
          {(isSAMLAdvancedAttrToRole ||
            isHardcodedRole ||
            isSAMLAttributeToRole ||
            isOIDCAdvancedClaimToRole ||
            isOIDCclaimToRole) && (
            <FormGroup
              label={t("common:role")}
              labelIcon={
                <HelpItem
                  helpText="identity-providers-help:role"
                  fieldLabelId="role"
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
                isDisabled={!!id}
                value={
                  selectedRole[0]?.clientRole
                    ? `${selectedRole[0].clientId}.${selectedRole[0]?.name}`
                    : selectedRole[0]?.name
                }
                validated={
                  errors.config?.role
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
              />
              <Button
                data-testid="select-role-button"
                onClick={() => toggleModal()}
                isDisabled={!!id}
              >
                {t("selectRole")}
              </Button>
            </FormGroup>
          )}
        </>
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
