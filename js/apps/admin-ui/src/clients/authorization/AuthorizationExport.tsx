import type ResourceServerRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  FormGroup,
  PageSection,
} from "@patternfly/react-core";
import { saveAs } from "file-saver";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { prettyPrintJSON } from "../../util";
import { useParams } from "../../utils/useParams";
import type { ClientParams } from "../routes/Client";

import "./authorization-details.css";

export const AuthorizationExport = () => {
  const { t } = useTranslation("clients");
  const { adminClient } = useAdminClient();
  const { clientId } = useParams<ClientParams>();
  const { addAlert, addError } = useAlerts();

  const [code, setCode] = useState<string>();
  const [authorizationDetails, setAuthorizationDetails] =
    useState<ResourceServerRepresentation>();

  useFetch(
    () =>
      adminClient.clients.exportResource({
        id: clientId,
      }),

    (authDetails) => {
      setCode(JSON.stringify(authDetails, null, 2));
      setAuthorizationDetails(authDetails);
    },
    []
  );

  const exportAuthDetails = () => {
    try {
      saveAs(
        new Blob([prettyPrintJSON(authorizationDetails)], {
          type: "application/json",
        }),
        "test-authz-config.json"
      );
      addAlert(t("exportAuthDetailsSuccess"), AlertVariant.success);
    } catch (error) {
      addError("exportAuthDetailsError", error);
    }
  };

  if (!code) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection>
      <FormAccess isHorizontal role="view-realm" className="pf-u-mt-lg">
        <FormGroup
          label={t("authDetails")}
          labelIcon={
            <HelpItem
              helpText={t("clients-help:authDetails")}
              fieldLabelId="clients:authDetails"
            />
          }
          fieldId="client"
        >
          <KeycloakTextArea
            id="authorizationDetails"
            readOnly
            resizeOrientation="vertical"
            value={code}
            aria-label={t("authDetails")}
          />
        </FormGroup>
        <ActionGroup>
          <Button
            data-testid="authorization-export-download"
            onClick={() => exportAuthDetails()}
          >
            {t("common:download")}
          </Button>
          <Button
            data-testid="authorization-export-copy"
            variant="secondary"
            onClick={async () => {
              try {
                await navigator.clipboard.writeText(code!);
                addAlert(t("copied"), AlertVariant.success);
              } catch (error) {
                addError(t("copyError"), error);
              }
            }}
          >
            {t("copy")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
};
