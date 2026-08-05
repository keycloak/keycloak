/**
 * @vitest-environment jsdom
 */
import { render } from "@testing-library/react";
import { FormProvider, useForm } from "react-hook-form";
import { MemoryRouter } from "react-router-dom";
import { expect, it, vi } from "vitest";
import type { AttributeForm } from "../key-value-form/AttributeForm";

const mocks = vi.hoisted(() => ({
  formAccess: vi.fn(),
  textControls: [] as any[],
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock("@keycloak/keycloak-ui-shared", () => ({
  FormSubmitButton: ({ children }: any) => <button>{children}</button>,
  TextAreaControl: () => null,
  TextControl: (props: any) => {
    mocks.textControls.push(props);
    return null;
  },
}));

vi.mock("../form/FormAccess", () => ({
  FormAccess: (props: any) => {
    mocks.formAccess(props);
    return <form>{props.children}</form>;
  },
}));

vi.mock("../view-header/ViewHeader", () => ({ ViewHeader: () => null }));

import { RoleForm } from "./RoleForm";

const ReadOnlyRoleForm = () => {
  const form = useForm<AttributeForm>({
    defaultValues: { name: "role" },
  });
  return (
    <MemoryRouter>
      <FormProvider {...form}>
        <RoleForm
          form={form}
          onSubmit={vi.fn()}
          cancelLink="/roles"
          role="manage-organizations"
          editMode
          isReadOnly
        />
      </FormProvider>
    </MemoryRouter>
  );
};

it("passes organization role read-only state to FormAccess", () => {
  render(<ReadOnlyRoleForm />);
  expect(mocks.formAccess).toHaveBeenCalledWith(
    expect.objectContaining({
      role: "manage-organizations",
      isReadOnly: true,
    }),
  );
  expect(mocks.textControls[0].rules.validate(" ")).toBe("required");
});
