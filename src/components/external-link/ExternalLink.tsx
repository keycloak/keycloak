import React from 'react';
import { ExternalLinkAltIcon } from '@patternfly/react-icons';

export const ExternalLink = ({
  title,
  href,
  ...rest
}: React.HTMLProps<HTMLAnchorElement>) => {
  return (
    <a href={href} {...rest}>
      {title ? title : href}{' '}
      {href?.startsWith('http') && <ExternalLinkAltIcon />}
    </a>
  );
};
