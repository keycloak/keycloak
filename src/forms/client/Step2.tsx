import {
  Form,
  FormGroup,
  Switch,
  Checkbox,
  Grid,
  GridItem,
} from "@patternfly/react-core";
import React, { FormEvent } from "react";
import { ClientRepresentation } from "../../model/client-model";

type Step2Props = {
  onChange: (
    value: string | boolean,
    event: FormEvent<HTMLInputElement>
  ) => void;
  client: ClientRepresentation;
};

export const Step2 = ({ client, onChange }: Step2Props) => (
  <Form isHorizontal>
    <FormGroup label="Client authentication" fieldId="kc-authentication">
      <Switch
        id="kc-authentication"
        name="publicClient"
        label="ON"
        labelOff="OFF"
        isChecked={client.publicClient}
        onChange={onChange}
      />
    </FormGroup>
    <FormGroup label="Authentication" fieldId="kc-authorisation">
      <Switch
        id="kc-authorisation"
        name="authorizationServicesEnabled"
        label="ON"
        labelOff="OFF"
        isChecked={client.authorizationServicesEnabled}
        onChange={onChange}
      />
    </FormGroup>
    <FormGroup label="Authentication flow" fieldId="kc-flow">
      <Grid>
        <GridItem span={6}>
          <Checkbox
            label="Standard flow"
            aria-label="Enable standard flow"
            id="kc-flow-standard"
            name="standardFlowEnabled"
            isChecked={client.standardFlowEnabled}
            onChange={onChange}
          />
        </GridItem>
        <GridItem span={6}>
          <Checkbox
            label="Direct access"
            aria-label="Enable Direct access"
            id="kc-flow-direct"
            name="directAccessGrantsEnabled"
            isChecked={client.directAccessGrantsEnabled}
            onChange={onChange}
          />
        </GridItem>
        <GridItem span={6}>
          <Checkbox
            label="Implicid flow"
            aria-label="Enable implicid flow"
            id="kc-flow-implicid"
            name="implicitFlowEnabled"
            isChecked={client.implicitFlowEnabled}
            onChange={onChange}
          />
        </GridItem>
        <GridItem span={6}>
          <Checkbox
            label="Service account"
            aria-label="Enable service account"
            id="kc-flow-service-account"
            name="serviceAccountsEnabled"
            isChecked={client.serviceAccountsEnabled}
            onChange={onChange}
          />
        </GridItem>
      </Grid>
    </FormGroup>
  </Form>
);
