import React from 'react';
import { Label } from '@patternfly/react-core';
import InfoCircleIcon from '@patternfly/react-icons/dist/esm/icons/info-circle-icon';
import { Link } from '@reach/router';

export const LabelRouterLink: React.FunctionComponent = () => (
  <Label
    color="blue"
    icon={<InfoCircleIcon />}
    onClose={() => Function.prototype}
    isTruncated
    render={({ className, content, componentRef }) => (
      <Link to="/" className={className} innerRef={componentRef}>
        {content}
      </Link>
    )}
  >
    Blue label router link with icon that overflows
  </Label>
);
