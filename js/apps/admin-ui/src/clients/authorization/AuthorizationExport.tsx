import type ResourceServerRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  PageSection,
} from "@patternfly/react-core";
import { saveAs } from "file-saver";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { TextAreaControl } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { prettyPrintJSON } from "../../util";
import { useFetch } from "../../utils/useFetch";
import { useParams } from "../../utils/useParams";
import type { ClientParams } from "../routes/Client";

export const AuthorizationExport = () => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
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
    [],
  );

  const exportAuthDetails = () => {
    try {
      saveAs(
        new Blob([prettyPrintJSON(authorizationDetails)], {
          type: "application/json",
        }),
        "test-authz-config.json",
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
      <FormAccess
        isHorizontal
        role="manage-authorization"
        className="pf-v5-u-mt-lg"
      >
        <TextAreaControl
          name="authDetails"
          label={t("authDetails")}
          labelIcon={t("authDetailsHelp")}
          resizeOrientation="vertical"
          defaultValue={code!}
          readOnly
          rows={10}
        />
        <ActionGroup>
          <Button
            data-testid="authorization-export-download"
            onClick={() => exportAuthDetails()}
          >
            {t("download")}
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
