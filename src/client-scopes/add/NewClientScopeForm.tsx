import React, { useContext, useState } from "react";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Form,
  FormGroup,
  PageSection,
  Select,
  SelectVariant,
  Switch,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";

import { ClientScopeRepresentation } from "../models/client-scope";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { HttpClientContext } from "../../http-service/HttpClientContext";
import { RealmContext } from "../../components/realm-context/RealmContext";
import { useAlerts } from "../../components/alert/Alerts";

export const NewClientScopeForm = () => {
  const { t } = useTranslation("client-scopes");
  const { register, control, handleSubmit } = useForm<
    ClientScopeRepresentation
  >();

  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);

  const [open, isOpen] = useState(false);
  const [add, Alerts] = useAlerts();

  const save = async (clientScopes: ClientScopeRepresentation) => {
    try {
      const keyValues = Object.keys(clientScopes.attributes!).map((key) => {
        const newKey = key.replace(/_/g, ".");
        return { [newKey]: clientScopes.attributes![key] };
      });
      clientScopes.attributes = Object.assign({}, ...keyValues);

      await httpClient.doPost(
        `/admin/realms/${realm}/client-scopes`,
        clientScopes
      );
      add(t("createClientScopeSuccess"), AlertVariant.success);
    } catch (error) {
      add(`${t("createClientScopeError")} '${error}'`, AlertVariant.danger);
    }
  };

  return (
    <PageSection variant="light">
      <Alerts />
      <Form isHorizontal onSubmit={handleSubmit(save)}>
        <FormGroup
          label={
            <>
              {t("name")} <HelpItem item="clientScope.name" />
            </>
          }
          fieldId="kc-name"
          isRequired
        >
          <TextInput
            ref={register({ required: true })}
            type="text"
            id="kc-name"
            name="name"
          />
        </FormGroup>
        <FormGroup
          label={
            <>
              {t("description")} <HelpItem item="clientScope.description" />
            </>
          }
          fieldId="kc-description"
        >
          <TextInput
            ref={register}
            type="text"
            id="kc-description"
            name="description"
          />
        </FormGroup>
        <FormGroup
          label={
            <>
              {t("protocol")} <HelpItem item="clientScope.protocol" />
            </>
          }
          fieldId="kc-protocol"
        >
          <Controller
            name="protocol"
            defaultValue=""
            control={control}
            render={({ onChange, value }) => (
              <Select
                id="kc-protocol"
                required
                onToggle={() => isOpen(!open)}
                onSelect={(_, value, isPlaceholder) => {
                  onChange(isPlaceholder ? "" : (value as string));
                  isOpen(false);
                }}
                selections={value}
                variant={SelectVariant.single}
                aria-label="Select Encryption type"
                isOpen={open}
              ></Select>
            )}
          />
        </FormGroup>
        <FormGroup
          label={
            <>
              {t("displayOnConsentScreen")}{" "}
              <HelpItem item="clientScope.displayOnConsentScreen" />
            </>
          }
          fieldId="kc-display.on.consent.screen"
        >
          <Controller
            name="attributes.display_on_consent_screen"
            control={control}
            defaultValue={false}
            render={({ onChange, value }) => (
              <Switch
                id="kc-display.on.consent.screen"
                label={t("common:on")}
                labelOff={t("common:off")}
                isChecked={value}
                onChange={onChange}
              />
            )}
          />
        </FormGroup>
        <FormGroup
          label={
            <>
              {t("consentScreenText")}{" "}
              <HelpItem item="clientScope.consentScreenText" />
            </>
          }
          fieldId="kc-consent-screen-text"
        >
          <TextInput
            ref={register}
            type="text"
            id="kc-consent-screen-text"
            name="attributes.consent_screen_text"
          />
        </FormGroup>
        <FormGroup
          label={
            <>
              {t("guiOrder")} <HelpItem item="clientScope.guiOrder" />
            </>
          }
          fieldId="kc-gui-order"
        >
          <TextInput
            ref={register}
            type="number"
            id="kc-gui-order"
            name="attributes.gui_order"
          />
        </FormGroup>
        <ActionGroup>
          <Button variant="primary" type="submit">
            {t("common:save")}
          </Button>
          <Button variant="link">{t("common:cancel")}</Button>
        </ActionGroup>
      </Form>
    </PageSection>
  );
};
