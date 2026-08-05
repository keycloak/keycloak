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
  listUsers: vi.fn(),
  listAvailableUsers: vi.fn(),
  addUsers: vi.fn(),
  delUsers: vi.fn(),
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
        listRoleUsers: mocks.listUsers,
        listAvailableRoleUsers: mocks.listAvailableUsers,
        addRoleUsers: mocks.addUsers,
        delRoleUsers: mocks.delUsers,
      },
    },
  }),
}));

vi.mock("../context/realm-context/RealmContext", () => ({
  useRealm: () => ({ realm: "test-realm" }),
}));

vi.mock("../groups/MembersModal", async () => {
  const { useEffect, useState } = await import("react");

  return {
    MemberModal: (props: any) => {
      const [availableUsers, setAvailableUsers] = useState<any[]>([]);
      const [selected, setSelected] = useState<any[]>([]);

      useEffect(() => {
        let mounted = true;
        props.availableUsersQuery(0, 10, "a").then((users: any[]) => {
          if (mounted) {
            setAvailableUsers(users);
          }
        });
        return () => {
          mounted = false;
        };
      }, []);

      return (
        <div role="dialog" aria-label={props.titleKey}>
          {availableUsers.map((user) => (
            <button
              key={user.id}
              disabled={!props.canSelectUser(user)}
              onClick={() => setSelected([user])}
            >
              {user.username}
            </button>
          ))}
          <button
            onClick={async () => {
              await props.onAdd(selected);
              props.onClose();
            }}
          >
            {props.confirmLabelKey}
          </button>
          <button onClick={props.onClose}>cancel</button>
        </div>
      );
    },
  };
});

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

  const rowLabel = (row: any) => row.username ?? row.id;

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
      </>
    ),
  };
});

import { OrganizationRoleUsers } from "./OrganizationRoleUsers";

const renderUsers = (canMapRole = true) =>
  render(
    <MemoryRouter>
      <OrganizationRoleUsers
        organizationId="org-id"
        roleId="role-id"
        canMapRole={canMapRole}
      />
    </MemoryRouter>,
  );

describe("OrganizationRoleUsers", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
    mocks.listUsers.mockReset().mockResolvedValue([
      { id: "user-id", username: "user" },
      {
        id: "restricted-id",
        username: "restricted",
        access: { manage: false },
      },
    ]);
    mocks.listAvailableUsers
      .mockReset()
      .mockResolvedValue([{ id: "available-id", username: "available" }]);
    mocks.addUsers.mockReset().mockResolvedValue(undefined);
    mocks.delUsers.mockReset().mockResolvedValue(undefined);
  });

  it("loads, assigns, and removes members", async () => {
    renderUsers();

    const userRow = await screen.findByRole("row", { name: "user" });
    expect(within(userRow).getByText("user")).toBeTruthy();
    expect(
      (screen.getByLabelText("Select restricted") as HTMLInputElement).disabled,
    ).toBe(true);

    fireEvent.click(screen.getByText("addOrganizationRoleUsers"));
    fireEvent.click(await screen.findByRole("button", { name: "available" }));
    fireEvent.click(screen.getByRole("button", { name: "assign" }));
    await waitFor(() =>
      expect(mocks.addUsers).toHaveBeenCalledWith(
        { orgId: "org-id", roleId: "role-id" },
        [{ id: "available-id" }],
      ),
    );
    expect(mocks.listAvailableUsers).toHaveBeenCalledWith({
      orgId: "org-id",
      roleId: "role-id",
      first: 0,
      max: 10,
      search: "a",
      briefRepresentation: true,
    });

    const refreshedUserRow = await screen.findByRole("row", { name: "user" });
    fireEvent.click(
      within(refreshedUserRow).getByRole("button", { name: "remove" }),
    );
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: "remove",
      }),
    );
    await waitFor(() =>
      expect(mocks.delUsers).toHaveBeenCalledWith(
        { orgId: "org-id", roleId: "role-id" },
        [{ id: "user-id" }],
      ),
    );
    expect(mocks.addAlert).toHaveBeenCalled();
  });

  it("reports loader and mutation failures", async () => {
    mocks.listUsers.mockRejectedValueOnce(new Error("load"));
    renderUsers();
    await waitFor(() =>
      expect(mocks.addError).toHaveBeenCalledWith(
        "organizationRoleUsersLoadError",
        expect.any(Error),
      ),
    );

    cleanup();
    renderUsers();
    await screen.findByRole("row", { name: "user" });
    mocks.listAvailableUsers.mockRejectedValueOnce(new Error("available"));
    fireEvent.click(screen.getByText("addOrganizationRoleUsers"));
    await waitFor(() =>
      expect(mocks.addError).toHaveBeenCalledWith(
        "organizationRoleUsersLoadError",
        expect.any(Error),
      ),
    );

    cleanup();
    renderUsers();
    await screen.findByRole("row", { name: "user" });
    mocks.addUsers.mockRejectedValueOnce(new Error("add"));
    fireEvent.click(screen.getByText("addOrganizationRoleUsers"));
    fireEvent.click(await screen.findByRole("button", { name: "available" }));
    fireEvent.click(screen.getByRole("button", { name: "assign" }));
    await waitFor(() =>
      expect(mocks.addError).toHaveBeenCalledWith(
        "organizationRoleUsersAddError",
        expect.any(Error),
      ),
    );

    mocks.delUsers.mockRejectedValueOnce(new Error("remove"));
    const row = screen.getByRole("row", { name: "user" });
    fireEvent.click(within(row).getByRole("button", { name: "remove" }));
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: "remove",
      }),
    );
    await waitFor(() =>
      expect(mocks.addError).toHaveBeenCalledWith(
        "organizationRoleUsersRemoveError",
        expect.any(Error),
      ),
    );
  });

  it("renders view-only controls", async () => {
    renderUsers(false);

    const row = await screen.findByRole("row", { name: "user" });
    expect(screen.queryByText("addOrganizationRoleUsers")).toBeNull();
    expect(within(row).queryByLabelText("Select user")).toBeNull();
    expect(within(row).queryByRole("button", { name: "remove" })).toBeNull();
  });
});
