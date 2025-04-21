import React from 'react';
import {
  CodeBlock,
  CodeBlockAction,
  CodeBlockCode,
  ClipboardCopyButton,
  ExpandableSection,
  ExpandableSectionToggle,
  Button
} from '@patternfly/react-core';
import PlayIcon from '@patternfly/react-icons/dist/esm/icons/play-icon';

export const ExpandableCodeBlock: React.FunctionComponent = () => {
  const [isExpanded, setIsExpanded] = React.useState(false);
  const [copied, setCopied] = React.useState(false);

  const onToggle = isExpanded => {
    setIsExpanded(isExpanded);
  };

  const clipboardCopyFunc = (event, text) => {
    const clipboard = event.currentTarget.parentElement;
    const el = document.createElement('textarea');
    el.value = text.toString();
    clipboard.appendChild(el);
    el.select();
    document.execCommand('copy');
    clipboard.removeChild(el);
  };

  const onClick = (event, text) => {
    clipboardCopyFunc(event, text);
    setCopied(true);
  };

  const copyBlock = `apiVersion: helm.openshift.io/v1beta1/
kind: HelmChartRepository
metadata:
name: azure-sample-repo
spec:
connectionConfig:
url: https://raw.githubusercontent.com/Azure-Samples/helm-charts/master/docs`;

  const code = `apiVersion: helm.openshift.io/v1beta1/
kind: HelmChartRepository
metadata:
name: azure-sample-repo`;
  const expandedCode = `spec:
connectionConfig:
url: https://raw.githubusercontent.com/Azure-Samples/helm-charts/master/docs`;

  const actions = (
    <React.Fragment>
      <CodeBlockAction>
        <ClipboardCopyButton
          id="copy-button"
          textId="code-content"
          aria-label="Copy to clipboard"
          onClick={e => onClick(e, copyBlock)}
          exitDelay={600}
          maxWidth="110px"
          variant="plain"
        >
          {copied ? 'Successfully copied to clipboard!' : 'Copy to clipboard'}
        </ClipboardCopyButton>
      </CodeBlockAction>
      <CodeBlockAction>
        <Button variant="plain" aria-label="Play icon">
          <PlayIcon />
        </Button>
      </CodeBlockAction>
    </React.Fragment>
  );

  return (
    <CodeBlock actions={actions}>
      <CodeBlockCode>
        {code}
        <ExpandableSection isExpanded={isExpanded} isDetached contentId="code-block-expand">
          {expandedCode}
        </ExpandableSection>
      </CodeBlockCode>
      <ExpandableSectionToggle isExpanded={isExpanded} onToggle={onToggle} contentId="code-block-expand" direction="up">
        {isExpanded ? 'Show Less' : 'Show More'}
      </ExpandableSectionToggle>
    </CodeBlock>
  );
};
