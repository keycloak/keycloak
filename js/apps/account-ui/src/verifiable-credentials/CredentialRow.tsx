import {
  ContinueCancelModal,
  useEnvironment,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  DataListAction,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Flex,
  FlexItem,
} from "@patternfly/react-core";
import { ExternalLinkAltIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";

import { deleteVerifiableCredential } from "../api/methods";
import { UserVerifiableCredentialRepresentation } from "../api/representations";
import { formatDate, FORMAT_DATE_ONLY } from "../utils/formatDate";
import { useAccountAlerts } from "../utils/useAccountAlerts";

type CredentialRowProps = {
  credential: UserVerifiableCredentialRepresentation;
  refresh: () => void;
};

export const CredentialRow = ({ credential, refresh }: CredentialRowProps) => {
  const { t } = useTranslation();
  const context = useEnvironment();
  const { addAlert, addError } = useAccountAlerts();

  const hasManageRole = () => {
    const token = context.keycloak.tokenParsed;
    const accountRoles = token?.resource_access?.["account"]?.roles || [];
    return (
      accountRoles.includes("manage-account") ||
      accountRoles.includes("manage-verifiable-credentials")
    );
  };

  const handleDelete = async () => {
    try {
      await deleteVerifiableCredential(
        context,
        credential.credentialScopeName!,
      );
      addAlert(t("credentialDeletedSuccess"));
      refresh();
    } catch (error) {
      addError("credentialDeleteError", error);
    }
  };

  const handleIssueToWallet = async () => {
    try {
      // Construct the AIA action parameter
      const config = {
        credential_configuration_id: credential.credentialScopeName,
        pre_authorized: false,
      };

      // Base64 encode the config
      const encodedConfig = btoa(JSON.stringify(config));

      // Trigger AIA flow using keycloak.login()
      await context.keycloak.login({
        action: `verifiable_credential_offer:${encodedConfig}`,
      });
    } catch (error) {
      addError("credentialIssuanceError", error);
    }
  };

  return (
    <DataListItem
      id={`credential-${credential.credentialScopeName}`}
      key={credential.credentialScopeName}
      aria-label={t("verifiableCredentials")}
    >
      <DataListItemRow>
        <DataListItemCells
          dataListCells={[
            <DataListCell key="name" width={2}>
              {credential.credentialScopeName}
            </DataListCell>,
            <DataListCell key="created" width={2}>
              {credential.createdDate
                ? formatDate(
                    new Date(credential.createdDate),
                    undefined,
                    FORMAT_DATE_ONLY,
                  )
                : "—"}
            </DataListCell>,
          ]}
        />
        <DataListAction
          aria-labelledby={t("actions")}
          aria-label={t("credentialActions")}
          id="credentialActions"
        >
          <Flex>
            <FlexItem>
              <Button
                id={`credential-${credential.credentialScopeName}-issue`}
                variant="link"
                onClick={handleIssueToWallet}
                icon={<ExternalLinkAltIcon />}
              >
                {t("issueToWallet")}
              </Button>
            </FlexItem>
            {hasManageRole() && (
              <FlexItem>
                <ContinueCancelModal
                  buttonTitle={t("delete")}
                  modalTitle={t("deleteCredential")}
                  continueLabel={t("delete")}
                  cancelLabel={t("cancel")}
                  buttonVariant="link"
                  onContinue={handleDelete}
                >
                  {t("deleteCredentialConfirm", {
                    credentialName: credential.credentialScopeName,
                  })}
                </ContinueCancelModal>
              </FlexItem>
            )}
          </Flex>
        </DataListAction>
      </DataListItemRow>
    </DataListItem>
  );
};
