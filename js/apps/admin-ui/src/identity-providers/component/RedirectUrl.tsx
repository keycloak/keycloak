import { ClipboardCopy, FormGroup } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { HelpItem, useEnvironment } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../../context/realm-context/RealmContext";
import { identityProviderRedirectUrl } from "../../utils/identity-provider-redirect-url";

export const RedirectUrl = ({ id }: { id: string }) => {
  const { environment } = useEnvironment();
  const { t } = useTranslation();

  const { realm, realmRepresentation } = useRealm();
  const callbackUrl = identityProviderRedirectUrl(
    id,
    realm,
    environment.serverBaseUrl,
    realmRepresentation.attributes?.frontendUrl,
  );

  return (
    <FormGroup
      label={t("redirectURI")}
      labelIcon={
        <HelpItem helpText={t("redirectURIHelp")} fieldLabelId="redirectURI" />
      }
      fieldId="kc-redirect-uri"
    >
      <ClipboardCopy isReadOnly>{callbackUrl}</ClipboardCopy>
    </FormGroup>
  );
};
