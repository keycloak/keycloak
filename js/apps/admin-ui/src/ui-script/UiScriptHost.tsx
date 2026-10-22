import { useCallback, useEffect, useRef } from "react";
import { useAdminClient } from "../admin-client";
import { useRealm } from "../context/realm-context/RealmContext";
import { useEnvironment } from "@keycloak/keycloak-ui-shared";
import type { Environment } from "../environment-types";
import type { UiScriptContext, UiScriptElement } from "./types";

type UiScriptHostProps = {
  tagName: string;
};

export const UiScriptHost = ({ tagName }: UiScriptHostProps) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const { keycloak } = useAdminClient();
  const { environment } = useEnvironment<Environment>();
  const { realm, realmRepresentation } = useRealm();

  const getAccessToken = useCallback(async () => {
    try {
      await keycloak.updateToken(5);
      return keycloak.token;
    } catch {
      return undefined;
    }
  }, [keycloak]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return;
    }

    const context: UiScriptContext = {
      realm,
      realmRepresentation,
      adminBaseUrl: environment.adminBaseUrl,
      serverBaseUrl: environment.serverBaseUrl,
      authServerUrl: environment.authServerUrl,
      getAccessToken,
    };

    const element = document.createElement(tagName) as UiScriptElement;
    element.context = context;
    container.appendChild(element);

    return () => {
      container.removeChild(element);
    };
  }, [
    tagName,
    realm,
    realmRepresentation,
    environment.adminBaseUrl,
    environment.serverBaseUrl,
    environment.authServerUrl,
    getAccessToken,
  ]);

  return <div ref={containerRef} />;
};
