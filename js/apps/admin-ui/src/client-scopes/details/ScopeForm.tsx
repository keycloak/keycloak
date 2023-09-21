import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import {
  ActionGroup,
  Button,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { HelpItem, TextControl } from "ui-shared";

import { getProtocolName } from "../../clients/utils";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import {
  ClientScopeDefaultOptionalType,
  allClientScopeTypes,
  clientScopeTypesSelectOptions,
} from "../../components/client-scope/ClientScopeTypes";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useLoginProviders } from "../../context/server-info/ServerInfoProvider";
import { convertAttributeNameToForm, convertToFormValues } from "../../util";
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";
import { toClientScopes } from "../routes/ClientScopes";

type ScopeFormProps = {
  clientScope?: ClientScopeRepresentation;
  save: (clientScope: ClientScopeDefaultOptionalType) => void;
};

export const ScopeForm = ({ clientScope, save }: ScopeFormProps) => {
  const { t } = useTranslation();
  const form = useForm<ClientScopeDefaultOptionalType>({ mode: "onChange" });
  const {
    register,
    control,
    handleSubmit,
    setValue,
    formState: { errors, isDirty, isValid },
  } = form;
  const { realm } = useRealm();

  const providers = useLoginProviders();
  const isFeatureEnabled = useIsFeatureEnabled();
  const isDynamicScopesEnabled = isFeatureEnabled(Feature.DynamicScopes);
  const [open, isOpen] = useState(false);
  const [openType, setOpenType] = useState(false);

  const displayOnConsentScreen: string = useWatch({
    control,
    name: convertAttributeNameToForm("attributes.display.on.consent.screen"),
    defaultValue:
      clientScope?.attributes?.["display.on.consent.screen"] ?? "true",
  });

  const dynamicScope = useWatch({
    control,
    name: convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
      "attributes.is.dynamic.scope",
    ),
    defaultValue: "false",
  });

  const setDynamicRegex = (value: string, append: boolean) =>
    setValue(
      convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
        "attributes.dynamic.scope.regexp",
      ),
      append ? `${value}:*` : value,
    );

  useEffect(() => {
    convertToFormValues(clientScope ?? {}, setValue);
  }, [clientScope]);

  return (
    <FormAccess
      role="manage-clients"
      onSubmit={handleSubmit(save)}
      isHorizontal
    >
      <FormGroup
        label={t("name")}
        labelIcon={
          <HelpItem helpText={t("scopeNameHelp")} fieldLabelId="name" />
        }
        fieldId="kc-name"
        validated={
          errors.name ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={t("required")}
        isRequired
      >
        <KeycloakTextInput
          id="kc-name"
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
          {...register("name", {
            required: true,
            onChange: (e) => {
              if (isDynamicScopesEnabled) {
                setDynamicRegex(e.target.value, true);
              }
            },
          })}
        />
      </FormGroup>
      {isDynamicScopesEnabled && (
        <FormProvider {...form}>
          <DefaultSwitchControl
            name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
              "attributes.is.dynamic.scope",
            )}
            label={t("dynamicScope")}
            labelIcon={t("dynamicScopeHelp")}
            onChange={(value) => {
              setDynamicRegex(value ? form.getValues("name") || "" : "", value);
            }}
            stringify
          />
          {dynamicScope === "true" && (
            <TextControl
              name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                "attributes.dynamic.scope.regexp",
              )}
              label={t("dynamicScopeFormat")}
              labelIcon={t("dynamicScopeFormatHelp")}
              isDisabled
            />
          )}
        </FormProvider>
      )}
      <FormGroup
        label={t("description")}
        labelIcon={
          <HelpItem
            helpText={t("scopeDescriptionHelp")}
            fieldLabelId="description"
          />
        }
        fieldId="kc-description"
        validated={
          errors.description ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={t("maxLength", { length: 255 })}
      >
        <KeycloakTextInput
          id="kc-description"
          validated={
            errors.description
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          {...register("description", {
            maxLength: 255,
          })}
        />
      </FormGroup>
      <FormGroup
        label={t("type")}
        labelIcon={
          <HelpItem helpText={t("scopeTypeHelp")} fieldLabelId="type" />
        }
        fieldId="kc-type"
      >
        <Controller
          name="type"
          defaultValue={allClientScopeTypes[0]}
          control={control}
          render={({ field }) => (
            <Select
              toggleId="kc-type"
              variant={SelectVariant.single}
              isOpen={openType}
              selections={field.value}
              onToggle={setOpenType}
              onSelect={(_, value) => {
                field.onChange(value);
                setOpenType(false);
              }}
            >
              {clientScopeTypesSelectOptions(t, allClientScopeTypes)}
            </Select>
          )}
        />
      </FormGroup>
      {!clientScope && (
        <FormGroup
          label={t("protocol")}
          labelIcon={
            <HelpItem helpText={t("protocolHelp")} fieldLabelId="protocol" />
          }
          fieldId="kc-protocol"
        >
          <Controller
            name="protocol"
            defaultValue={providers[0]}
            control={control}
            render={({ field }) => (
              <Select
                toggleId="kc-protocol"
                onToggle={isOpen}
                onSelect={(_, value) => {
                  field.onChange(value);
                  isOpen(false);
                }}
                selections={field.value}
                variant={SelectVariant.single}
                isOpen={open}
              >
                {providers.map((option) => (
                  <SelectOption
                    selected={option === field.value}
                    key={option}
                    value={option}
                    data-testid={`option-${option}`}
                  >
                    {getProtocolName(t, option)}
                  </SelectOption>
                ))}
              </Select>
            )}
          />
        </FormGroup>
      )}
      <FormGroup
        hasNoPaddingTop
        label={t("displayOnConsentScreen")}
        labelIcon={
          <HelpItem
            helpText={t("displayOnConsentScreenHelp")}
            fieldLabelId="displayOnConsentScreen"
          />
        }
        fieldId="kc-display-on-consent-screen"
      >
        <Controller
          name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
            "attributes.display.on.consent.screen",
          )}
          control={control}
          defaultValue={displayOnConsentScreen}
          render={({ field }) => (
            <Switch
              id="kc-display-on-consent-screen"
              label={t("on")}
              labelOff={t("off")}
              isChecked={field.value === "true"}
              onChange={(value) => field.onChange(value.toString())}
            />
          )}
        />
      </FormGroup>
      {displayOnConsentScreen === "true" && (
        <FormGroup
          label={t("consentScreenText")}
          labelIcon={
            <HelpItem
              helpText={t("consentScreenTextHelp")}
              fieldLabelId="consentScreenText"
            />
          }
          fieldId="kc-consent-screen-text"
        >
          <KeycloakTextArea
            id="kc-consent-screen-text"
            {...register(
              convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                "attributes.consent.screen.text",
              ),
            )}
          />
        </FormGroup>
      )}
      <FormGroup
        hasNoPaddingTop
        label={t("includeInTokenScope")}
        labelIcon={
          <HelpItem
            helpText={t("includeInTokenScopeHelp")}
            fieldLabelId="includeInTokenScope"
          />
        }
        fieldId="kc-include-in-token-scope"
      >
        <Controller
          name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
            "attributes.include.in.token.scope",
          )}
          control={control}
          defaultValue="true"
          render={({ field }) => (
            <Switch
              id="kc-include-in-token-scope"
              label={t("on")}
              labelOff={t("off")}
              isChecked={field.value === "true"}
              onChange={(value) => field.onChange(value.toString())}
            />
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("guiOrder")}
        labelIcon={
          <HelpItem helpText={t("guiOrderHelp")} fieldLabelId="guiOrder" />
        }
        fieldId="kc-gui-order"
      >
        <Controller
          name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
            "attributes.gui.order",
          )}
          defaultValue=""
          control={control}
          render={({ field }) => (
            <KeycloakTextInput
              id="kc-gui-order"
              type="number"
              value={field.value}
              min={0}
              onChange={field.onChange}
            />
          )}
        />
      </FormGroup>
      <ActionGroup>
        <Button
          variant="primary"
          type="submit"
          isDisabled={!isDirty || !isValid}
        >
          {t("save")}
        </Button>
        <Button
          variant="link"
          component={(props) => (
            <Link {...props} to={toClientScopes({ realm })}></Link>
          )}
        >
          {t("cancel")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
