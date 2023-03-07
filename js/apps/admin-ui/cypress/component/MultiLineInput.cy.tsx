import {
  ActionGroup,
  Button,
  Form,
  FormGroup,
  Page,
  PageSection,
} from "@patternfly/react-core";
import { mount } from "cypress/react";
import { useEffect } from "react";
import { FormProvider, useForm } from "react-hook-form";
import {
  MultiLineInput,
  MultiLineInputProps,
} from "../../src/components/multi-line-input/MultiLineInput";

type MultiLineInputTestProps = Omit<MultiLineInputProps, "name"> & {
  submit: (values: any) => void;
  defaultValues?: string | string[];
};

describe("MultiLineInput", () => {
  const MultiLineInputTest = ({
    submit,
    defaultValues,
    ...rest
  }: MultiLineInputTestProps) => {
    const form = useForm();

    useEffect(() => {
      form.setValue("name", defaultValues || "");
    }, [form.setValue]);

    return (
      <Page>
        <PageSection variant="light">
          <FormProvider {...form}>
            <Form isHorizontal onSubmit={form.handleSubmit(submit)}>
              <FormGroup label="Test field" fieldId="name">
                <MultiLineInput
                  id="name"
                  name="name"
                  aria-label="test"
                  addButtonLabel="Add"
                  {...rest}
                />
              </FormGroup>
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

  it("basic interaction", () => {
    const submit = cy.spy().as("onSubmit");
    mount(<MultiLineInputTest submit={submit} />);

    cy.get("input").should("exist");
    cy.findByTestId("name0").type("value");
    cy.findAllByTestId("save").click();
    cy.get("@onSubmit").should("have.been.calledWith", { name: ["value"] });
  });

  it("add values", () => {
    const submit = cy.spy().as("onSubmit");
    mount(<MultiLineInputTest submit={submit} />);

    cy.findByTestId("name0").type("value");
    cy.findByTestId("addValue").click();
    cy.findByTestId("name1").type("value1");
    cy.findByTestId("addValue").click();
    cy.findByTestId("name2").type("value2");
    cy.findAllByTestId("save").click();
    cy.get("@onSubmit").should("have.been.calledWith", {
      name: ["value", "value1", "value2"],
    });
  });

  it("from existing values as string", () => {
    const submit = cy.spy().as("onSubmit");
    mount(
      <MultiLineInputTest
        submit={submit}
        defaultValues="one##two##three"
        stringify
      />
    );

    cy.findByTestId("name0").should("have.value", "one");
    cy.findByTestId("name1").should("have.value", "two");
    cy.findByTestId("name2").should("have.value", "three");
    cy.findByTestId("addValue").click();
    cy.findByTestId("name3").type("four");
    cy.findAllByTestId("save").click();
    cy.get("@onSubmit").should("have.been.calledWith", {
      name: "one##two##three##four",
    });
  });

  it("from existing values as string[]", () => {
    const submit = cy.spy().as("onSubmit");
    mount(
      <MultiLineInputTest
        submit={submit}
        defaultValues={["one", "two", "three"]}
      />
    );

    cy.findByTestId("name0").should("have.value", "one");
    cy.findByTestId("name1").should("have.value", "two");
    cy.findByTestId("name2").should("have.value", "three");
    cy.findByTestId("remove0").click();
    cy.findAllByTestId("save").click();
    cy.get("@onSubmit").should("have.been.calledWith", {
      name: ["two", "three"],
    });
  });

  it("remove test", () => {
    const submit = cy.spy().as("onSubmit");
    mount(
      <MultiLineInputTest
        submit={submit}
        defaultValues={["one", "two", "three", "four", "five", "six"]}
      />
    );

    cy.findByTestId("remove0").click();
    cy.findByTestId("remove2").click();
    cy.findByTestId("remove2").click();
    cy.findAllByTestId("save").click();
    cy.get("@onSubmit").should("have.been.calledWith", {
      name: ["two", "three", "six"],
    });
  });

  it("add / update test", () => {
    const submit = cy.spy().as("onSubmit");
    mount(<MultiLineInputTest submit={submit} defaultValues={["one"]} />);

    cy.findByTestId("name0").type("-one");
    cy.findByTestId("addValue").click();
    cy.findByTestId("name1").type("twos");
    cy.findAllByTestId("save").click();
    cy.get("@onSubmit").should("have.been.calledWith", {
      name: ["one-one", "twos"],
    });
  });
});
