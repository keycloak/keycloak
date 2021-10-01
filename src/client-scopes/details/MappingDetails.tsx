import React, { useState } from "react";
import { Link, useHistory, useParams } from "react-router-dom";
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
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import type ProtocolMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/protocolMapperRepresentation";
import type { ProtocolMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";

import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useAlerts } from "../../components/alert/Alerts";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useRealm } from "../../context/realm-context/RealmContext";
import type { MapperParams } from "../routes/Mapper";
import { Components, COMPONENTS } from "../add/components/components";

import "./mapping-details.css";
import { toClientScope } from "../routes/ClientScope";

export const MappingDetails = () => {
  const { t } = useTranslation("client-scopes");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const { id, mapperId, type } = useParams<MapperParams>();
  const form = useForm();
  const { register, setValue, errors, handleSubmit } = form;
  const [mapping, setMapping] = useState<ProtocolMapperTypeRepresentation>();
  const [config, setConfig] =
    useState<{ protocol?: string; protocolMapper?: string }>();

  const history = useHistory();
  const { realm } = useRealm();
  const serverInfo = useServerInfo();
  const isGuid = /^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$/;
  const isUpdating = mapperId.match(isGuid);

  useFetch(
    async () => {
      if (isUpdating) {
        const data = await adminClient.clientScopes.findProtocolMapper({
          id,
          mapperId,
        });
        if (!data) {
          throw new Error(t("common:notFound"));
        }

        const mapperTypes = serverInfo.protocolMapperTypes![data!.protocol!];
        const mapping = mapperTypes.find(
          (type) => type.id === data!.protocolMapper
        );

        return {
          config: {
            protocol: data.protocol,
            protocolMapper: data.protocolMapper,
          },
          mapping,
          data,
        };
      } else {
        const scope = await adminClient.clientScopes.findOne({ id });
        if (!scope) {
          throw new Error(t("common:notFound"));
        }
        const protocolMappers =
          serverInfo.protocolMapperTypes![scope.protocol!];
        const mapping = protocolMappers.find(
          (mapper) => mapper.id === mapperId
        );
        if (!mapping) {
          throw new Error(t("common:notFound"));
        }
        return {
          mapping,
          config: {
            protocol: scope.protocol,
            protocolMapper: mapperId,
          },
        };
      }
    },
    ({ config, mapping, data }) => {
      setConfig(config);
      setMapping(mapping);
      if (data) {
        Object.entries(data).map(([key, value]) => {
          if (key === "config") {
            convertToFormValues(value, "config", setValue);
          }
          setValue(key, value);
        });
      }
    },
    []
  );

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "common:deleteMappingTitle",
    messageKey: "common:deleteMappingConfirm",
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.clientScopes.delProtocolMapper({
          id,
          mapperId: mapperId,
        });
        addAlert(t("common:mappingDeletedSuccess"), AlertVariant.success);
        history.push(`/${realm}/client-scopes/${id}/mappers`);
      } catch (error) {
        addError("common:mappingDeletedError", error);
      }
    },
  });

  const save = async (formMapping: ProtocolMapperRepresentation) => {
    const configAttributes = convertFormValuesToObject(formMapping.config);
    const key = isUpdating ? "Updated" : "Created";
    try {
      if (isUpdating) {
        await adminClient.clientScopes.updateProtocolMapper(
          { id, mapperId },
          { ...formMapping, config: configAttributes }
        );
      } else {
        await adminClient.clientScopes.addProtocolMapper(
          { id },
          { ...formMapping, ...config, config: configAttributes }
        );
      }
      addAlert(t(`common:mapping${key}Success`), AlertVariant.success);
    } catch (error) {
      addError(`common:mapping${key}Error`, error);
    }
  };

  const isValidComponentType = (value: string): value is Components =>
    value in COMPONENTS;

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={isUpdating ? mapping?.name! : t("common:addMapper")}
        subKey={isUpdating ? mapperId : "client-scopes:addMapperExplain"}
        dropdownItems={
          isUpdating
            ? [
                <DropdownItem
                  key="delete"
                  value="delete"
                  onClick={toggleDeleteDialog}
                >
                  {t("common:delete")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          onSubmit={handleSubmit(save)}
          role="manage-clients"
          className="keycloak__client-scope-mapping-details__form"
        >
          {!mapperId.match(isGuid) && (
            <>
              <FormGroup label={t("common:mapperType")} fieldId="mapperType">
                <TextInput
                  type="text"
                  id="mapperType"
                  name="mapperType"
                  isReadOnly
                  value={mapping?.name}
                />
              </FormGroup>
              <FormGroup
                label={t("common:name")}
                labelIcon={
                  <HelpItem
                    helpText="client-scopes-help:mapperName"
                    forLabel={t("common:name")}
                    forID="name"
                  />
                }
                fieldId="name"
                isRequired
                validated={
                  errors.name
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                helperTextInvalid={t("common:required")}
              >
                <TextInput
                  ref={register({ required: true })}
                  type="text"
                  id="name"
                  name="name"
                  validated={
                    errors.name
                      ? ValidatedOptions.error
                      : ValidatedOptions.default
                  }
                />
              </FormGroup>
            </>
          )}
          <FormProvider {...form}>
            {mapping?.properties.map((property) => {
              const componentType = property.type!;
              if (isValidComponentType(componentType)) {
                const Component = COMPONENTS[componentType];
                return <Component key={property.name} {...property} />;
              } else {
                console.warn(
                  `There is no editor registered for ${componentType}`
                );
              }
            })}
          </FormProvider>
          <ActionGroup>
            <Button variant="primary" type="submit">
              {t("common:save")}
            </Button>
            <Button
              variant="link"
              component={(props) => (
                <Link
                  {...props}
                  to={toClientScope({ realm, id, type, tab: "mappers" })}
                />
              )}
            >
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
};
