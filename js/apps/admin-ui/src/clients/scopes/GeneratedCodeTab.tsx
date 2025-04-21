import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  CodeBlock,
  CodeBlockAction,
  EmptyState,
  EmptyStateBody,
  EmptyStateHeader,
  TextArea,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { CopyToClipboardButton } from "../../components/copy-to-clipboard-button/CopyToClipboardButton";

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
      <TextArea
        id={`text-area-${label}`}
        rows={20}
        value={text}
        aria-label={label}
      />
    </CodeBlock>
  ) : (
    <EmptyState variant="lg" id={label}>
      <EmptyStateHeader titleText={<>{t(`${label}No`)}</>} headingLevel="h2" />
      <EmptyStateBody>{t(`${label}IsDisabled`)}</EmptyStateBody>
    </EmptyState>
  );
};
