import { fetchWithError } from "@keycloak/keycloak-admin-client";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  PageSection,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { FormSubmitButton, TextControl } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
import { FileUploadForm } from "../../components/json-file-upload/FileUploadForm";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import {
  addTrailingSlash,
  convertFormValuesToObject,
  convertToFormValues,
} from "../../util";
import { getAuthorizationHeaders } from "../../utils/getAuthorizationHeaders";
import { ClientDescription } from "../ClientDescription";
import { FormFields } from "../ClientDetails";
import { CapabilityConfig } from "../add/CapabilityConfig";
import { toClient } from "../routes/Client";
import { toClients } from "../routes/Clients";

const isXml = (text: string) => text.match(/(<.[^(><.)]+>)/g);

export default function ImportForm() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const navigate = useNavigate();
  const { realm } = useRealm();
  const form = useForm<FormFields>();
  const { handleSubmit, setValue, formState } = form;
  const [imported, setImported] = useState<ClientRepresentation>({});

  const { addAlert, addError } = useAlerts();

  const handleFileChange = async (contents: string) => {
    try {
      const parsed = await parseFileContents(contents);

      convertToFormValues(parsed, setValue);
      setImported(parsed);
    } catch (error) {
      addError("importParseError", error);
    }
  };

  async function parseFileContents(
    contents: string,
  ): Promise<ClientRepresentation> {
    if (!isXml(contents)) {
      return JSON.parse(contents);
    }

    const response = await fetchWithError(
      `${addTrailingSlash(
        adminClient.baseUrl,
      )}admin/realms/${realm}/client-description-converter`,
      {
        method: "POST",
        body: contents,
        headers: getAuthorizationHeaders(await adminClient.getAccessToken()),
      },
    );

    if (!response.ok) {
      throw new Error(
        `Server responded with invalid status: ${response.statusText}`,
      );
    }

    return response.json();
  }

  const save = async (client: ClientRepresentation) => {
    try {
      const newClient = await adminClient.clients.create({
        ...imported,
        ...convertFormValuesToObject({
          ...client,
          attributes: client.attributes || {},
        }),
      });
      addAlert(t("clientImportSuccess"), AlertVariant.success);
      navigate(toClient({ realm, clientId: newClient.id, tab: "settings" }));
    } catch (error) {
      addError("clientImportError", error);
    }
  };

  return (
    <>
      <ViewHeader titleKey="importClient" subKey="clientsExplain" />
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          onSubmit={handleSubmit(save)}
          role="manage-clients"
        >
          <FormProvider {...form}>
            <FileUploadForm
              id="realm-file"
              language="json"
              extension=".json,.xml"
              helpText={t("helpFileUploadClient")}
              onChange={handleFileChange}
            />
            <ClientDescription hasConfigureAccess />
            <TextControl name="protocol" label={t("type")} readOnly />
            <CapabilityConfig unWrap={true} />
            <ActionGroup>
              <FormSubmitButton
                formState={formState}
                allowInvalid
                allowNonDirty
              >
                {t("save")}
              </FormSubmitButton>
              <Button
                variant="link"
                component={(props) => (
                  <Link {...props} to={toClients({ realm })} />
                )}
              >
                {t("cancel")}
              </Button>
            </ActionGroup>
          </FormProvider>
        </FormAccess>
      </PageSection>
    </>
  );
}
