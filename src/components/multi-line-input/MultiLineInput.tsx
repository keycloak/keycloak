import React from "react";
import { useFieldArray, UseFormMethods } from "react-hook-form";
import {
  TextInput,
  Split,
  SplitItem,
  Button,
  ButtonVariant,
} from "@patternfly/react-core";
import { MinusIcon, PlusIcon } from "@patternfly/react-icons";

export type MultiLineInputProps = {
  form: UseFormMethods;
  name: string;
};

export const MultiLineInput = ({ name, form }: MultiLineInputProps) => {
  const { register, control } = form;
  const { fields, append, remove } = useFieldArray({
    name,
    control,
  });
  return (
    <>
      {fields.map(({ id, value }, index) => (
        <Split key={id}>
          <SplitItem>
            <TextInput
              ref={register()}
              name={`${name}[${index}].value`}
              defaultValue={value}
            />
          </SplitItem>
          <SplitItem>
            {index === fields.length - 1 && (
              <Button
                variant={ButtonVariant.link}
                onClick={() => append({})}
                tabIndex={-1}
              >
                <PlusIcon />
              </Button>
            )}
            {index !== fields.length - 1 && (
              <Button
                variant={ButtonVariant.link}
                onClick={() => remove(index)}
                tabIndex={-1}
              >
                <MinusIcon />
              </Button>
            )}
          </SplitItem>
        </Split>
      ))}
    </>
  );
};
