import React, { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  ClipboardCopyButton,
  CodeBlock,
  CodeBlockAction,
  EmptyState,
  EmptyStateBody,
  TextArea,
  Title,
} from "@patternfly/react-core";

import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import useSetTimeout from "../../utils/useSetTimeout";

type GeneratedCodeTabProps = {
  user?: UserRepresentation;
  text: string;
  label: string;
};

enum CopyState {
  Ready,
  Copied,
  Error,
}

export const GeneratedCodeTab = ({
  text,
  user,
  label,
}: GeneratedCodeTabProps) => {
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

  return user ? (
    <CodeBlock
      id={label}
      actions={
        <CodeBlockAction>
          <ClipboardCopyButton
            id={`copy-button-${label}`}
            textId={label}
            aria-label={t("copyToClipboard")}
            onClick={() => copyToClipboard(text)}
            exitDelay={600}
            variant="plain"
          >
            {copyMessage}
          </ClipboardCopyButton>
        </CodeBlockAction>
      }
    >
      <TextArea id={`text-area-${label}`} rows={20} value={text} />
    </CodeBlock>
  ) : (
    <EmptyState variant="large">
      <Title headingLevel="h4" size="lg">
        {t(`${label}No`)}
      </Title>
      <EmptyStateBody>{t(`${label}IsDisabled`)}</EmptyStateBody>
    </EmptyState>
  );
};
