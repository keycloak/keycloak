import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import { ActionGroup, Button } from "@patternfly/react-core";
import { useEffect } from "react";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import {
  FormSubmitButton,
  SelectControl,
  TextAreaControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";

import { getProtocolName } from "../../clients/utils";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import {
  ClientScopeDefaultOptionalType,
  allClientScopeTypes,
} from "../../components/client-scope/ClientScopeTypes";
import { FormAccess } from "../../components/form/FormAccess";
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
  const { control, handleSubmit, setValue, formState } = form;
  const { isDirty, isValid } = formState;
  const { realm } = useRealm();

  const providers = useLoginProviders();
  const isFeatureEnabled = useIsFeatureEnabled();
  const isDynamicScopesEnabled = isFeatureEnabled(Feature.DynamicScopes);

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
      { shouldDirty: true }, // Mark the field as dirty when we modify the field
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
      <FormProvider {...form}>
        <TextControl
          name="name"
          label={t("name")}
          labelIcon={t("scopeNameHelp")}
          rules={{
            required: t("required"),
            onChange: (e) => {
              if (isDynamicScopesEnabled)
                setDynamicRegex(e.target.validated, true);
            },
          }}
        />
        {isDynamicScopesEnabled && (
          <>
            <DefaultSwitchControl
              name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
                "attributes.is.dynamic.scope",
              )}
              label={t("dynamicScope")}
              labelIcon={t("dynamicScopeHelp")}
              onChange={(event, value) => {
                setDynamicRegex(
                  value ? form.getValues("name") || "" : "",
                  value,
                );
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
          </>
        )}
        <TextAreaControl
          name="description"
          label={t("description")}
          labelIcon={t("scopeDescriptionHelp")}
          rules={{
            maxLength: {
              value: 255,
              message: t("maxLength"),
            },
          }}
        />
        <SelectControl
          id="kc-type"
          name="type"
          label={t("type")}
          labelIcon={t("scopeTypeHelp")}
          controller={{ defaultValue: allClientScopeTypes[0] }}
          options={allClientScopeTypes.map((key) => ({
            key,
            value: t(`clientScopeType.${key}`),
          }))}
        />
        {!clientScope && (
          <SelectControl
            id="kc-protocol"
            name="protocol"
            label={t("protocol")}
            labelIcon={t("protocolHelp")}
            controller={{ defaultValue: providers[0] }}
            options={providers.map((option) => ({
              key: option,
              value: getProtocolName(t, option),
            }))}
          />
        )}
        <DefaultSwitchControl
          name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
            "attributes.display.on.consent.screen",
          )}
          defaultValue={displayOnConsentScreen}
          label={t("displayOnConsentScreen")}
          labelIcon={t("displayOnConsentScreenHelp")}
          stringify
        />
        {displayOnConsentScreen === "true" && (
          <TextAreaControl
            name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
              "attributes.consent.screen.text",
            )}
            label={t("consentScreenText")}
            labelIcon={t("consentScreenTextHelp")}
          />
        )}
        <DefaultSwitchControl
          name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
            "attributes.include.in.token.scope",
          )}
          label={t("includeInTokenScope")}
          labelIcon={t("includeInTokenScopeHelp")}
          stringify
        />
        <TextControl
          name={convertAttributeNameToForm<ClientScopeDefaultOptionalType>(
            "attributes.gui.order",
          )}
          label={t("guiOrder")}
          labelIcon={t("guiOrderHelp")}
          type="number"
          min={0}
        />
        <ActionGroup>
          <FormSubmitButton
            data-testid="save"
            formState={formState}
            disabled={!isDirty || !isValid}
          >
            {t("save")}
          </FormSubmitButton>
          <Button
            variant="link"
            component={(props) => (
              <Link {...props} to={toClientScopes({ realm })}></Link>
            )}
          >
            {t("cancel")}
          </Button>
        </ActionGroup>
      </FormProvider>
    </FormAccess>
  );
};
