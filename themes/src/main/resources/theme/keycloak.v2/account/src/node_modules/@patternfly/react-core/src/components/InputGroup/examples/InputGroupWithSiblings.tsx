import React from 'react';
import DollarSignIcon from '@patternfly/react-icons/dist/esm/icons/dollar-sign-icon';
import { Button, TextArea, InputGroup, InputGroupText, TextInput } from '@patternfly/react-core';

export const InputGroupWithSiblings: React.FunctionComponent = () => (
  <React.Fragment>
    <InputGroup>
      <Button id="textAreaButton1" variant="control">
        Button
      </Button>
      <TextArea name="textarea1" id="textarea1" aria-label="textarea with buttons" />
      <Button variant="control">Button</Button>
    </InputGroup>
    <br />
    <InputGroup>
      <Button id="textAreaButton3" variant="control">
        Button
      </Button>
      <Button variant="control">Button</Button>
      <TextArea name="textarea3" id="textarea3" aria-label="textarea with 3 buttons" />
      <Button variant="control">Button</Button>
    </InputGroup>
    <br />
    <InputGroup>
      <InputGroupText>
        <DollarSignIcon />
      </InputGroupText>
      <TextInput id="textInput5" type="number" aria-label="Dollar amount input example" />
      <InputGroupText>.00</InputGroupText>
    </InputGroup>
  </React.Fragment>
);
