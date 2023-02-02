import {
  ActionGroup,
  Button,
  Form,
  Page,
  PageSection,
} from "@patternfly/react-core";
import { mount } from "cypress/react";
import { useEffect } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { KeyValueType } from "../../src/components/key-value-form/key-value-convert";
import { KeyValueInput } from "../../src/components/key-value-form/KeyValueInput";

type KeyValueInputTestProps = {
  submit: (values: any) => void;
  defaultValues?: KeyValueType[];
};

const KeyValueInputTest = ({
  submit,
  defaultValues,
}: KeyValueInputTestProps) => {
  const form = useForm();

  useEffect(() => {
    form.setValue("name", defaultValues || "");
  }, [form.setValue]);

  return (
    <Page>
      <PageSection variant="light">
        <FormProvider {...form}>
          <Form isHorizontal onSubmit={form.handleSubmit(submit)}>
            <KeyValueInput name="name" />
            <ActionGroup>
              <Button data-testid="save" type="submit">
                Save
              </Button>
            </ActionGroup>
          </Form>
        </FormProvider>
      </PageSection>
    </Page>
  );
};

describe("KeyValueInput", () => {
  it("basic interaction", () => {
    const submit = cy.spy().as("onSubmit");
    mount(<KeyValueInputTest submit={submit} />);

    cy.get("input").should("exist");
    cy.findAllByTestId("name-add-row").should("exist").should("be.disabled");

    cy.findAllByTestId("name[0].key").type("key");
    cy.findAllByTestId("name[0].value").type("value");

    cy.findAllByTestId("name-add-row").should("be.enabled");
    cy.findAllByTestId("save").click();
    cy.get("@onSubmit").should("have.been.calledWith", {
      name: [{ key: "key", value: "value" }],
    });
  });

  it("from existing values", () => {
    const submit = cy.spy().as("onSubmit");
    mount(
      <KeyValueInputTest
        submit={submit}
        defaultValues={[{ key: "key1", value: "value1" }]}
      />
    );

    cy.findAllByTestId("name[0].key").should("have.value", "key1");
    cy.findAllByTestId("name[0].value").should("have.value", "value1");

    cy.findAllByTestId("name-add-row").should("be.enabled").click();

    cy.findAllByTestId("name[1].key").type("key2");
    cy.findAllByTestId("name[1].value").type("value2");
    cy.findAllByTestId("save").click();
    cy.get("@onSubmit").should("have.been.calledWith", {
      name: [
        { key: "key1", value: "value1" },
        { key: "key2", value: "value2" },
      ],
    });
  });

  it("leaving it empty", () => {
    const submit = cy.spy().as("onSubmit");
    mount(<KeyValueInputTest submit={submit} />);

    cy.get("input").should("exist");
    cy.findAllByTestId("save").click();
    cy.get("@onSubmit").should("have.been.calledWith", {
      name: [{ key: "", value: "" }],
    });
  });

  it("deleting values", () => {
    const submit = cy.spy().as("onSubmit");
    mount(
      <KeyValueInputTest
        submit={submit}
        defaultValues={[
          { key: "key1", value: "value1" },
          { key: "key2", value: "value2" },
          { key: "key3", value: "value3" },
          { key: "key4", value: "value4" },
        ]}
      />
    );

    cy.findAllByTestId("name[2].remove").click();
    cy.findAllByTestId("name[1].remove").click();
    cy.findAllByTestId("save").click();
    cy.get("@onSubmit").should("have.been.calledWith", {
      name: [
        { key: "key1", value: "value1" },
        { key: "key4", value: "value4" },
      ],
    });
  });
});
