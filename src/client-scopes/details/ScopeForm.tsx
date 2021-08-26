import React, { useEffect, useState } from "react";
import { useHistory, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Controller, useForm, useWatch } from "react-hook-form";
import {
  Form,
  FormGroup,
  ValidatedOptions,
  TextInput,
  Select,
  SelectVariant,
  SelectOption,
  Switch,
  ActionGroup,
  Button,
  TextArea,
  NumberInput,
} from "@patternfly/react-core";

import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import {
  clientScopeTypesSelectOptions,
  allClientScopeTypes,
  ClientScopeDefaultOptionalType,
} from "../../components/client-scope/ClientScopeTypes";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useLoginProviders } from "../../context/server-info/ServerInfoProvider";
import { convertToFormValues } from "../../util";
import { useRealm } from "../../context/realm-context/RealmContext";

type ScopeFormProps = {
  clientScope: ClientScopeRepresentation;
  save: (clientScope: ClientScopeDefaultOptionalType) => void;
};

export const ScopeForm = ({ clientScope, save }: ScopeFormProps) => {
  const { t } = useTranslation("client-scopes");
  const { register, control, handleSubmit, errors, setValue } =
    useForm<ClientScopeRepresentation>({
      defaultValues: { attributes: { "display-on-consent-screen": "true" } },
    });
  const history = useHistory();
  const { realm } = useRealm();

  const providers = useLoginProviders();
  const [open, isOpen] = useState(false);
  const [openType, setOpenType] = useState(false);
  const { id } = useParams<{ id: string }>();

  const displayOnConsentScreen = useWatch({
    control,
    name: "attributes.display-on-consent-screen",
  });

  useEffect(() => {
    Object.entries(clientScope).map((entry) => {
      if (entry[0] === "attributes") {
        convertToFormValues(entry[1], "attributes", setValue);
      }
      setValue(entry[0], entry[1]);
    });
  }, [clientScope]);

  return (
    <Form isHorizontal onSubmit={handleSubmit(save)} className="pf-u-mt-md">
      <FormGroup
        label={t("common:name")}
        labelIcon={
          <HelpItem
            id="name-help-icon"
            helpText="client-scopes-help:name"
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
            forLabel={t("common:description")}
            forID={t(`common:helpLabel`, { label: t("common:description") })}
          />
        }
        fieldId="kc-description"
        validated={
          errors.description ? ValidatedOptions.error : ValidatedOptions.default
        }
        helperTextInvalid={t("common:maxLength", { length: 255 })}
      >
        <TextInput
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
            forLabel={t("type")}
            forID={t(`common:helpLabel`, { label: t("type") })}
          />
        }
        fieldId="type"
      >
        <Controller
          name="type"
          defaultValue=""
          control={control}
          render={({ onChange, value }) => (
            <Select
              id="type"
              variant={SelectVariant.single}
              isOpen={openType}
              selections={value}
              onToggle={() => setOpenType(!openType)}
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
              forLabel="protocol"
              forID={t(`common:helpLabel`, { label: t("protocol") })}
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
                onToggle={() => isOpen(!open)}
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
                  />
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
            forLabel={t("displayOnConsentScreen")}
            forID={t(`common:helpLabel`, {
              label: t("displayOnConsentScreen"),
            })}
          />
        }
        fieldId="kc-display.on.consent.screen"
      >
        <Controller
          name="attributes.display-on-consent-screen"
          control={control}
          defaultValue="true"
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
              forLabel={t("consentScreenText")}
              forID={t(`common:helpLabel`, { label: t("consentScreenText") })}
            />
          }
          fieldId="kc-consent-screen-text"
        >
          <TextArea
            ref={register}
            type="text"
            id="kc-consent-screen-text"
            name="attributes.consent-screen-text"
          />
        </FormGroup>
      )}
      <FormGroup
        hasNoPaddingTop
        label={t("includeInTokenScope")}
        labelIcon={
          <HelpItem
            helpText="client-scopes-help:includeInTokenScope"
            forLabel={t("includeInTokenScope")}
            forID={t(`common:helpLabel`, { label: t("includeInTokenScope") })}
          />
        }
        fieldId="includeInTokenScope"
      >
        <Controller
          name="attributes.include-in-token-scope"
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
            forLabel={t("guiOrder")}
            forID={t(`common:helpLabel`, { label: t("guiOrder") })}
          />
        }
        fieldId="kc-gui-order"
      >
        <Controller
          name="attributes.gui-order"
          defaultValue={1}
          control={control}
          render={({ onChange, value }) => {
            const MIN_VALUE = 0;
            const setValue = (newValue: number) =>
              onChange(Math.max(newValue, MIN_VALUE));

            return (
              <NumberInput
                id="kc-gui-order"
                value={value}
                min={MIN_VALUE}
                onPlus={() => setValue(value + 1)}
                onMinus={() => setValue(value - 1)}
                onChange={(event) => {
                  const newValue = Number(event.currentTarget.value);
                  setValue(!isNaN(newValue) ? newValue : 0);
                }}
              />
            );
          }}
        />
      </FormGroup>
      <ActionGroup>
        <Button variant="primary" type="submit">
          {t("common:save")}
        </Button>
        <Button
          variant="link"
          onClick={() => history.push(`/${realm}/client-scopes`)}
        >
          {t("common:cancel")}
        </Button>
      </ActionGroup>
    </Form>
  );
};
