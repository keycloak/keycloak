import { useTranslation } from "react-i18next";
import {
  CodeBlock,
  CodeBlockAction,
  EmptyState,
  EmptyStateBody,
  Title,
} from "@patternfly/react-core";

import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { CopyToClipboardButton } from "./CopyToClipboardButton";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";

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
  const { t } = useTranslation();

  return user ? (
    <CodeBlock
      id={label}
      actions={
        <CodeBlockAction>
          <CopyToClipboardButton id="code" text={text} label={label} />
        </CodeBlockAction>
      }
    >
      <KeycloakTextArea id={`text-area-${label}`} rows={20} value={text} />
    </CodeBlock>
  ) : (
    <EmptyState variant="large" id={label}>
      <Title headingLevel="h2" size="lg">
        {t(`${label}No`)}
      </Title>
      <EmptyStateBody>{t(`${label}IsDisabled`)}</EmptyStateBody>
    </EmptyState>
  );
};
