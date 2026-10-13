import { ClipboardCopy, FormGroup } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { HelpItem, useEnvironment } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../../context/realm-context/RealmContext";
import { addTrailingSlash } from "../../util";

export const RedirectUrl = ({ id }: { id: string }) => {
  const { environment } = useEnvironment();
  const { t } = useTranslation();

  const { realm } = useRealm();
  const callbackUrl = `${addTrailingSlash(
    environment.serverBaseUrl,
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
