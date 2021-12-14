import React from "react";
import { useTranslation } from "react-i18next";
import { ClipboardCopy, FormGroup } from "@patternfly/react-core";

import { getBaseUrl } from "../../util";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useAdminClient } from "../../context/auth/AdminClient";

export const RedirectUrl = ({ id }: { id: string }) => {
  const { t } = useTranslation("identity-providers");

  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const callbackUrl = `${getBaseUrl(adminClient)}realms/${realm}/broker`;

  return (
    <FormGroup
      label={t("redirectURI")}
      labelIcon={
        <HelpItem
          helpText="identity-providers-help:redirectURI"
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
