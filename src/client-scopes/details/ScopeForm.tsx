import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Controller, useForm, useWatch } from "react-hook-form";
import {
  FormGroup,
  ValidatedOptions,
  Select,
  SelectVariant,
  SelectOption,
  Switch,
  ActionGroup,
  Button,
} from "@patternfly/react-core";

import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import {
  clientScopeTypesSelectOptions,
  allClientScopeTypes,
  ClientScopeDefaultOptionalType,
} from "../../components/client-scope/ClientScopeTypes";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useLoginProviders } from "../../context/server-info/ServerInfoProvider";
import { convertAttributeNameToForm, convertToFormValues } from "../../util";
import { useRealm } from "../../context/realm-context/RealmContext";
import { getProtocolName } from "../../clients/utils";
import { toClientScopes } from "../routes/ClientScopes";
import { FormAccess } from "../../components/form-access/FormAccess";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";

type ScopeFormProps = {
  clientScope: ClientScopeRepresentation;
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
    formState: { errors },
  } = useForm<ClientScopeRepresentation>({
    defaultValues: { attributes: { "display.on.consent.screen": "true" } },
  });
  const { realm } = useRealm();

  const providers = useLoginProviders();
  const [open, isOpen] = useState(false);
  const [openType, setOpenType] = useState(false);
  const { id } = useParams<{ id: string }>();

  const displayOnConsentScreen = useWatch({
    control,
    name: "attributes.display.on.consent.screen",
    defaultValue:
      clientScope.attributes?.["display.on.consent.screen"] ?? "true",
  });

  useEffect(() => {
    convertToFormValues(clientScope, setValue);
  }, [clientScope]);

  return (
    <FormAccess
      isHorizontal
      onSubmit={handleSubmit(save)}
      role="manage-clients"
    >
      <FormGroup
        label={t("common:name")}
        labelIcon={
          <HelpItem helpText="client-scopes-help:name" fieldLabelId="name" />
        }
        fieldId="kc-name"
        isRequired
        validated={
          errors.name ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={t("common:required")}
      >
        <KeycloakTextInput
          ref={register({
            required: true,
            validate: (value: string) =>
              !!value.trim() || t("common:required").toString(),
          })}
          type="text"
          id="kc-name"
          name="name"
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
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
          ref={register({
            maxLength: 255,
          })}
          validated={
            errors.description
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          type="text"
          id="kc-description"
          name="description"
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
        fieldId="type"
      >
        <Controller
          name="type"
          defaultValue={allClientScopeTypes[0]}
          control={control}
          render={({ onChange, value }) => (
            <Select
              id="type"
              variant={SelectVariant.single}
              isOpen={openType}
              selections={value}
              onToggle={setOpenType}
              onSelect={(_, value) => {
                onChange(value);
                setOpenType(false);
              }}
            >
              {clientScopeTypesSelectOptions(t, allClientScopeTypes)}
            </Select>
          )}
        />
      </FormGroup>
      {!id && (
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
            render={({ onChange, value }) => (
              <Select
                toggleId="kc-protocol"
                required
                onToggle={isOpen}
                onSelect={(_, value) => {
                  onChange(value as string);
                  isOpen(false);
                }}
                selections={value}
                variant={SelectVariant.single}
                aria-label={t("selectEncryptionType")}
                isOpen={open}
              >
                {providers.map((option) => (
                  <SelectOption
                    selected={option === value}
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
        fieldId="kc-display.on.consent.screen"
      >
        <Controller
          name={convertAttributeNameToForm(
            "attributes.display.on.consent.screen"
          )}
          control={control}
          defaultValue={displayOnConsentScreen}
          render={({ onChange, value }) => (
            <Switch
              id="kc-display.on.consent.screen-switch"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={value === "true"}
              onChange={(value) => onChange("" + value)}
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
            ref={register}
            type="text"
            id="kc-consent-screen-text"
            name={convertAttributeNameToForm("attributes.consent.screen.text")}
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
        fieldId="includeInTokenScope"
      >
        <Controller
          name={convertAttributeNameToForm("attributes.include.in.token.scope")}
          control={control}
          defaultValue="true"
          render={({ onChange, value }) => (
            <Switch
              id="includeInTokenScope-switch"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={value === "true"}
              onChange={(value) => onChange("" + value)}
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
          name={convertAttributeNameToForm("attributes.gui.order")}
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <KeycloakTextInput
              id="kc-gui-order"
              type="number"
              value={value}
              data-testid="displayOrder"
              min={0}
              onChange={onChange}
            />
          )}
        />
      </FormGroup>
      <ActionGroup>
        <Button variant="primary" type="submit">
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
