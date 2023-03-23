import { useTranslation } from "react-i18next";
import { ClipboardCopy, FormGroup } from "@patternfly/react-core";

import { HelpItem } from "ui-shared";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useAdminClient } from "../../context/auth/AdminClient";
import { addTrailingSlash } from "../../util";

export const RedirectUrl = ({ id }: { id: string }) => {
  const { t } = useTranslation("identity-providers");

  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const callbackUrl = `${addTrailingSlash(
    adminClient.baseUrl
  )}realms/${realm}/broker`;

  return (
    <FormGroup
      label={t("redirectURI")}
      labelIcon={
        <HelpItem
          helpText={t("identity-providers-help:redirectURI")}
          fieldLabelId="identity-providers:redirectURI"
        />
      }
      fieldId="kc-redirect-uri"
    >
      <ClipboardCopy
        isReadOnly
      >{`${callbackUrl}/${id}/endpoint`}</ClipboardCopy>
    </FormGroup>
  );
};
