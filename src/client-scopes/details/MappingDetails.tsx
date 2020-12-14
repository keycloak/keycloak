import React, { useEffect, useState } from "react";
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
  Form,
  FormGroup,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { ConfigPropertyRepresentation } from "keycloak-admin/lib/defs/configPropertyRepresentation";
import ProtocolMapperRepresentation from "keycloak-admin/lib/defs/protocolMapperRepresentation";

import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAdminClient } from "../../context/auth/AdminClient";
import { Controller, useForm } from "react-hook-form";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useAlerts } from "../../components/alert/Alerts";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { convertFormValuesToObject, convertToFormValues } from "../../util";

type Params = {
  scopeId: string;
  id: string;
};

export const MappingDetails = () => {
  const { t } = useTranslation("client-scopes");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

  const { scopeId, id } = useParams<Params>();
  const { register, errors, setValue, control, handleSubmit } = useForm();
  const [mapping, setMapping] = useState<ProtocolMapperRepresentation>();
  const [typeOpen, setTypeOpen] = useState(false);
  const [configProperties, setConfigProperties] = useState<
    ConfigPropertyRepresentation[]
  >();

  const history = useHistory();
  const serverInfo = useServerInfo();
  const isGuid = /^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$/;

  useEffect(() => {
    if (id.match(isGuid)) {
      (async () => {
        const data = await adminClient.clientScopes.findProtocolMapper({
          id: scopeId,
          mapperId: id,
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
        setConfigProperties(properties);

        setMapping(data);
      })();
    } else {
      (async () => {
        const scope = await adminClient.clientScopes.findOne({ id: scopeId });
        setMapping({ protocol: scope.protocol, protocolMapper: id });
      })();
    }
  }, []);

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "client-scopes:deleteMappingTitle",
    messageKey: "client-scopes:deleteMappingConfirm",
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.clientScopes.delClientScopeMappings(
          { client: scopeId, id },
          []
        );
        addAlert(t("mappingDeletedSuccess"), AlertVariant.success);
        history.push(`/client-scopes/${scopeId}`);
      } catch (error) {
        addAlert(t("mappingDeletedError", { error }), AlertVariant.danger);
      }
    },
  });

  const save = async (formMapping: ProtocolMapperRepresentation) => {
    const config = convertFormValuesToObject(formMapping.config);
    const map = { ...mapping, ...formMapping, config };
    const key = id.match(isGuid) ? "Updated" : "Created";
    try {
      if (id.match(isGuid)) {
        await adminClient.clientScopes.updateProtocolMapper(
          { id: scopeId, mapperId: id },
          map
        );
      } else {
        await adminClient.clientScopes.addProtocolMapper({ id: scopeId }, map);
      }
      addAlert(t(`mapping${key}Success`), AlertVariant.success);
    } catch (error) {
      addAlert(t(`mapping${key}Error`, { error }), AlertVariant.danger);
    }
  };

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={mapping ? mapping.name! : t("addMapper")}
        subKey={id.match(isGuid) ? id : ""}
        badge={mapping?.protocol}
        dropdownItems={
          id.match(isGuid)
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
        <Form isHorizontal onSubmit={handleSubmit(save)}>
          {!id.match(isGuid) && (
            <FormGroup
              label={t("name")}
              labelIcon={
                <HelpItem
                  helpText="client-scopes-help:mapperName"
                  forLabel={t("name")}
                  forID="name"
                />
              }
              fieldId="name"
              isRequired
              validated={
                errors.name ? ValidatedOptions.error : ValidatedOptions.default
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
              name="config.usermodel_realmRoleMapping_rolePrefix"
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
              name="config.claim_name"
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
              name="config.jsonType_label"
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
                  name="config.id_token_claim"
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
                  name="config.access_token_claim"
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
                  name="config.userinfo_token_claim"
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
        </Form>
      </PageSection>
    </>
  );
};
