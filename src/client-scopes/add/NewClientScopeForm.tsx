import React, { useState } from "react";
import { useHistory } from "react-router-dom";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Form,
  FormGroup,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";

import { ClientScopeRepresentation } from "../models/client-scope";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useAlerts } from "../../components/alert/Alerts";
import { useAdminClient } from "../../context/auth/AdminClient";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useLoginProviders } from "../../context/server-info/ServerInfoProvider";

export const NewClientScopeForm = () => {
  const { t } = useTranslation("client-scopes");
  const helpText = useTranslation("client-scopes-help").t;
  const { register, control, handleSubmit, errors } = useForm<
    ClientScopeRepresentation
  >();
  const history = useHistory();

  const adminClient = useAdminClient();
  const providers = useLoginProviders();

  const [open, isOpen] = useState(false);
  const { addAlert } = useAlerts();

  const save = async (clientScopes: ClientScopeRepresentation) => {
    try {
      const keyValues = Object.keys(clientScopes.attributes!).map((key) => {
        const newKey = key.replace(/_/g, ".");
        return { [newKey]: clientScopes.attributes![key] };
      });
      clientScopes.attributes = Object.assign({}, ...keyValues);

      await adminClient.clientScopes.create({ ...clientScopes });
      addAlert(t("createClientScopeSuccess"), AlertVariant.success);
    } catch (error) {
      addAlert(
        `${t("createClientScopeError")} '${error}'`,
        AlertVariant.danger
      );
    }
  };

  return (
    <>
      <ViewHeader
        titleKey="client-scopes:createClientScope"
        subKey="client-scopes:clientScopeExplain"
      />

      <PageSection variant="light">
        <Form isHorizontal onSubmit={handleSubmit(save)}>
          <FormGroup
            label={t("name")}
            labelIcon={
              <HelpItem
                helpText={helpText("name")}
                forLabel={t("name")}
                forID="kc-name"
              />
            }
            fieldId="kc-name"
            isRequired
            validated={errors.name ? "error" : "default"}
            helperTextInvalid={t("common:required")}
          >
            <TextInput
              ref={register({ required: true })}
              type="text"
              id="kc-name"
              name="name"
            />
          </FormGroup>
          <FormGroup
            label={t("description")}
            labelIcon={
              <HelpItem
                helpText={helpText("description")}
                forLabel={t("description")}
                forID="kc-description"
              />
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
            label={t("protocol")}
            labelIcon={
              <HelpItem
                helpText={helpText("protocol")}
                forLabel="protocol"
                forID="kc-protocol"
              />
            }
            fieldId="kc-protocol"
          >
            <Controller
              name="protocol"
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="kc-protocol"
                  required
                  onToggle={() => isOpen(!open)}
                  onSelect={(_, value, isPlaceholder) => {
                    onChange(isPlaceholder ? "" : (value as string));
                    isOpen(false);
                  }}
                  selections={value}
                  variant={SelectVariant.single}
                  aria-label={t("selectEncryptionType")}
                  placeholderText={t("common:selectOne")}
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
          <FormGroup
            hasNoPaddingTop
            label={t("displayOnConsentScreen")}
            labelIcon={
              <HelpItem
                helpText={helpText("displayOnConsentScreen")}
                forLabel={t("displayOnConsentScreen")}
                forID="kc-display.on.consent.screen"
              />
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
            label={t("consentScreenText")}
            labelIcon={
              <HelpItem
                helpText={helpText("consentScreenText")}
                forLabel={t("consentScreenText")}
                forID="kc-consent-screen-text"
              />
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
            label={t("guiOrder")}
            labelIcon={
              <HelpItem
                helpText={helpText("guiOrder")}
                forLabel={t("guiOrder")}
                forID="kc-gui-order"
              />
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
            <Button variant="link" onClick={() => history.push("..")}>
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </Form>
      </PageSection>
    </>
  );
};
