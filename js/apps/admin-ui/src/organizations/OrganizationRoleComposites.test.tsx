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
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  addAlert: vi.fn(),
  addError: vi.fn(),
  listComposites: vi.fn(),
  listAvailableComposites: vi.fn(),
  listEffectiveComposites: vi.fn(),
  addComposites: vi.fn(),
  delComposites: vi.fn(),
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
        listRoleComposites: mocks.listComposites,
        listAvailableRoleComposites: mocks.listAvailableComposites,
        listEffectiveRoleComposites: mocks.listEffectiveComposites,
        addRoleComposites: mocks.addComposites,
        delRoleComposites: mocks.delComposites,
      },
    },
  }),
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

  const rowLabel = (row: any) => row.name ?? row.id;

  return {
    useAlerts: () => ({ addAlert: mocks.addAlert, addError: mocks.addError }),
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
            const label = rowLabel(row);
            const disabled = props.isRowDisabled?.(row) ?? false;
            return (
              <div key={row.id} role="row" aria-label={label}>
                {props.onSelect && (
                  <input
                    aria-label={`Select ${label}`}
                    type="checkbox"
                    disabled={disabled}
                    onChange={(event) =>
                      props.onSelect(event.currentTarget.checked ? [row] : [])
                    }
                  />
                )}
                {props.columns.map((column: any) => (
                  <span key={column.name}>
                    {column.cellRenderer
                      ? column.cellRenderer(row)
                      : String(row[column.name] ?? "")}
                  </span>
                ))}
                {props.actions?.map((action: any) => (
                  <button
                    key={action.title}
                    disabled={disabled}
                    onClick={() => action.onRowClick(row)}
                  >
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
        {props.secondaryActions?.map((action: any) => (
          <button key={action.text} onClick={action.onClick}>
            {action.text}
          </button>
        ))}
      </>
    ),
  };
});

import {
  AddOrganizationRoleCompositeModal,
  OrganizationRoleComposites,
} from "./OrganizationRoleComposites";

const renderComposites = (canManage = true) =>
  render(
    <OrganizationRoleComposites
      organizationId="org-id"
      roleId="role-id"
      roleName="parent"
      canManage={canManage}
    />,
  );

const confirmDialog = (name: string) => {
  fireEvent.click(
    within(screen.getByRole("dialog")).getByRole("button", { name }),
  );
};

describe("OrganizationRoleComposites", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
    mocks.listComposites.mockReset().mockResolvedValue([]);
    mocks.listAvailableComposites.mockReset().mockResolvedValue([]);
    mocks.listEffectiveComposites.mockReset().mockResolvedValue([]);
    mocks.addComposites.mockReset().mockResolvedValue(undefined);
    mocks.delComposites.mockReset().mockResolvedValue(undefined);
  });

  it("loads direct composites and removes a selected role", async () => {
    mocks.listComposites.mockResolvedValueOnce([
      { id: "org-child", containerId: "org-id", name: "org" },
      { id: "realm-child", containerId: "realm-id", name: "realm" },
      { id: "client-child", clientRole: true, name: "client" },
    ]);

    renderComposites();

    const orgRow = await screen.findByRole("row", { name: "org" });
    expect(screen.getByText("organizationRole")).toBeTruthy();
    expect(screen.getByText("realmRole")).toBeTruthy();
    expect(screen.getByText("clientRole")).toBeTruthy();

    fireEvent.click(within(orgRow).getByLabelText("Select org"));
    fireEvent.click(screen.getAllByRole("button", { name: "unAssignRole" })[0]);
    confirmDialog("remove");

    await waitFor(() =>
      expect(mocks.delComposites).toHaveBeenCalledWith(
        { orgId: "org-id", roleId: "role-id" },
        [{ id: "org-child", containerId: "org-id", name: "org" }],
      ),
    );
  });

  it("opens the assignment modal and assigns an available role", async () => {
    mocks.listAvailableComposites.mockResolvedValueOnce([
      { id: "available", name: "available", clientName: "not-sent" },
    ]);

    renderComposites();
    fireEvent.click(
      screen.getAllByRole("button", { name: "addAssociatedRoles" })[0],
    );
    expect(screen.getByText(/addAssociatedRolesTo/)).toBeTruthy();

    const availableRow = await screen.findByRole("row", { name: "available" });
    fireEvent.click(within(availableRow).getByLabelText("Select available"));
    fireEvent.click(screen.getByTestId("assign-organization-role-composites"));

    await waitFor(() =>
      expect(mocks.addComposites).toHaveBeenCalledWith(
        { orgId: "org-id", roleId: "role-id" },
        [{ id: "available", name: "available" }],
      ),
    );
  });

  it("loads every source in the assignment modal", async () => {
    const onAssign = vi.fn().mockResolvedValue(true);
    render(
      <AddOrganizationRoleCompositeModal
        organizationId="org-id"
        roleId="role-id"
        roleName="parent"
        onAssign={onAssign}
        onClose={vi.fn()}
      />,
    );

    await waitFor(() =>
      expect(mocks.listAvailableComposites).toHaveBeenLastCalledWith({
        orgId: "org-id",
        roleId: "role-id",
        source: "organization",
        first: undefined,
        max: undefined,
        search: undefined,
      }),
    );

    mocks.listAvailableComposites.mockResolvedValueOnce([
      { id: "realm-role", name: "realm-role" },
    ]);
    fireEvent.click(screen.getByText("realmRoles"));
    await screen.findByRole("row", { name: "realm-role" });
    expect(mocks.listAvailableComposites).toHaveBeenLastCalledWith(
      expect.objectContaining({ source: "realm" }),
    );

    mocks.listAvailableComposites.mockResolvedValueOnce([
      {
        id: "client-role",
        name: "client-role",
        clientRole: true,
        description: "description",
      },
    ]);
    fireEvent.click(screen.getByText("clientRoles"));
    await screen.findByRole("row", { name: "client-role" });
    expect(mocks.listAvailableComposites).toHaveBeenLastCalledWith(
      expect.objectContaining({ source: "client" }),
    );
  });

  it("loads effective composites and prevents inherited removal", async () => {
    mocks.listComposites.mockResolvedValueOnce([
      { id: "direct", containerId: "org-id", name: "direct" },
    ]);
    renderComposites();
    await screen.findByRole("row", { name: "direct" });

    mocks.listEffectiveComposites.mockResolvedValueOnce([
      { id: "direct", containerId: "org-id", name: "direct" },
      { id: "inherited", containerId: "realm-id", name: "inherited" },
    ]);
    mocks.listComposites.mockResolvedValueOnce([
      { id: "direct", containerId: "org-id", name: "direct" },
    ]);
    fireEvent.click(screen.getByTestId("hideInheritedRoles"));

    const inheritedRow = await screen.findByRole("row", {
      name: "inherited",
    });
    expect(mocks.listEffectiveComposites).toHaveBeenCalledWith({
      orgId: "org-id",
      roleId: "role-id",
      first: undefined,
      max: undefined,
      search: undefined,
    });
    expect(screen.getByText("true")).toBeTruthy();
    expect(
      (
        within(inheritedRow).getByLabelText(
          "Select inherited",
        ) as HTMLInputElement
      ).disabled,
    ).toBe(true);
    expect(
      (
        within(inheritedRow).getByRole("button", {
          name: "unAssignRole",
        }) as HTMLButtonElement
      ).disabled,
    ).toBe(true);

    const directRow = screen.getByRole("row", { name: "direct" });
    fireEvent.click(
      within(directRow).getByRole("button", { name: "unAssignRole" }),
    );
    confirmDialog("remove");
    await waitFor(() =>
      expect(mocks.delComposites).toHaveBeenCalledWith(
        { orgId: "org-id", roleId: "role-id" },
        [{ id: "direct", containerId: "org-id", name: "direct" }],
      ),
    );
  });

  it("reports loader and mutation failures", async () => {
    mocks.listComposites.mockRejectedValueOnce(new Error("load"));
    renderComposites();
    await waitFor(() =>
      expect(mocks.addError).toHaveBeenCalledWith(
        "organizationRoleCompositesLoadError",
        expect.any(Error),
      ),
    );

    cleanup();
    mocks.listAvailableComposites.mockResolvedValueOnce([
      { id: "available", name: "available" },
    ]);
    mocks.addComposites.mockRejectedValueOnce(new Error("add"));
    renderComposites();
    fireEvent.click(
      screen.getAllByRole("button", { name: "addAssociatedRoles" })[0],
    );
    const availableRow = await screen.findByRole("row", { name: "available" });
    fireEvent.click(within(availableRow).getByLabelText("Select available"));
    fireEvent.click(screen.getByTestId("assign-organization-role-composites"));
    await waitFor(() =>
      expect(mocks.addError).toHaveBeenCalledWith(
        "organizationRoleCompositesAddError",
        expect.any(Error),
      ),
    );

    cleanup();
    mocks.listComposites.mockResolvedValueOnce([
      { id: "remove", containerId: "realm-id", name: "remove" },
    ]);
    mocks.delComposites.mockRejectedValueOnce(new Error("remove"));
    renderComposites();
    const row = await screen.findByRole("row", { name: "remove" });
    fireEvent.click(within(row).getByRole("button", { name: "unAssignRole" }));
    confirmDialog("remove");
    await waitFor(() =>
      expect(mocks.addError).toHaveBeenCalledWith(
        "organizationRoleCompositesRemoveError",
        expect.any(Error),
      ),
    );
  });

  it("renders view-only controls", async () => {
    mocks.listComposites.mockResolvedValueOnce([
      { id: "direct", containerId: "org-id", name: "direct" },
    ]);
    renderComposites(false);

    const row = await screen.findByRole("row", { name: "direct" });
    expect(screen.getByTestId("hideInheritedRoles")).toBeTruthy();
    expect(screen.queryByText("addAssociatedRoles")).toBeNull();
    expect(within(row).queryByLabelText("Select direct")).toBeNull();
    expect(
      within(row).queryByRole("button", { name: "unAssignRole" }),
    ).toBeNull();
  });
});
