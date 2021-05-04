import React, { useState } from "react";
import { useHistory, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Checkbox,
  DropdownItem,
  Flex,
  FlexItem,
  FormGroup,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import type { ConfigPropertyRepresentation } from "keycloak-admin/lib/defs/configPropertyRepresentation";
import type ProtocolMapperRepresentation from "keycloak-admin/lib/defs/protocolMapperRepresentation";

import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { Controller, useForm } from "react-hook-form";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useAlerts } from "../../components/alert/Alerts";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useRealm } from "../../context/realm-context/RealmContext";

type Params = {
  id: string;
  mapperId: string;
};

export const MappingDetails = () => {
  const { t } = useTranslation("client-scopes");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

  const { id, mapperId } = useParams<Params>();
  const { register, errors, setValue, control, handleSubmit } = useForm();
  const [mapping, setMapping] = useState<ProtocolMapperRepresentation>();
  const [typeOpen, setTypeOpen] = useState(false);
  const [configProperties, setConfigProperties] = useState<
    ConfigPropertyRepresentation[]
  >();

  const history = useHistory();
  const { realm } = useRealm();
  const serverInfo = useServerInfo();
  const isGuid = /^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$/;

  useFetch(
    async () => {
      if (mapperId.match(isGuid)) {
        const data = await adminClient.clientScopes.findProtocolMapper({
          id,
          mapperId,
        });
        if (data) {
          Object.entries(data).map((entry) => {
            convertToFormValues(entry[1], "config", setValue);
          });
        }
        const mapperTypes = serverInfo.protocolMapperTypes![data!.protocol!];
        const properties = mapperTypes.find(
          (type) => type.id === data!.protocolMapper
        )?.properties!;

        return {
          configProperties: properties,
          mapping: data,
        };
      } else {
        const scope = await adminClient.clientScopes.findOne({ id });
        const protocolMappers = serverInfo.protocolMapperTypes![
          scope.protocol!
        ];
        const mapping = protocolMappers.find(
          (mapper) => mapper.id === mapperId
        )!;
        return {
          mapping: {
            name: mapping.name,
            protocol: scope.protocol,
            protocolMapper: mapperId,
          },
        };
      }
    },
    (result) => {
      setConfigProperties(result.configProperties);
      setMapping(result.mapping);
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
        addAlert(
          t("common:mappingDeletedError", { error }),
          AlertVariant.danger
        );
      }
    },
  });

  const save = async (formMapping: ProtocolMapperRepresentation) => {
    const config = convertFormValuesToObject(formMapping.config);
    const map = { ...mapping, ...formMapping, config };
    const key = mapperId.match(isGuid) ? "Updated" : "Created";
    try {
      if (mapperId.match(isGuid)) {
        await adminClient.clientScopes.updateProtocolMapper(
          { id, mapperId },
          map
        );
      } else {
        await adminClient.clientScopes.addProtocolMapper({ id }, map);
      }
      addAlert(t(`common:mapping${key}Success`), AlertVariant.success);
    } catch (error) {
      addAlert(t(`common:mapping${key}Error`, { error }), AlertVariant.danger);
    }
  };

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={mapping ? mapping.name! : t("common:addMapper")}
        subKey={mapperId.match(isGuid) ? mapperId : ""}
        badge={mapping?.protocol}
        dropdownItems={
          mapperId.match(isGuid)
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
        >
          <>
            {!mapperId.match(isGuid) && (
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
            )}
          </>
          <FormGroup
            label={t("realmRolePrefix")}
            labelIcon={
              <HelpItem
                helpText="client-scopes-help:prefix"
                forLabel={t("realmRolePrefix")}
                forID="prefix"
              />
            }
            fieldId="prefix"
          >
            <TextInput
              ref={register()}
              type="text"
              id="prefix"
              name="config.usermodel-realmRoleMapping-rolePrefix"
            />
          </FormGroup>
          <FormGroup
            label={t("multiValued")}
            labelIcon={
              <HelpItem
                helpText="client-scopes-help:multiValued"
                forLabel={t("multiValued")}
                forID="multiValued"
              />
            }
            fieldId="multiValued"
          >
            <Controller
              name="config.multivalued"
              control={control}
              defaultValue="false"
              render={({ onChange, value }) => (
                <Switch
                  id="multiValued"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value === "true"}
                  onChange={(value) => onChange("" + value)}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("tokenClaimName")}
            labelIcon={
              <HelpItem
                helpText="client-scopes-help:tokenClaimName"
                forLabel={t("tokenClaimName")}
                forID="claimName"
              />
            }
            fieldId="claimName"
          >
            <TextInput
              ref={register()}
              type="text"
              id="claimName"
              name="config.claim-name"
            />
          </FormGroup>
          <FormGroup
            label={t("claimJsonType")}
            labelIcon={
              <HelpItem
                helpText="client-scopes-help:claimJsonType"
                forLabel={t("claimJsonType")}
                forID="claimJsonType"
              />
            }
            fieldId="claimJsonType"
          >
            <Controller
              name="config.jsonType-label"
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="claimJsonType"
                  onToggle={() => setTypeOpen(!typeOpen)}
                  onSelect={(_, value) => {
                    onChange(value as string);
                    setTypeOpen(false);
                  }}
                  selections={value}
                  variant={SelectVariant.single}
                  aria-label={t("claimJsonType")}
                  isOpen={typeOpen}
                >
                  {configProperties &&
                    configProperties
                      .find((property) => property.name! === "jsonType.label")
                      ?.options!.map((option) => (
                        <SelectOption
                          selected={option === value}
                          key={option}
                          value={option}
                        />
                      ))}
                </Select>
              )}
            />
          </FormGroup>
          <FormGroup
            hasNoPaddingTop
            label={t("addClaimTo")}
            fieldId="addClaimTo"
          >
            <Flex>
              <FlexItem>
                <Controller
                  name="config.id-token-claim"
                  defaultValue="false"
                  control={control}
                  render={({ onChange, value }) => (
                    <Checkbox
                      label={t("idToken")}
                      id="idToken"
                      isChecked={value === "true"}
                      onChange={(value) => onChange("" + value)}
                    />
                  )}
                />
              </FlexItem>
              <FlexItem>
                <Controller
                  name="config.access-token-claim"
                  defaultValue="false"
                  control={control}
                  render={({ onChange, value }) => (
                    <Checkbox
                      label={t("accessToken")}
                      id="accessToken"
                      isChecked={value === "true"}
                      onChange={(value) => onChange("" + value)}
                    />
                  )}
                />
              </FlexItem>
              <FlexItem>
                <Controller
                  name="config.userinfo-token-claim"
                  defaultValue="false"
                  control={control}
                  render={({ onChange, value }) => (
                    <Checkbox
                      label={t("userInfo")}
                      id="userInfo"
                      isChecked={value === "true"}
                      onChange={(value) => onChange("" + value)}
                    />
                  )}
                />
              </FlexItem>
            </Flex>
          </FormGroup>
          <ActionGroup>
            <Button variant="primary" type="submit">
              {t("common:save")}
            </Button>
            <Button variant="link">{t("common:cancel")}</Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
};
