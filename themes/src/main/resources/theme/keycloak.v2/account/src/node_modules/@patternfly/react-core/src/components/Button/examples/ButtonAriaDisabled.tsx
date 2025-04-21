import React from 'react';
import { Button } from '@patternfly/react-core';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
import PlusCircleIcon from '@patternfly/react-icons/dist/esm/icons/plus-circle-icon';

export const ButtonAriaDisabled: React.FunctionComponent = () => (
  <React.Fragment>
    <Button isAriaDisabled>Primary aria disabled</Button> <Button isAriaDisabled>Secondary aria disabled</Button>{' '}
    <Button variant="secondary" isDanger isAriaDisabled>
      Danger secondary aria disabled
    </Button>{' '}
    <Button isAriaDisabled variant="tertiary">
      Tertiary aria disabled
    </Button>{' '}
    <Button isAriaDisabled variant="danger">
      Danger disabled
    </Button>{' '}
    <Button isAriaDisabled variant="warning">
      Warning disabled
    </Button>
    <br />
    <br />
    <Button isAriaDisabled variant="link" icon={<PlusCircleIcon />}>
      Link aria disabled
    </Button>{' '}
    <Button isAriaDisabled variant="link" isInline>
      Inline link aria disabled
    </Button>{' '}
    <Button variant="link" isDanger isAriaDisabled>
      Danger link disabled
    </Button>{' '}
    <Button isAriaDisabled variant="plain" aria-label="Action">
      <TimesIcon />
    </Button>{' '}
    <Button isAriaDisabled variant="control">
      Control aria disabled
    </Button>
  </React.Fragment>
);
