import { Alert, PageSection } from "@patternfly/react-core";
import { KeycloakSpinner, useEnvironment } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import type { Environment } from "../environment-types";
import { SCRIPT_PROVIDER } from "./constants";
import { UiScriptHost } from "./UiScriptHost";
import { useScriptLoader } from "./useScriptLoader";
import type { UiScriptParams } from "./routes";

export default function ScriptPage() {
  const { t } = useTranslation();
  const { componentTypes } = useServerInfo();
  const { realm } = useRealm();
  const { environment } = useEnvironment<Environment>();
  const { providerId } = useParams<UiScriptParams>();
  const scripts = componentTypes?.[SCRIPT_PROVIDER];
  const script = scripts?.find((p) => p.id === providerId);

  if (!script) {
    throw new Error(t("notFound"));
  }

  const tagName = script.metadata.tagName as string | undefined;
  const scriptUrl = `${environment.adminBaseUrl}/admin/realms/${encodeURIComponent(realm)}/ui-scripts/${encodeURIComponent(providerId!)}/script.js`;
  const { state, error } = useScriptLoader(scriptUrl, tagName);

  return (
    <>
      <ViewHeader titleKey={script.id} subKey={script.helpText} />
      <PageSection variant="light">
        {state === "loading" && <KeycloakSpinner />}
        {state === "error" && (
          <Alert variant="danger" title={error?.message || t("error")} />
        )}
        {state === "ready" && tagName && <UiScriptHost tagName={tagName} />}
      </PageSection>
    </>
  );
}
