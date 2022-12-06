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
import { Controller, useForm, useWatch } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom-v5-compat";

import { getProtocolName } from "../../clients/utils";
import {
  allClientScopeTypes,
  ClientScopeDefaultOptionalType,
  clientScopeTypesSelectOptions,
} from "../../components/client-scope/ClientScopeTypes";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useLoginProviders } from "../../context/server-info/ServerInfoProvider";
import { convertAttributeNameToForm, convertToFormValues } from "../../util";
import { toClientScopes } from "../routes/ClientScopes";

type ScopeFormProps = {
  clientScope?: ClientScopeRepresentation;
  save: (clientScope: ClientScopeDefaultOptionalType) => void;
};

export const ScopeForm = ({ clientScope, save }: ScopeFormProps) => {
  const { t } = useTranslation("client-scopes");
  const { t: tc } = useTranslation("clients");
  const {
    register,
    control,
    handleSubmit,
    setValue,
    formState: { errors, isDirty, isValid },
  } = useForm<ClientScopeDefaultOptionalType>({ mode: "onChange" });
  const { realm } = useRealm();

  const providers = useLoginProviders();
  const [open, isOpen] = useState(false);
  const [openType, setOpenType] = useState(false);

  const displayOnConsentScreen: string = useWatch({
    control,
    name: convertAttributeNameToForm("attributes.display.on.consent.screen"),
    defaultValue:
      clientScope?.attributes?.["display.on.consent.screen"] ?? "true",
  });

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
        label={t("common:name")}
        labelIcon={
          <HelpItem helpText="client-scopes-help:name" fieldLabelId="name" />
        }
        fieldId="kc-name"
        validated={
          errors.name ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={t("common:required")}
        isRequired
      >
        <KeycloakTextInput
          id="kc-name"
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
          {...register("name", { required: true })}
        />
      </FormGroup>
      <FormGroup
        label={t("common:description")}
        labelIcon={
          <HelpItem
            helpText="client-scopes-help:description"
            fieldLabelId="description"
          />
        }
        fieldId="kc-description"
        validated={
          errors.description ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={t("common:maxLength", { length: 255 })}
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
          <HelpItem
            helpText="client-scopes-help:type"
            fieldLabelId="client-scopes:type"
          />
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
            <HelpItem
              helpText="client-scopes-help:protocol"
              fieldLabelId="client-scopes:protocol"
            />
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
                    {getProtocolName(tc, option)}
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
            helpText="client-scopes-help:displayOnConsentScreen"
            fieldLabelId="client-scopes:displayOnConsentScreen"
          />
        }
        fieldId="kc-display-on-consent-screen"
      >
        <Controller
          name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
            "attributes.display.on.consent.screen"
          )}
          control={control}
          defaultValue={displayOnConsentScreen}
          render={({ field }) => (
            <Switch
              id="kc-display-on-consent-screen"
              label={t("common:on")}
              labelOff={t("common:off")}
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
              helpText="client-scopes-help:consentScreenText"
              fieldLabelId="client-scopes:consentScreenText"
            />
          }
          fieldId="kc-consent-screen-text"
        >
          <KeycloakTextArea
            id="kc-consent-screen-text"
            {...register(
              convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                "attributes.consent.screen.text"
              )
            )}
          />
        </FormGroup>
      )}
      <FormGroup
        hasNoPaddingTop
        label={t("includeInTokenScope")}
        labelIcon={
          <HelpItem
            helpText="client-scopes-help:includeInTokenScope"
            fieldLabelId="client-scopes:includeInTokenScope"
          />
        }
        fieldId="kc-include-in-token-scope"
      >
        <Controller
          name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
            "attributes.include.in.token.scope"
          )}
          control={control}
          defaultValue="true"
          render={({ field }) => (
            <Switch
              id="kc-include-in-token-scope"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={field.value === "true"}
              onChange={(value) => field.onChange(value.toString())}
            />
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("guiOrder")}
        labelIcon={
          <HelpItem
            helpText="client-scopes-help:guiOrder"
            fieldLabelId="client-scopes:guiOrder"
          />
        }
        fieldId="kc-gui-order"
      >
        <Controller
          name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
            "attributes.gui.order"
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
          {t("common:save")}
        </Button>
        <Button
          variant="link"
          component={(props) => (
            <Link {...props} to={toClientScopes({ realm })}></Link>
          )}
        >
          {t("common:cancel")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
