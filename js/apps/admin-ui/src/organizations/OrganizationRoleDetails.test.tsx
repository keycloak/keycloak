/**
 * @vitest-environment jsdom
 */
import { Suspense, useState } from "react";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  params: { orgId: "org-id", roleId: "role-id" },
  manager: true,
  viewEvents: true,
  addAlert: vi.fn(),
  addError: vi.fn(),
  findRole: vi.fn(),
  findDefaultRole: vi.fn(),
  updateRole: vi.fn(),
  delRole: vi.fn(),
  navigate: vi.fn(),
  fetchErrors: [] as unknown[],
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, values?: Record<string, unknown>) =>
      values ? `${key}:${JSON.stringify(values)}` : key,
  }),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<any>("react-router-dom");
  return { ...actual, useNavigate: () => mocks.navigate };
});

vi.mock("@patternfly/react-core", async () => {
  const actual = await vi.importActual<any>("@patternfly/react-core");
  return {
    ...actual,
    Tab: ({ children }: any) => <div>{children}</div>,
    TabTitleText: ({ children }: any) => children,
  };
});

vi.mock("../admin-client", () => ({
  useAdminClient: () => ({
    adminClient: {
      organizations: {
        findRole: mocks.findRole,
        findDefaultRole: mocks.findDefaultRole,
        updateRole: mocks.updateRole,
        delRole: mocks.delRole,
      },
    },
  }),
}));

vi.mock("../context/access/Access", () => ({
  useAccess: () => ({
    hasAccess: (access: string) =>
      access === "manage-organizations" ? mocks.manager : mocks.viewEvents,
  }),
}));

vi.mock("../context/realm-context/RealmContext", () => ({
  useRealm: () => ({ realm: "test-realm" }),
}));

vi.mock("../utils/useParams", () => ({
  useParams: () => mocks.params,
}));

vi.mock("../components/confirm-dialog/ConfirmDialog", async () => {
  const { useState } = await import("react");

  return {
    useConfirmDialog: (config: any) => {
      const [open, setOpen] = useState(false);
      const Dialog = () =>
        open ? (
          <div role="dialog">
            <button
              onClick={async () => {
                await config.onConfirm();
                setOpen(false);
              }}
            >
              {config.continueButtonLabel}
            </button>
            <button onClick={() => setOpen(false)}>cancel</button>
          </div>
        ) : null;

      return [() => setOpen(true), Dialog];
    },
  };
});

vi.mock("../components/role-form/RoleForm", () => ({
  RoleForm: (props: any) => {
    const [name, setName] = useState(props.form.getValues("name") ?? "");
    const [description, setDescription] = useState(
      props.form.getValues("description") ?? "",
    );

    return (
      <form
        aria-label="role-form"
        onSubmit={(event) => {
          event.preventDefault();
          props.onSubmit({
            name,
            description,
            attributes: [{ key: "two", value: "2" }],
          });
        }}
      >
        <input
          aria-label="name"
          disabled={props.isReadOnly}
          value={name}
          onChange={(event) => setName(event.currentTarget.value)}
        />
        <textarea
          aria-label="description"
          disabled={props.isReadOnly}
          value={description}
          onChange={(event) => setDescription(event.currentTarget.value)}
        />
        <span data-testid="role-form-mode">
          {props.isReadOnly ? "read-only" : "editable"}
        </span>
        <button type="submit">save</button>
        <a data-testid="cancel" href={props.cancelLink.pathname}>
          cancel
        </a>
      </form>
    );
  },
}));

vi.mock("../components/key-value-form/AttributeForm", () => ({
  AttributesForm: (props: any) => (
    <div data-testid="attributes-form">
      <button onClick={props.reset}>reset-attributes</button>
    </div>
  ),
}));

vi.mock("../components/routable-tabs/RoutableTabs", () => ({
  RoutableTabs: ({ children }: any) => <div>{children}</div>,
  useRoutableTab: () => ({}),
}));

vi.mock("../components/view-header/ViewHeader", () => ({
  ViewHeader: (props: any) => (
    <header>
      <h1>{props.titleKey}</h1>
      {props.badges?.map((badge: any) => (
        <span key={badge.id}>{badge.text}</span>
      ))}
      <div>{props.dropdownItems}</div>
    </header>
  ),
}));

vi.mock("./OrganizationRoleComposites", () => ({
  OrganizationRoleComposites: (props: any) => (
    <div data-testid="composites">
      {props.roleId}:{props.canManage ? "manage" : "view"}
    </div>
  ),
}));

vi.mock("./OrganizationRoleUsers", () => ({
  OrganizationRoleUsers: (props: any) => (
    <div data-testid="users">
      {props.roleId}:{props.canMapRole ? "map" : "view"}
    </div>
  ),
}));

vi.mock("../events/AdminEvents", () => ({
  AdminEvents: (props: any) => (
    <div data-testid="events">{props.resourcePath}</div>
  ),
}));

vi.mock("@keycloak/keycloak-ui-shared", async () => {
  const { useEffect } = await import("react");
  return {
    KeycloakSpinner: () => <div>loading</div>,
    useAlerts: () => ({ addAlert: mocks.addAlert, addError: mocks.addError }),
    useFetch: (
      request: () => Promise<unknown>,
      success: (value: any) => void,
      deps: unknown[],
    ) => {
      useEffect(() => {
        request()
          .then(success)
          .catch((error) => mocks.fetchErrors.push(error));
      }, deps);
    },
  };
});

import OrganizationRoleDetails from "./OrganizationRoleDetails";
import organizationRoutes from "./routes";
import {
  OrganizationRoleRoute,
  toOrganizationRole,
} from "./routes/OrganizationRole";

const role = {
  id: "role-id",
  name: "role-name",
  description: "description",
  composite: true,
  attributes: { one: ["1"] },
};

const renderDetails = () =>
  render(
    <MemoryRouter>
      <OrganizationRoleDetails />
    </MemoryRouter>,
  );

const confirmDelete = () => {
  fireEvent.click(
    within(screen.getByRole("dialog")).getByRole("button", { name: "delete" }),
  );
};

describe("OrganizationRoleDetails", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
    mocks.params = { orgId: "org-id", roleId: "role-id" };
    mocks.manager = true;
    mocks.viewEvents = true;
    mocks.fetchErrors.length = 0;
    mocks.findRole.mockResolvedValue(role);
    mocks.findDefaultRole.mockResolvedValue({ ...role, id: "default-id" });
    mocks.updateRole.mockResolvedValue(undefined);
    mocks.delRole.mockResolvedValue(undefined);
  });

  it("loads a regular role and renders connected tabs", async () => {
    renderDetails();

    await screen.findByRole("form", { name: "role-form" });
    expect(mocks.findRole).toHaveBeenCalledWith({
      orgId: "org-id",
      roleId: "role-id",
    });
    expect(screen.getByTestId("composites").textContent).toBe("role-id:manage");
    expect(screen.getByTestId("users").textContent).toBe("role-id:map");
    expect(screen.getByTestId("events").textContent).toContain("role-id");
  });

  it("submits updates from the visible role form", async () => {
    renderDetails();

    await screen.findByRole("form", { name: "role-form" });
    fireEvent.change(screen.getByLabelText("name"), {
      target: { value: "  role-name  " },
    });
    fireEvent.change(screen.getByLabelText("description"), {
      target: { value: "updated" },
    });
    fireEvent.submit(screen.getByRole("form", { name: "role-form" }));

    await waitFor(() =>
      expect(mocks.updateRole).toHaveBeenCalledWith(
        { orgId: "org-id", roleId: "role-id" },
        expect.objectContaining({
          name: "role-name",
          description: "updated",
          attributes: { two: ["2"] },
        }),
      ),
    );
    expect(mocks.addAlert).toHaveBeenCalled();
    fireEvent.click(screen.getByText("reset-attributes"));
  });

  it("deletes a regular role from the visible header action", async () => {
    renderDetails();

    await screen.findByRole("form", { name: "role-form" });
    fireEvent.click(screen.getByRole("menuitem", { name: "deleteRole" }));
    confirmDelete();

    await waitFor(() => expect(mocks.delRole).toHaveBeenCalled());
    expect(mocks.navigate).toHaveBeenCalled();
  });

  it("reports update and delete errors", async () => {
    renderDetails();
    await screen.findByRole("form", { name: "role-form" });

    mocks.updateRole.mockRejectedValueOnce(new Error("update"));
    fireEvent.submit(screen.getByRole("form", { name: "role-form" }));
    await waitFor(() =>
      expect(mocks.addError).toHaveBeenCalledWith(
        "organizationRoleSaveError",
        expect.any(Error),
      ),
    );

    mocks.delRole.mockRejectedValueOnce(new Error("delete"));
    fireEvent.click(screen.getByRole("menuitem", { name: "deleteRole" }));
    confirmDelete();
    await waitFor(() =>
      expect(mocks.addError).toHaveBeenCalledWith(
        "roleDeleteError",
        expect.any(Error),
      ),
    );
  });

  it("loads a protected default role in view-only mode", async () => {
    mocks.params = { orgId: "org-id", roleId: "default" };
    mocks.manager = false;
    mocks.viewEvents = false;
    renderDetails();

    await waitFor(() => expect(mocks.findDefaultRole).toHaveBeenCalled());
    await screen.findByRole("form", { name: "role-form" });
    expect(screen.queryByTestId("delete-organization-role")).toBeNull();
    expect(screen.queryByTestId("attributes-form")).toBeNull();
    expect(screen.queryByTestId("users")).toBeNull();
    expect(screen.queryByTestId("events")).toBeNull();
    expect(screen.getByTestId("composites").textContent).toBe(
      "default-id:view",
    );
    expect(screen.getByTestId("role-form-mode").textContent).toBe("read-only");
  });

  it("uses role access when legacy organization management is absent", async () => {
    mocks.manager = false;
    mocks.findRole.mockResolvedValueOnce({
      ...role,
      access: { manage: true, mapRole: false },
    });
    renderDetails();

    await screen.findByRole("form", { name: "role-form" });
    expect(screen.getByTestId("delete-organization-role")).toBeTruthy();
    expect(screen.getByTestId("role-form-mode").textContent).toBe("editable");
    expect(screen.getByTestId("composites").textContent).toBe("role-id:manage");
    expect(screen.getByTestId("users").textContent).toBe("role-id:view");
  });

  it("handles a concurrently removed role", async () => {
    mocks.findRole.mockResolvedValueOnce(null);
    renderDetails();

    await waitFor(() => expect(mocks.fetchErrors).toHaveLength(1));
    expect((mocks.fetchErrors[0] as Error).message).toBe("notFound");
  });

  it("builds and lazy-loads the organization role route", async () => {
    expect(
      toOrganizationRole({
        realm: "realm",
        orgId: "org",
        roleId: "role",
        tab: "details",
      }).pathname,
    ).toBe("/realm/organizations/org/roles/role/details");
    expect(
      OrganizationRoleRoute.handle.breadcrumb!(((key: string) => key) as any),
    ).toBe("organizationRoleDetails");
    expect(organizationRoutes).toContain(OrganizationRoleRoute);
    render(
      <Suspense fallback={null}>{OrganizationRoleRoute.element}</Suspense>,
    );
    await waitFor(() => expect(mocks.findRole).toHaveBeenCalled());
  });
});
