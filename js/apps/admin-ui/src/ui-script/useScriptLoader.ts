import { useEffect, useState } from "react";
import { useAdminClient } from "../admin-client";

type ScriptLoaderState = "loading" | "ready" | "error";

export function useScriptLoader(
  scriptUrl?: string,
  tagName?: string,
): { state: ScriptLoaderState; error?: Error } {
  const { keycloak } = useAdminClient();
  const [state, setState] = useState<ScriptLoaderState>("loading");
  const [error, setError] = useState<Error>();

  useEffect(() => {
    if (!scriptUrl || !tagName) {
      return;
    }

    let blobUrl: string | undefined;
    let script: HTMLScriptElement | undefined;
    let cancelled = false;

    const load = async () => {
      try {
        setState("loading");
        setError(undefined);
        await keycloak.updateToken(5);
        const response = await fetch(scriptUrl, {
          headers: {
            Authorization: `Bearer ${keycloak.token}`,
          },
        });

        if (!response.ok) {
          throw new Error(`Failed to load script: ${response.status}`);
        }

        const code = await response.text();
        if (cancelled) {
          return;
        }

        blobUrl = URL.createObjectURL(
          new Blob([code], { type: "text/javascript" }),
        );
        script = document.createElement("script");
        script.src = blobUrl;
        script.async = true;

        await new Promise<void>((resolve, reject) => {
          script!.onload = () => resolve();
          script!.onerror = () => reject(new Error("Script failed to load"));
          document.head.appendChild(script!);
        });

        if (!customElements.get(tagName)) {
          throw new Error(`Custom element ${tagName} was not registered`);
        }

        setState("ready");
      } catch (loadError) {
        if (!cancelled) {
          setError(
            loadError instanceof Error
              ? loadError
              : new Error(String(loadError)),
          );
          setState("error");
        }
      }
    };

    void load();

    return () => {
      cancelled = true;
      if (script?.parentNode) {
        script.parentNode.removeChild(script);
      }
      if (blobUrl) {
        URL.revokeObjectURL(blobUrl);
      }
    };
  }, [scriptUrl, tagName, keycloak]);

  return { state, error };
}
