import React from 'react';
import AtIcon from '@patternfly/react-icons/dist/esm/icons/at-icon';
import { InputGroup, InputGroupText, InputGroupTextVariant, TextInput, ValidatedOptions } from '@patternfly/react-core';

export const InputGroupBasic: React.FunctionComponent = () => (
  <React.Fragment>
    <InputGroup>
      <TextInput id="textInput6" type="email" aria-label="email input field" />
      <InputGroupText id="email-example">@example.com</InputGroupText>
    </InputGroup>
    <br />
    <InputGroup>
      <InputGroupText id="username">
        <AtIcon />
      </InputGroupText>
      <TextInput
        validated={ValidatedOptions.error}
        id="textInput7"
        type="email"
        aria-label="Error state username example"
      />
    </InputGroup>
    <br />
    <InputGroup>
      <TextInput name="textIndex12" id="textInput12" type="text" aria-label="percentage" />
      <InputGroupText id="plain-example" variant={InputGroupTextVariant.plain}>
        %
      </InputGroupText>
    </InputGroup>
  </React.Fragment>
);
