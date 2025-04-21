import React from 'react';
import { Button, TextArea, InputGroup } from '@patternfly/react-core';

export const InputGroupWithTextarea: React.FunctionComponent = () => (
  <React.Fragment>
    <InputGroup>
      <TextArea name="textarea2" id="textarea2" aria-label="textarea with button" />
      <Button id="textAreaButton2" variant="control">
        Button
      </Button>
    </InputGroup>
  </React.Fragment>
);
