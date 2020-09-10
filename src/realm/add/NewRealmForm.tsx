import React from "react";
import {
  Text,
  PageSection,
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
import { useTranslation } from "react-i18next";

//type NewRealmFormProps = {
//  realm: string;
//};

export const NewRealmForm = () => {
  const { t } = useTranslation("realm");
  //({ realm }: NewRealmFormProps) => {
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
          <FormGroup label={t("Upload JSON file")} fieldId="kc-realm-filename">
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
          <FormGroup label={t("Realm name")} isRequired fieldId="kc-realm-name">
            <TextInput
              isRequired
              type="text"
              id="kc-realm-name"
              name="kc-realm-name"
              // value={value2}
              // onChange={this.handleTextInputChange2}
            />
          </FormGroup>
          <FormGroup label={t("Enabled")} fieldId="kc-realm-enabled-switch">
            <Switch
              id="kc-realm-enabled-switch"
              name="kc-realm-enabled-switch"
              label={t("On")}
              labelOff={t("Off")}
              // isChecked={isChecked}
              // onChange={this.handleChange}
            />
          </FormGroup>
          <ActionGroup>
            <Button variant="primary">{t("Create")}</Button>
            <Button variant="link">{t("Cancel")}</Button>
          </ActionGroup>
        </Form>
      </PageSection>
    </>
  );
};
