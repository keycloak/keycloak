import React from "react";
import { useTranslation } from "react-i18next";
import {
  CodeBlock,
  CodeBlockAction,
  EmptyState,
  EmptyStateBody,
  TextArea,
  Title,
} from "@patternfly/react-core";

import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { CopyToClipboardButton } from "./CopyToClipboardButton";

type GeneratedCodeTabProps = {
  user?: UserRepresentation;
  text: string;
  label: string;
};

export const GeneratedCodeTab = ({
  text,
  user,
  label,
}: GeneratedCodeTabProps) => {
  const { t } = useTranslation("clients");

  return user ? (
    <CodeBlock
      id={label}
      actions={
        <CodeBlockAction>
          <CopyToClipboardButton text={text} label={label} />
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
