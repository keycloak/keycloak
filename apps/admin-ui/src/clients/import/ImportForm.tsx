import { Language } from "@patternfly/react-code-editor";
import {
  ActionGroup,
  AlertVariant,
  Button,
  FormGroup,
  PageSection,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom-v5-compat";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";

import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form-access/FormAccess";
import { FileUploadForm } from "../../components/json-file-upload/FileUploadForm";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import {
  addTrailingSlash,
  convertFormValuesToObject,
  convertToFormValues,
} from "../../util";
import { getAuthorizationHeaders } from "../../utils/getAuthorizationHeaders";
import { CapabilityConfig } from "../add/CapabilityConfig";
import { ClientDescription } from "../ClientDescription";
import { FormFields } from "../ClientDetails";
import { toClient } from "../routes/Client";
import { toClients } from "../routes/Clients";

const isXml = (text: string) => text.startsWith("<");

export default function ImportForm() {
  const { t } = useTranslation("clients");
  const navigate = useNavigate();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const form = useForm<FormFields>({ shouldUnregister: false });
  const { register, handleSubmit, setValue } = form;
  const [imported, setImported] = useState<ClientRepresentation>({});

  const { addAlert, addError } = useAlerts();

  const handleFileChange = async (contents: string) => {
    try {
      const parsed = await parseFileContents(contents);

      convertToFormValues(parsed, setValue);
      setImported(parsed);
    } catch (error) {
      addError("clients:importParseError", error);
    }
  };

  async function parseFileContents(
    contents: string
  ): Promise<ClientRepresentation> {
    if (!isXml(contents)) {
      return JSON.parse(contents);
    }

    const response = await fetch(
      `${addTrailingSlash(
        adminClient.baseUrl
      )}admin/realms/${realm}/client-description-converter`,
      {
        method: "POST",
        body: contents,
        headers: getAuthorizationHeaders(await adminClient.getAccessToken()),
      }
    );

    if (!response.ok) {
      throw new Error(
        `Server responded with invalid status: ${response.statusText}`
      );
    }

    return response.json();
  }

  const save = async (client: ClientRepresentation) => {
    try {
      const newClient = await adminClient.clients.create({
        ...imported,
        ...convertFormValuesToObject(client),
      });
      addAlert(t("clientImportSuccess"), AlertVariant.success);
      navigate(toClient({ realm, clientId: newClient.id, tab: "settings" }));
    } catch (error) {
      addError("clients:clientImportError", error);
    }
  };

  return (
    <>
      <ViewHeader
        titleKey="clients:importClient"
        subKey="clients:clientsExplain"
      />
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          onSubmit={handleSubmit(save)}
          role="manage-clients"
        >
          <FormProvider {...form}>
            <FileUploadForm
              id="realm-file"
              language={Language.json}
              extension=".json,.xml"
              helpText="common-help:helpFileUploadClient"
              onChange={handleFileChange}
            />
            <ClientDescription hasConfigureAccess />
            <FormGroup label={t("common:type")} fieldId="kc-type">
              <KeycloakTextInput
                id="kc-type"
                isReadOnly
                {...register("protocol")}
              />
            </FormGroup>
            <CapabilityConfig unWrap={true} />
            <ActionGroup>
              <Button variant="primary" type="submit">
                {t("common:save")}
              </Button>
              <Button
                variant="link"
                component={(props) => (
                  <Link {...props} to={toClients({ realm })} />
                )}
              >
                {t("common:cancel")}
              </Button>
            </ActionGroup>
          </FormProvider>
        </FormAccess>
      </PageSection>
    </>
  );
}
