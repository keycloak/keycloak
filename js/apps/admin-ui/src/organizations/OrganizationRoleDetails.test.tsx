/**
 * @vitest-environment jsdom
 */
import { Suspense } from "react";
import { act, cleanup, render, waitFor } from "@testing-library/react";
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
  roleForms: [] as any[],
  attributeForms: [] as any[],
  headers: [] as any[],
  composites: [] as any[],
  users: [] as any[],
  events: [] as any[],
  confirms: [] as any[],
  toggleConfirm: vi.fn(),
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

vi.mock("../components/confirm-dialog/ConfirmDialog", () => ({
  useConfirmDialog: (config: any) => {
    mocks.confirms.push(config);
    return [mocks.toggleConfirm, () => null];
  },
}));

vi.mock("../components/role-form/RoleForm", () => ({
  RoleForm: (props: any) => {
    mocks.roleForms.push(props);
    return null;
  },
}));

vi.mock("../components/key-value-form/AttributeForm", () => ({
  AttributesForm: (props: any) => {
    mocks.attributeForms.push(props);
    return null;
  },
}));

vi.mock("../components/routable-tabs/RoutableTabs", () => ({
  RoutableTabs: ({ children }: any) => <div>{children}</div>,
  useRoutableTab: () => ({}),
}));

vi.mock("../components/view-header/ViewHeader", () => ({
  ViewHeader: (props: any) => {
    mocks.headers.push(props);
    return <div>{props.dropdownItems}</div>;
  },
}));

vi.mock("./OrganizationRoleComposites", () => ({
  OrganizationRoleComposites: (props: any) => {
    mocks.composites.push(props);
    return null;
  },
}));

vi.mock("./OrganizationRoleUsers", () => ({
  OrganizationRoleUsers: (props: any) => {
    mocks.users.push(props);
    return null;
  },
}));

vi.mock("../events/AdminEvents", () => ({
  AdminEvents: (props: any) => {
    mocks.events.push(props);
    return null;
  },
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

describe("OrganizationRoleDetails", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
    mocks.params = { orgId: "org-id", roleId: "role-id" };
    mocks.manager = true;
    mocks.viewEvents = true;
    mocks.fetchErrors.length = 0;
    mocks.roleForms.length = 0;
    mocks.attributeForms.length = 0;
    mocks.headers.length = 0;
    mocks.composites.length = 0;
    mocks.users.length = 0;
    mocks.events.length = 0;
    mocks.confirms.length = 0;
    mocks.findRole.mockResolvedValue(role);
    mocks.findDefaultRole.mockResolvedValue({ ...role, id: "default-id" });
    mocks.updateRole.mockResolvedValue(undefined);
    mocks.delRole.mockResolvedValue(undefined);
  });

  it("loads, updates, resets, and deletes a regular role", async () => {
    renderDetails();
    await waitFor(() => expect(mocks.roleForms.length).toBeGreaterThan(0));
    expect(mocks.findRole).toHaveBeenCalledWith({
      orgId: "org-id",
      roleId: "role-id",
    });
    expect(mocks.composites.at(-1)).toEqual(
      expect.objectContaining({ roleId: "role-id", isManager: true }),
    );
    expect(mocks.users.at(-1)).toEqual(
      expect.objectContaining({ roleId: "role-id", isManager: true }),
    );
    expect(mocks.events.at(-1).resourcePath).toContain("role-id");

    await act(() =>
      mocks.roleForms.at(-1).onSubmit({
        name: "  role-name  ",
        description: "updated",
        attributes: [{ key: "two", value: "2" }],
      }),
    );
    expect(mocks.updateRole).toHaveBeenCalledWith(
      { orgId: "org-id", roleId: "role-id" },
      expect.objectContaining({
        name: "role-name",
        attributes: { two: ["2"] },
      }),
    );
    mocks.attributeForms.at(-1).reset();

    mocks.headers.at(-1).dropdownItems[0].props.onClick();
    expect(mocks.toggleConfirm).toHaveBeenCalled();
    await act(() => mocks.confirms.at(-1).onConfirm());
    expect(mocks.delRole).toHaveBeenCalled();
    expect(mocks.navigate).toHaveBeenCalled();
  });

  it("reports update and delete errors", async () => {
    renderDetails();
    await waitFor(() => expect(mocks.roleForms.length).toBeGreaterThan(0));
    mocks.updateRole.mockRejectedValueOnce(new Error("update"));
    await act(() =>
      mocks.roleForms.at(-1).onSubmit({ name: "role-name", attributes: [] }),
    );
    mocks.delRole.mockRejectedValueOnce(new Error("delete"));
    await act(() => mocks.confirms.at(-1).onConfirm());
    expect(mocks.addError).toHaveBeenCalledTimes(2);
  });

  it("loads a protected default role in view-only mode", async () => {
    mocks.params = { orgId: "org-id", roleId: "default" };
    mocks.manager = false;
    mocks.viewEvents = false;
    renderDetails();
    await waitFor(() => expect(mocks.findDefaultRole).toHaveBeenCalled());
    await waitFor(() => expect(mocks.roleForms.length).toBeGreaterThan(0));
    expect(mocks.headers.at(-1).dropdownItems).toBeUndefined();
    expect(mocks.attributeForms).toHaveLength(0);
    expect(mocks.events).toHaveLength(0);
    expect(mocks.composites.at(-1).isManager).toBe(false);
    expect(mocks.roleForms.at(-1).isReadOnly).toBe(true);
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
