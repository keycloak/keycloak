import type ProtocolMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/protocolMapperRepresentation";
import type { ProtocolMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
import { TextControl, useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  FormGroup,
  PageSection,
  TextInput,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useMatch, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { toDedicatedScope } from "../../clients/routes/DedicatedScopeDetails";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { FormAccess } from "../../components/form/FormAccess";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import { useParams } from "../../utils/useParams";
import { toClientScope } from "../routes/ClientScope";
import { MapperParams, MapperRoute } from "../routes/Mapper";

export default function MappingDetails() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const { id, mapperId } = useParams<MapperParams>();
  const form = useForm();
  const { setValue, handleSubmit } = form;
  const [mapping, setMapping] = useState<ProtocolMapperTypeRepresentation>();
  const [config, setConfig] = useState<{
    protocol?: string;
    protocolMapper?: string;
  }>();

  const navigate = useNavigate();
  const { realm } = useRealm();
  const serverInfo = useServerInfo();
  const isGuid = /^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$/;
  const isUpdating = !!isGuid.exec(mapperId);

  const isOnClientScope = !!useMatch(MapperRoute.path);
  const toDetails = () =>
    isOnClientScope
      ? toClientScope({ realm, id, tab: "mappers" })
      : toDedicatedScope({ realm, clientId: id, tab: "mappers" });

  useFetch(
    async () => {
      let data: ProtocolMapperRepresentation | undefined;
      if (isUpdating) {
        if (isOnClientScope) {
          data = await adminClient.clientScopes.findProtocolMapper({
            id,
            mapperId,
          });
        } else {
          data = await adminClient.clients.findProtocolMapperById({
            id,
            mapperId,
          });
        }
        if (!data) {
          throw new Error(t("notFound"));
        }

        const mapperTypes = serverInfo.protocolMapperTypes![data!.protocol!];
        const mapping = mapperTypes.find(
          (type) => type.id === data!.protocolMapper,
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
        const model = isOnClientScope
          ? await adminClient.clientScopes.findOne({ id })
          : await adminClient.clients.findOne({ id });
        if (!model) {
          throw new Error(t("notFound"));
        }
        const protocolMappers =
          serverInfo.protocolMapperTypes![model.protocol!];
        const mapping = protocolMappers.find(
          (mapper) => mapper.id === mapperId,
        );
        if (!mapping) {
          throw new Error(t("notFound"));
        }
        return {
          mapping,
          config: {
            protocol: model.protocol,
            protocolMapper: mapperId,
          },
        };
      }
    },
    ({ config, mapping, data }) => {
      setConfig(config);
      setMapping(mapping);
      if (data) {
        convertToFormValues(data, setValue);
      }
    },
    [],
  );

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteMappingTitle",
    messageKey: "deleteMappingConfirm",
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        if (isOnClientScope) {
          await adminClient.clientScopes.delProtocolMapper({
            id,
            mapperId,
          });
        } else {
          await adminClient.clients.delProtocolMapper({
            id,
            mapperId,
          });
        }
        addAlert(t("mappingDeletedSuccess"), AlertVariant.success);
        navigate(toDetails());
      } catch (error) {
        addError("mappingDeletedError", error);
      }
    },
  });

  const save = async (formMapping: ProtocolMapperRepresentation) => {
    const key = isUpdating ? "Updated" : "Created";
    try {
      const mapping = { ...config, ...convertFormValuesToObject(formMapping) };
      if (isUpdating) {
        if (isOnClientScope) {
          await adminClient.clientScopes.updateProtocolMapper(
            { id, mapperId },
            { id: mapperId, ...mapping },
          );
        } else {
          await adminClient.clients.updateProtocolMapper(
            { id, mapperId },
            { id: mapperId, ...mapping },
          );
        }
      } else {
        if (isOnClientScope) {
          await adminClient.clientScopes.addProtocolMapper({ id }, mapping);
        } else {
          await adminClient.clients.addProtocolMapper({ id }, mapping);
        }
      }
      addAlert(t(`mapping${key}Success`), AlertVariant.success);
    } catch (error) {
      addError(`mapping${key}Error`, error);
    }
  };

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={isUpdating ? mapping?.name! : t("addMapper")}
        subKey={isUpdating ? mapperId : "addMapperExplain"}
        dropdownItems={
          isUpdating
            ? [
                <DropdownItem
                  key="delete"
                  value="delete"
                  onClick={toggleDeleteDialog}
                >
                  {t("delete")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <PageSection variant="light">
        <FormProvider {...form}>
          <FormAccess
            isHorizontal
            onSubmit={handleSubmit(save)}
            role="manage-clients"
          >
            <FormGroup label={t("mapperType")} fieldId="mapperType">
              <TextInput
                type="text"
                id="mapperType"
                name="mapperType"
                readOnlyVariant="default"
                value={mapping?.name}
              />
            </FormGroup>
            <TextControl
              name="name"
              label={t("name")}
              labelIcon={t("mapperNameHelp")}
              readOnlyVariant={isUpdating ? "default" : undefined}
              rules={{ required: t("required") }}
            />
            <DynamicComponents
              properties={mapping?.properties || []}
              isNew={!isUpdating}
              stringify
            />
            <ActionGroup>
              <Button variant="primary" type="submit">
                {t("save")}
              </Button>
              <Button
                variant="link"
                component={(props) => <Link {...props} to={toDetails()} />}
              >
                {t("cancel")}
              </Button>
            </ActionGroup>
          </FormAccess>
        </FormProvider>
      </PageSection>
    </>
  );
}
