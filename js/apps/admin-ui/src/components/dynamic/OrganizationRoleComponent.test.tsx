/**
 * @vitest-environment jsdom
 */
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { PropsWithChildren } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  listRoles: vi.fn(),
  findRole: vi.fn(),
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock("../../admin-client", () => ({
  useAdminClient: () => ({
    adminClient: {
      organizations: {
        listRoles: mocks.listRoles,
        findRole: mocks.findRole,
      },
    },
  }),
}));

vi.mock("@keycloak/keycloak-ui-shared", async () => {
  const React = await import("react");
  const { useFormContext, useWatch } = await import("react-hook-form");
  const actual = await vi.importActual<
    typeof import("@keycloak/keycloak-ui-shared")
  >("@keycloak/keycloak-ui-shared");

  return {
    ...actual,
    useFetch: (
      request: () => Promise<unknown>,
      callback: (result: unknown) => void,
      dependencies: React.DependencyList,
    ) => {
      const requestRef = React.useRef(request);
      const callbackRef = React.useRef(callback);
      requestRef.current = request;
      callbackRef.current = callback;
      const dependencyKey = JSON.stringify(dependencies);
      React.useEffect(() => {
        let active = true;
        void requestRef
          .current()
          .then((result) => active && callbackRef.current(result));
        return () => {
          active = false;
        };
      }, [dependencyKey]);
    },
    SelectControl: (props: any) => {
      const { register, control } = useFormContext();
      const value = useWatch({ control, name: props.name });
      return (
        <>
          <select
            aria-label={props.label}
            disabled={props.isDisabled}
            {...register(props.name, props.controller?.rules)}
          >
            <option value="" />
            {props.options.map((option: any) => (
              <option key={option.key} value={option.key}>
                {option.value}
              </option>
            ))}
          </select>
          <input
            aria-label="role-filter"
            onChange={(event) => props.onFilter(event.target.value)}
          />
          <span data-testid="selected-role">
            {props.selectedOptions?.[0]?.value}
          </span>
          <span data-testid="field-value">{value}</span>
        </>
      );
    },
  };
});

import { GroupsResourceContext } from "../../context/group-resource/GroupResourceContext";
import { COMPONENTS, isValidComponentType } from "./components";
import { OrganizationRoleComponent } from "./OrganizationRoleComponent";

type TestFormProps = PropsWithChildren & {
  organizationId?: string;
  roleId?: string;
  required?: boolean;
};

const TestForm = ({
  organizationId,
  roleId,
  required = true,
}: TestFormProps) => {
  const form = useForm({
    defaultValues: { config: { organizationRole: roleId ?? "" } },
  });

  return (
    <FormProvider {...form}>
      <GroupsResourceContext.Provider
        value={{ getOrgId: () => organizationId } as any}
      >
        <OrganizationRoleComponent
          name="organizationRole"
          label="Organization Role"
          helpText="Select an organization role"
          required={required}
          convertToName={(name) => `config.${name}`}
        />
      </GroupsResourceContext.Provider>
    </FormProvider>
  );
};

describe("OrganizationRoleComponent", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
    mocks.listRoles.mockResolvedValue([
      { id: "member-id", name: "Member" },
      { id: "admin-id", name: "Admin" },
      { name: "Missing id" },
      { id: "missing-name" },
    ]);
    mocks.findRole.mockResolvedValue({ id: "admin-id", name: "Admin" });
  });

  it("registers the organization role dynamic component", () => {
    expect(isValidComponentType("OrganizationRole")).toBe(true);
    expect(COMPONENTS.OrganizationRole).toBe(OrganizationRoleComponent);
  });

  it("loads, searches and selects roles from the linked organization", async () => {
    render(<TestForm organizationId="org-id" roleId="admin-id" />);

    await waitFor(() =>
      expect(mocks.listRoles).toHaveBeenCalledWith({
        orgId: "org-id",
        max: 20,
        search: undefined,
      }),
    );
    await waitFor(() =>
      expect(mocks.findRole).toHaveBeenCalledWith({
        orgId: "org-id",
        roleId: "admin-id",
      }),
    );

    const select = screen.getByLabelText(
      "Organization Role",
    ) as HTMLSelectElement;
    expect(screen.getByTestId("selected-role").textContent).toBe("Admin");
    expect(screen.queryByText("Missing id")).toBeNull();

    fireEvent.change(select, { target: { value: "member-id" } });
    expect(screen.getByTestId("field-value").textContent).toBe("member-id");

    fireEvent.change(screen.getByLabelText("role-filter"), {
      target: { value: "mem" },
    });
    await waitFor(() =>
      expect(mocks.listRoles).toHaveBeenLastCalledWith({
        orgId: "org-id",
        max: 20,
        search: "mem",
      }),
    );
  });

  it("disables selection when the identity provider has no organization", async () => {
    render(<TestForm required={false} />);

    const select = screen.getByLabelText(
      "Organization Role",
    ) as HTMLSelectElement;
    expect(select.disabled).toBe(true);
    await waitFor(() => expect(mocks.listRoles).not.toHaveBeenCalled());
    expect(mocks.findRole).not.toHaveBeenCalled();
  });
});
