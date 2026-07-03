/**
 * @vitest-environment jsdom
 */
import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  addAlert: vi.fn(),
  addError: vi.fn(),
  hasAccess: vi.fn(() => true),
  listRoles: vi.fn(),
  findDefaultRole: vi.fn(),
  createRole: vi.fn(),
  delRole: vi.fn(),
  tables: [] as any[],
  emptyStates: [] as any[],
  confirms: [] as any[],
  toggleConfirm: vi.fn(),
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, values?: Record<string, unknown>) =>
      values ? `${key}:${JSON.stringify(values)}` : key,
  }),
}));

vi.mock("../admin-client", () => ({
  useAdminClient: () => ({
    adminClient: {
      organizations: {
        listRoles: mocks.listRoles,
        findDefaultRole: mocks.findDefaultRole,
        createRole: mocks.createRole,
        delRole: mocks.delRole,
      },
    },
  }),
}));

vi.mock("../context/access/Access", () => ({
  useAccess: () => ({ hasAccess: mocks.hasAccess }),
}));

vi.mock("../context/realm-context/RealmContext", () => ({
  useRealm: () => ({ realm: "test-realm" }),
}));

vi.mock("../utils/useParams", () => ({
  useParams: () => ({ id: "org-id" }),
}));

vi.mock("../components/confirm-dialog/ConfirmDialog", () => ({
  useConfirmDialog: (config: any) => {
    mocks.confirms.push(config);
    return [mocks.toggleConfirm, () => null];
  },
}));

vi.mock("@keycloak/keycloak-ui-shared", async () => {
  const { useFormContext } = await import("react-hook-form");
  return {
    useAlerts: () => ({
      addAlert: mocks.addAlert,
      addError: mocks.addError,
    }),
    KeycloakDataTable: (props: any) => {
      mocks.tables.push(props);
      return (
        <div data-testid="table">
          {props.toolbarItem}
          {props.emptyState}
        </div>
      );
    },
    ListEmptyState: (props: any) => {
      mocks.emptyStates.push(props);
      return props.onPrimaryAction ? (
        <button onClick={props.onPrimaryAction}>empty-action</button>
      ) : null;
    },
    TextControl: ({ name, rules }: any) => {
      const { register } = useFormContext();
      return <input aria-label={name} {...register(name, rules)} />;
    },
    TextAreaControl: ({ name, rules }: any) => {
      const { register } = useFormContext();
      return <textarea aria-label={name} {...register(name, rules)} />;
    },
  };
});

import { OrganizationRoles } from "./OrganizationRoles";

const renderRoles = (canCreateRole?: boolean) =>
  render(
    <MemoryRouter>
      <OrganizationRoles
        {...(canCreateRole === undefined ? {} : { canCreateRole })}
      />
    </MemoryRouter>,
  );

describe("OrganizationRoles", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
    mocks.tables.length = 0;
    mocks.emptyStates.length = 0;
    mocks.confirms.length = 0;
    mocks.hasAccess.mockReset().mockReturnValue(true);
    mocks.listRoles.mockReset().mockResolvedValue([
      { id: "default-id", name: "default-role" },
      { id: "role-id", name: "role", composite: false },
    ]);
    mocks.findDefaultRole.mockReset().mockResolvedValue({
      id: "default-id",
      name: "default-role",
    });
    mocks.createRole.mockReset().mockResolvedValue({ id: "created-id" });
    mocks.delRole.mockReset().mockResolvedValue(undefined);
  });

  it("loads, renders, creates, and deletes roles", async () => {
    renderRoles();
    const table = mocks.tables.at(-1);
    const rows = await table.loader(0, 10, "role");
    expect(rows.map((row: any) => row.isDefault)).toEqual([true, false]);

    render(
      <MemoryRouter>
        {table.columns[0].cellRenderer(rows[0])}
        {table.columns[0].cellRenderer(rows[1])}
      </MemoryRouter>,
    );
    expect(screen.getByText("default")).toBeTruthy();
    expect(table.actionResolver({ data: rows[0] })).toEqual([]);
    act(() => {
      table.actionResolver({ data: rows[1] })[0].onClick();
    });
    expect(mocks.toggleConfirm).toHaveBeenCalled();

    await act(async () => {
      await mocks.confirms.at(-1).onConfirm();
    });
    expect(mocks.delRole).toHaveBeenCalledWith({
      orgId: "org-id",
      roleId: "role-id",
    });
    expect(mocks.addAlert).toHaveBeenCalled();
    act(() => {
      mocks.emptyStates.at(-1).onPrimaryAction();
    });
    fireEvent.click(screen.getByRole("button", { name: "cancel" }));

    fireEvent.click(screen.getByTestId("create-organization-role"));
    fireEvent.change(screen.getByLabelText("name"), {
      target: { value: "  created  " },
    });
    fireEvent.submit(document.getElementById("create-organization-role-form")!);
    await waitFor(() =>
      expect(mocks.createRole).toHaveBeenCalledWith({
        orgId: "org-id",
        name: "created",
        description: "",
      }),
    );
    expect(mocks.addAlert).toHaveBeenCalled();
  });

  it("reports load, create, and delete errors", async () => {
    renderRoles();
    mocks.listRoles.mockRejectedValueOnce(new Error("load"));
    expect(await mocks.tables.at(-1).loader()).toEqual([]);
    expect(mocks.addError).toHaveBeenCalledWith(
      "organizationRolesLoadError",
      expect.any(Error),
    );

    mocks.delRole.mockRejectedValueOnce(new Error("delete"));
    act(() => {
      mocks.tables
        .at(-1)
        .actionResolver({ data: { id: "role-id", isDefault: false } })[0]
        .onClick();
    });
    await act(async () => {
      await mocks.confirms.at(-1).onConfirm();
    });
    expect(mocks.addError).toHaveBeenCalledWith(
      "roleDeleteError",
      expect.any(Error),
    );

    fireEvent.click(screen.getByTestId("create-organization-role"));
    mocks.createRole.mockRejectedValueOnce(new Error("create"));
    fireEvent.change(screen.getByLabelText("name"), {
      target: { value: "new-role" },
    });
    fireEvent.submit(document.getElementById("create-organization-role-form")!);
    await waitFor(() =>
      expect(mocks.addError).toHaveBeenCalledWith(
        "organizationRoleCreateError",
        expect.any(Error),
      ),
    );
    fireEvent.click(screen.getByRole("button", { name: "cancel" }));
  });

  it("renders a view-only empty state", () => {
    mocks.hasAccess.mockReturnValue(false);
    renderRoles(false);
    const table = mocks.tables.at(-1);
    expect(table.toolbarItem).toBe(false);
    expect(
      table.actionResolver({
        data: { id: "role-id", isDefault: false, access: { manage: false } },
      }),
    ).toEqual([]);
    expect(mocks.emptyStates.at(-1).onPrimaryAction).toBeUndefined();
  });

  it("allows row actions from role access without create access", () => {
    mocks.hasAccess.mockReturnValue(false);
    renderRoles(false);
    const table = mocks.tables.at(-1);
    expect(table.toolbarItem).toBe(false);
    expect(
      table.actionResolver({
        data: { id: "role-id", isDefault: false, access: { manage: true } },
      }),
    ).toHaveLength(1);
  });
});
