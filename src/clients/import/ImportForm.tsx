import {
  ActionGroup,
  AlertVariant,
  Button,
  FormGroup,
  PageSection,
  TextInput,
} from "@patternfly/react-core";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import React, { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useHistory } from "react-router-dom";
import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form-access/FormAccess";
import { JsonFileUpload } from "../../components/json-file-upload/JsonFileUpload";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import { CapabilityConfig } from "../add/CapabilityConfig";
import { ClientDescription } from "../ClientDescription";
import { toClient } from "../routes/Client";
import { toClients } from "../routes/Clients";

export const ImportForm = () => {
  const { t } = useTranslation("clients");
  const history = useHistory();
  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const form = useForm<ClientRepresentation>();
  const { register, handleSubmit, setValue } = form;
  const [imported, setImported] = useState<ClientRepresentation>({});

  const { addAlert, addError } = useAlerts();

  const handleFileChange = (obj: object) => {
    const defaultClient = {
      protocol: "",
      clientId: "",
      name: "",
      description: "",
    };

    Object.entries(obj || defaultClient).forEach((entries) => {
      if (entries[0] === "attributes") {
        convertToFormValues(entries[1], "attributes", form.setValue);
      } else {
        setValue(entries[0], entries[1]);
      }
    });
    setImported(obj || defaultClient);
  };

  const save = async (client: ClientRepresentation) => {
    try {
      const newClient = await adminClient.clients.create({
        ...imported,
        ...client,
        attributes: convertFormValuesToObject(client.attributes || {}),
      });
      addAlert(t("clientImportSuccess"), AlertVariant.success);
      history.push(
        toClient({ realm, clientId: newClient.id, tab: "settings" })
      );
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
            <JsonFileUpload id="realm-file" onChange={handleFileChange} />
            <ClientDescription />
            <FormGroup label={t("common:type")} fieldId="kc-type">
              <TextInput
                type="text"
                id="kc-type"
                name="protocol"
                isReadOnly
                ref={register()}
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
};
