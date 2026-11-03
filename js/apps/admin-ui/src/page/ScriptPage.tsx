import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import { KeycloakSpinner, useAlerts } from "@keycloak/keycloak-ui-shared";
import { PageSection } from "@patternfly/react-core";
import type { i18n as I18n } from "i18next";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { SCRIPT_PROVIDER } from "./constants";

export type ScriptPageParams = { realm?: string; providerId: string };

export type UiScriptContext = {
  realm: string;
  i18n: I18n;
  adminClient: KeycloakAdminClient;
};

type UiScriptModule = {
  default: (context: UiScriptContext) => HTMLElement;
};

export default function ScriptPage() {
  const { adminClient } = useAdminClient();
  const { t, i18n } = useTranslation();
  const { addError } = useAlerts();
  const { realm } = useRealm();
  const { componentTypes } = useServerInfo();
  const { providerId } = useParams<ScriptPageParams>();
  const [container, setContainer] = useState<HTMLDivElement | null>(null);
  const [loading, setLoading] = useState(true);

  const scripts = componentTypes?.[SCRIPT_PROVIDER];
  const script = scripts?.find((s) => s.id === providerId);
  const url =
    typeof script?.metadata.url === "string" ? script.metadata.url : undefined;

  useEffect(() => {
    if (!container || !url) {
      return;
    }

    const host = container;
    const active = { current: true };
    let mounted: HTMLElement | undefined;

    setLoading(true);

    void (async () => {
      try {
        const module = (await import(/* @vite-ignore */ url)) as UiScriptModule;
        if (!active.current) {
          return;
        }
        mounted = module.default({ realm, i18n, adminClient });
        host.replaceChildren(mounted);
      } catch (error) {
        if (active.current) {
          addError("notFound", error);
        }
      } finally {
        if (active.current) {
          setLoading(false);
        }
      }
    })();

    return () => {
      active.current = false;
      mounted?.remove();
      host.replaceChildren();
    };
  }, [container, url, realm, i18n, adminClient, addError]);

  if (!script || !url) {
    throw new Error(t("notFound"));
  }

  return (
    <>
      <ViewHeader titleKey={script.id} />
      <PageSection variant="light">
        {loading && <KeycloakSpinner />}
        <div ref={setContainer} hidden={loading} />
      </PageSection>
    </>
  );
}
