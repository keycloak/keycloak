import React, { useState } from "react";
import {
  Text,
  PageSection,
  PageSectionVariants,
  TextContent,
  FormGroup,
  Form,
  TextInput,
  Switch,
  FileUpload,
  ActionGroup,
  Button,
  Divider,
} from "@patternfly/react-core";

type NewRealmFormProps = {
  realm: string;
};

export const NewRealmForm = ({ realm }: NewRealmFormProps) => {
  return (
    <>
      <PageSection variant="light">
        <TextContent>
          <Text component="h1">Create Realm</Text>
        </TextContent>
      </PageSection>
      <Divider />
      <PageSection variant="light">
        <Form isHorizontal>
          <FormGroup label="Upload JSON file" fieldId="kc-realm-filename">
            <FileUpload
              id="simple-text-file"
              type="text"
              //   value={value}
              //   filename={filename}
              //   onChange={this.handleFileChange}
              //   onReadStarted={this.handleFileReadStarted}
              //   onReadFinished={this.handleFileReadFinished}
              //   isLoading={isLoading}
            />
          </FormGroup>
          <FormGroup label="Realm name" isRequired fieldId="kc-realm-name">
            <TextInput
              isRequired
              type="text"
              id="kc-realm-name"
              name="kc-realm-name"
              // value={value2}
              // onChange={this.handleTextInputChange2}
            />
          </FormGroup>
          <FormGroup label="Enabled" fieldId="kc-realm-enabled-switch">
            <Switch
              id="kc-realm-enabled-switch"
              name="kc-realm-enabled-switch"
              label="On"
              labelOff="Off"
              // isChecked={isChecked}
              // onChange={this.handleChange}
            />
          </FormGroup>
          <ActionGroup>
            <Button variant="primary">Create</Button>
            <Button variant="link">Cancel</Button>
          </ActionGroup>
        </Form>
      </PageSection>
    </>
  );
};
