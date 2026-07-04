/**
 * @vitest-environment jsdom
 */
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
  addAlert: vi.fn(),
  addError: vi.fn(),
  hasAccess: vi.fn(() => true),
  listRoles: vi.fn(),
  findDefaultRole: vi.fn(),
  createRole: vi.fn(),
  delRole: vi.fn(),
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
            <button
              onClick={() => {
                config.onCancel?.();
                setOpen(false);
              }}
            >
              cancel
            </button>
          </div>
        ) : null;

      return [() => setOpen(true), Dialog];
    },
  };
});

vi.mock("@keycloak/keycloak-ui-shared", async () => {
  const { useEffect, useState } = await import("react");
  const { useFormContext } = await import("react-hook-form");

  const rowLabel = (row: any) => row.name ?? row.id;

  return {
    useAlerts: () => ({
      addAlert: mocks.addAlert,
      addError: mocks.addError,
    }),
    KeycloakDataTable: (props: any) => {
      const [rows, setRows] = useState<any[]>([]);

      useEffect(() => {
        let mounted = true;
        props.loader?.().then((loadedRows: any[]) => {
          if (mounted) {
            setRows(loadedRows);
          }
        });
        return () => {
          mounted = false;
        };
      }, []);

      return (
        <div data-testid={props.ariaLabelKey}>
          {props.toolbarItem}
          {rows.length === 0 && props.emptyState}
          {rows.map((row) => {
            const actions = props.actionResolver?.({ data: row }) ?? [];
            return (
              <div key={row.id} role="row" aria-label={rowLabel(row)}>
                {props.columns.map((column: any) => (
                  <span key={column.name}>
                    {column.cellRenderer
                      ? column.cellRenderer(row)
                      : String(row[column.name] ?? "")}
                  </span>
                ))}
                {actions.map((action: any) => (
                  <button key={action.title} onClick={action.onClick}>
                    {action.title}
                  </button>
                ))}
              </div>
            );
          })}
        </div>
      );
    },
    ListEmptyState: (props: any) => (
      <>
        <div data-testid="empty-state">{props.message}</div>
        {props.onPrimaryAction && (
          <button onClick={props.onPrimaryAction}>
            {props.primaryActionText}
          </button>
        )}
      </>
    ),
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

  it("renders loaded rows and deletes a manageable non-default role", async () => {
    renderRoles();

    const defaultRow = await screen.findByRole("row", {
      name: "default-role",
    });
    expect(within(defaultRow).getByText("default")).toBeTruthy();
    expect(within(defaultRow).queryByRole("button", { name: "delete" })).toBe(
      null,
    );

    const roleRow = screen.getByRole("row", { name: "role" });
    fireEvent.click(within(roleRow).getByRole("button", { name: "delete" }));
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: "delete",
      }),
    );

    await waitFor(() =>
      expect(mocks.delRole).toHaveBeenCalledWith({
        orgId: "org-id",
        roleId: "role-id",
      }),
    );
    expect(mocks.addAlert).toHaveBeenCalled();
  });

  it("creates roles from visible controls", async () => {
    renderRoles();

    fireEvent.click(await screen.findByTestId("create-organization-role"));
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

  it("opens the create role modal from the empty state", async () => {
    mocks.listRoles.mockResolvedValueOnce([]);
    mocks.findDefaultRole.mockResolvedValueOnce(undefined);
    renderRoles();

    const createButtons = await screen.findAllByRole("button", {
      name: "createRole",
    });
    fireEvent.click(createButtons.at(-1)!);
    expect(screen.getByLabelText("name")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "cancel" }));
  });

  it("reports load, create, and delete errors", async () => {
    mocks.listRoles.mockRejectedValueOnce(new Error("load"));
    renderRoles();
    await waitFor(() =>
      expect(mocks.addError).toHaveBeenCalledWith(
        "organizationRolesLoadError",
        expect.any(Error),
      ),
    );

    cleanup();
    mocks.listRoles.mockResolvedValueOnce([
      { id: "role-id", name: "role", composite: false },
    ]);
    mocks.findDefaultRole.mockResolvedValueOnce({ id: "default-id" });
    mocks.delRole.mockRejectedValueOnce(new Error("delete"));
    renderRoles();
    const roleRow = await screen.findByRole("row", { name: "role" });
    fireEvent.click(within(roleRow).getByRole("button", { name: "delete" }));
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: "delete",
      }),
    );
    await waitFor(() =>
      expect(mocks.addError).toHaveBeenCalledWith(
        "roleDeleteError",
        expect.any(Error),
      ),
    );

    mocks.createRole.mockRejectedValueOnce(new Error("create"));
    fireEvent.click(screen.getByTestId("create-organization-role"));
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
  });

  it("renders view-only controls without create or delete actions", async () => {
    mocks.hasAccess.mockReturnValue(false);
    renderRoles(false);

    expect(screen.queryByTestId("create-organization-role")).toBeNull();
    const row = await screen.findByRole("row", { name: "role" });
    expect(within(row).queryByRole("button", { name: "delete" })).toBeNull();
  });

  it("allows row actions from role access without create access", async () => {
    mocks.hasAccess.mockReturnValue(false);
    mocks.listRoles.mockResolvedValueOnce([
      { id: "role-id", name: "role", access: { manage: true } },
    ]);
    renderRoles(false);

    expect(screen.queryByTestId("create-organization-role")).toBeNull();
    const row = await screen.findByRole("row", { name: "role" });
    expect(within(row).getByRole("button", { name: "delete" })).toBeTruthy();
  });
});
