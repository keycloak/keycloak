import { ClipboardCopy, FormGroup } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { useRealm } from "../../context/realm-context/RealmContext";
import { addTrailingSlash } from "../../util";

export const RedirectUrl = ({ id }: { id: string }) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();

  const { realm } = useRealm();
  const callbackUrl = `${addTrailingSlash(
    adminClient.baseUrl,
  )}realms/${realm}/broker`;

  return (
    <FormGroup
      label={t("redirectURI")}
      labelIcon={
        <HelpItem helpText={t("redirectURIHelp")} fieldLabelId="redirectURI" />
      }
      fieldId="kc-redirect-uri"
    >
      <ClipboardCopy
        isReadOnly
      >{`${callbackUrl}/${id}/endpoint`}</ClipboardCopy>
    </FormGroup>
  );
};
