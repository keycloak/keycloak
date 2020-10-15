import React, { useContext, useEffect, useState } from "react";
import { useHistory, useParams } from "react-router-dom";
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
import { HttpClientContext } from "../../context/http-service/HttpClientContext";
import { RealmContext } from "../../context/realm-context/RealmContext";
import { useAlerts } from "../../components/alert/Alerts";
import { useLoginProviders } from "../../context/server-info/ServerInfoProvider";
import { ViewHeader } from "../../components/view-header/ViewHeader";

export const ClientScopeForm = () => {
  const { t } = useTranslation("client-scopes");
  const helpText = useTranslation("client-scopes-help").t;
  const { register, control, handleSubmit, errors, setValue } = useForm<
    ClientScopeRepresentation
  >();
  const history = useHistory();

  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);
  const providers = useLoginProviders();
  const { id } = useParams<{ id: string }>();

  const [open, isOpen] = useState(false);
  const { addAlert } = useAlerts();

  useEffect(() => {
    (async () => {
      if (id) {
        const response = await httpClient.doGet<ClientScopeRepresentation>(
          `/admin/realms/${realm}/client-scopes/${id}`
        );
        if (response.data) {
          Object.entries(response.data).map((entry) => {
            if (entry[0] === "attributes") {
              Object.keys(entry[1]).map((key) => {
                const newKey = key.replace(/\./g, "_");
                setValue("attributes." + newKey, entry[1][key]);
              });
            }
            setValue(entry[0], entry[1]);
          });
        }
      }
    })();
  }, []);

  const save = async (clientScopes: ClientScopeRepresentation) => {
    try {
      const keyValues = Object.keys(clientScopes.attributes!).map((key) => {
        const newKey = key.replace(/_/g, ".");
        return { [newKey]: clientScopes.attributes![key] };
      });
      clientScopes.attributes = Object.assign({}, ...keyValues);

      const url = `/admin/realms/${realm}/client-scopes/`;
      if (id) {
        await httpClient.doPut(url + id, clientScopes);
      } else {
        await httpClient.doPost(url, clientScopes);
      }
      addAlert(t((id ? "update" : "create") + "Success"), AlertVariant.success);
    } catch (error) {
      addAlert(
        t((id ? "update" : "create") + "Error", { error }),
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
            hasNoPaddingTop
            label={t("includeInTokenScope")}
            labelIcon={
              <HelpItem
                helpText={helpText("includeInTokenScope")}
                forLabel={t("includeInTokenScope")}
                forID="includeInTokenScope"
              />
            }
            fieldId="includeInTokenScope"
          >
            <Controller
              name="attributes.include_in_token_scope"
              control={control}
              defaultValue="false"
              render={({ onChange, value }) => (
                <Switch
                  id="includeInTokenScope"
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
