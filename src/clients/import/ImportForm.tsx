import React from "react";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  PageSection,
  FormGroup,
  TextInput,
  ActionGroup,
  Button,
  AlertVariant,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";

import { ClientDescription } from "../ClientDescription";
import { JsonFileUpload } from "../../components/json-file-upload/JsonFileUpload";
import { useAlerts } from "../../components/alert/Alerts";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import type ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";
import { useAdminClient } from "../../context/auth/AdminClient";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import { CapabilityConfig } from "../add/CapabilityConfig";

export const ImportForm = () => {
  const { t } = useTranslation("clients");
  const history = useHistory();
  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const form = useForm<ClientRepresentation>();
  const { register, handleSubmit, setValue } = form;

  const { addAlert } = useAlerts();

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
  };

  const save = async (client: ClientRepresentation) => {
    try {
      const newClient = await adminClient.clients.create({
        ...client,
        attributes: convertFormValuesToObject(client.attributes || {}),
      });
      addAlert(t("clientImportSuccess"), AlertVariant.success);
      history.push(`/${realm}/clients/${newClient.id}`);
    } catch (error) {
      addAlert(t("clientImportError", { error }), AlertVariant.danger);
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
                onClick={() => history.push(`/${realm}/clients`)}
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
