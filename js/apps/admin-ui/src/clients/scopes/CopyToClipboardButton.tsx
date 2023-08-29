import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  ClipboardCopyButton,
  ClipboardCopyButtonProps,
} from "@patternfly/react-core";

import useSetTimeout from "../../utils/useSetTimeout";

enum CopyState {
  Ready,
  Copied,
  Error,
  Denied,
}

type CopyToClipboardButtonProps = Pick<ClipboardCopyButtonProps, "variant"> & {
  id: string;
  label: string;
  text: string;
};

export const CopyToClipboardButton = ({
  id,
  label,
  text,
  variant = "plain",
}: CopyToClipboardButtonProps) => {
  const { t } = useTranslation("clients");
  const setTimeout = useSetTimeout();

  const [copyState, setCopyState] = useState(CopyState.Ready);

  const copyMessage = useMemo(() => {
    switch (copyState) {
      case CopyState.Ready:
        return t("copyToClipboard");
      case CopyState.Copied:
        return t("copySuccess");
      case CopyState.Error:
        return t("clipboardCopyError");
      case CopyState.Denied:
        return t("clipboardCopyDenied");
    }
  }, [copyState]);

  useEffect(() => {
    if (copyState !== CopyState.Ready) {
      return setTimeout(() => setCopyState(CopyState.Ready), 1000);
    }
  }, [copyState]);

  const copy = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopyState(CopyState.Copied);
    } catch (error) {
      setCopyState(CopyState.Error);
    }
  };

  const copyToClipboard = async (text: string) => {
    const permissionStatus = await navigator.permissions.query({
      //@ts-ignore
      name: "clipboard-write",
    });
    switch (permissionStatus.state) {
      case "granted":
        copy(text);
        break;
      case "prompt":
        permissionStatus.onchange = function () {
          if (this.state === "granted") {
            copy(text);
          }
        };
        break;
      case "denied":
        setCopyState(CopyState.Denied);
    }
  };

  return (
    <ClipboardCopyButton
      id={`copy-button-${id}`}
      textId={label}
      aria-label={t("copyToClipboard")}
      onClick={() => copyToClipboard(text)}
      exitDelay={600}
      variant={variant}
    >
      {copyMessage}
    </ClipboardCopyButton>
  );
};
