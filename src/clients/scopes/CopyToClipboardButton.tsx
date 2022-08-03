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

  const [copy, setCopy] = useState(CopyState.Ready);

  const copyMessage = useMemo(() => {
    switch (copy) {
      case CopyState.Ready:
        return t("copyToClipboard");
      case CopyState.Copied:
        return t("copySuccess");
      case CopyState.Error:
        return t("clipboardCopyError");
    }
  }, [copy]);

  useEffect(() => {
    if (copy !== CopyState.Ready) {
      return setTimeout(() => setCopy(CopyState.Ready), 1000);
    }
  }, [copy]);

  const copyToClipboard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopy(CopyState.Copied);
    } catch (error) {
      setCopy(CopyState.Error);
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
