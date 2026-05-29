import {
  ContinueCancelModal,
  useEnvironment,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  DataList,
  DataListAction,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Label,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getIssuedVerifiableCredentials,
  revokeIssuedVerifiableCredential,
} from "../api/methods";
import { IssuedUserVerifiableCredentialRepresentation } from "../api/representations";
import { EmptyRow } from "../components/datalist/EmptyRow";
import { formatDate, FORMAT_DATE_AND_TIME } from "../utils/formatDate";
import { useAccountAlerts } from "../utils/useAccountAlerts";

type IssuedCredentialsModalProps = {
  credentialScopeName: string;
  onClose: () => void;
};

export const IssuedCredentialsModal = ({
  credentialScopeName,
  onClose,
}: IssuedCredentialsModalProps) => {
  const { t } = useTranslation();
  const context = useEnvironment();
  const { addAlert, addError } = useAccountAlerts();
  const [issuedCredentials, setIssuedCredentials] = useState<
    IssuedUserVerifiableCredentialRepresentation[]
  >([]);
  const [key, setKey] = useState(1);

  const refresh = () => setKey(key + 1);

  const hasManageRole = () => {
    const token = context.keycloak.tokenParsed;
    const accountRoles = token?.resource_access?.["account"]?.roles || [];
    return (
      accountRoles.includes("manage-account") ||
      accountRoles.includes("manage-verifiable-credentials")
    );
  };

  useEffect(() => {
    const controller = new AbortController();

    async function fetchIssuedCredentials() {
      try {
        const data = await getIssuedVerifiableCredentials({
          signal: controller.signal,
          context,
        });
        // Filter by credential type
        const filtered = data.filter(
          (ic) => ic.credentialType === credentialScopeName,
        );
        setIssuedCredentials(filtered);
      } catch (error) {
        console.error("Error fetching issued verifiable credentials:", error);
        setIssuedCredentials([]);
      }
    }

    void fetchIssuedCredentials();
    return () => controller.abort();
  }, [key, context, credentialScopeName]);

  const handleRevoke = async (credentialId: string) => {
    try {
      await revokeIssuedVerifiableCredential(context, credentialId);
      addAlert(t("revokeIssuedCredentialSuccess"));
      refresh();
    } catch (error) {
      addError("revokeIssuedCredentialError", error);
    }
  };

  const isExpired = (expiresAt?: number) => {
    if (!expiresAt) return false;
    return expiresAt < Date.now();
  };

  return (
    <Modal
      variant={ModalVariant.large}
      title={t("Issued Credentials")}
      isOpen={true}
      onClose={onClose}
      width="90%"
    >
      <DataList
        id="issued-credentials-modal-list"
        aria-label={t("issuedCredentials")}
        isCompact
      >
        <DataListItem
          id="issued-credentials-modal-header"
          aria-labelledby="Column headers"
        >
          <DataListItemRow>
            <DataListItemCells
              dataListCells={[
                <DataListCell key="credential-id-header" width={3}>
                  <strong>{t("Credential ID")}</strong>
                </DataListCell>,
                <DataListCell key="issued-date-header" width={2}>
                  <strong>{t("Issued Date")}</strong>
                </DataListCell>,
                <DataListCell key="expires-date-header" width={2}>
                  <strong>{t("Expires")}</strong>
                </DataListCell>,
                <DataListCell key="status-header" width={1}>
                  <strong>{t("Status")}</strong>
                </DataListCell>,
              ]}
            />
          </DataListItemRow>
        </DataListItem>
        {issuedCredentials.length > 0 ? (
          issuedCredentials.map((credential) => (
            <DataListItem
              key={credential.id}
              id={`issued-credential-${credential.id}`}
              aria-label={credential.id}
            >
              <DataListItemRow>
                <DataListItemCells
                  dataListCells={[
                    <DataListCell key="id" width={3}>
                      <code className="pf-v5-u-font-size-sm">
                        {credential.id}
                      </code>
                    </DataListCell>,
                    <DataListCell key="issued" width={2}>
                      {credential.issuedAt
                        ? formatDate(
                            new Date(credential.issuedAt),
                            undefined,
                            FORMAT_DATE_AND_TIME,
                          )
                        : "—"}
                    </DataListCell>,
                    <DataListCell key="expires" width={2}>
                      {credential.expiresAt
                        ? formatDate(
                            new Date(credential.expiresAt),
                            undefined,
                            FORMAT_DATE_AND_TIME,
                          )
                        : "—"}
                    </DataListCell>,
                    <DataListCell key="status" width={1}>
                      {isExpired(credential.expiresAt) ? (
                        <Label color="red">{t("Expired")}</Label>
                      ) : (
                        <Label color="green">{t("Active")}</Label>
                      )}
                    </DataListCell>,
                  ]}
                />
                {hasManageRole() && (
                  <DataListAction
                    aria-labelledby={t("actions")}
                    aria-label={t("credentialActions")}
                    id={`issued-credential-${credential.id}-actions`}
                  >
                    <ContinueCancelModal
                      buttonTitle={t("revoke")}
                      modalTitle={t("Revoke Issued Credential")}
                      continueLabel={t("revoke")}
                      cancelLabel={t("cancel")}
                      buttonVariant="link"
                      onContinue={() => handleRevoke(credential.id!)}
                    >
                      {t("Revoke Issued Credential Confirm")}
                    </ContinueCancelModal>
                  </DataListAction>
                )}
              </DataListItemRow>
            </DataListItem>
          ))
        ) : (
          <EmptyRow message={t("No Issued Credentials")} />
        )}
      </DataList>
      <div className="pf-v5-u-mt-md pf-v5-u-text-align-right">
        <Button variant="primary" onClick={onClose}>
          {t("close")}
        </Button>
      </div>
    </Modal>
  );
};
