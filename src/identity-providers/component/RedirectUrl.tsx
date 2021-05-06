import React from "react";
import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ClipboardCopy, FormGroup } from "@patternfly/react-core";

import { getBaseUrl } from "../../util";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useAdminClient } from "../../context/auth/AdminClient";

export const RedirectUrl = () => {
  const { t } = useTranslation("identity-providers");
  const { t: th } = useTranslation("identity-providers-help");
  const { id } = useParams<{ id: string }>();

  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const callbackUrl = `${getBaseUrl(adminClient)}/realms/${realm}/broker`;

  return (
    <FormGroup
      label={t("redirectURI")}
      labelIcon={
        <HelpItem
          helpText={th("redirectURI")}
          forLabel={t("redirectURI")}
          forID="kc-redirect-uri"
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
