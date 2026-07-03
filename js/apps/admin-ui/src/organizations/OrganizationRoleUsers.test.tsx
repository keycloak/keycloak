/**
 * @vitest-environment jsdom
 */
import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
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
  tables: [] as any[],
  emptyStates: [] as any[],
  memberModals: [] as any[],
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

vi.mock("../groups/MembersModal", () => ({
  MemberModal: (props: any) => {
    mocks.memberModals.push(props);
    return <button onClick={props.onClose}>member-modal</button>;
  },
}));

vi.mock("../components/confirm-dialog/ConfirmDialog", () => ({
  useConfirmDialog: (config: any) => {
    mocks.confirms.push(config);
    return [mocks.toggleConfirm, () => null];
  },
}));

vi.mock("@keycloak/keycloak-ui-shared", () => ({
  useAlerts: () => ({ addAlert: mocks.addAlert, addError: mocks.addError }),
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
}));

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
    mocks.tables.length = 0;
    mocks.emptyStates.length = 0;
    mocks.memberModals.length = 0;
    mocks.confirms.length = 0;
    mocks.listUsers
      .mockReset()
      .mockResolvedValue([{ id: "user-id", username: "user" }]);
    mocks.listAvailableUsers
      .mockReset()
      .mockResolvedValue([{ id: "available-id", username: "available" }]);
    mocks.addUsers.mockReset().mockResolvedValue(undefined);
    mocks.delUsers.mockReset().mockResolvedValue(undefined);
  });

  it("loads, assigns, and removes members", async () => {
    renderUsers();
    const table = mocks.tables.at(-1);
    expect(await table.loader(0, 10)).toEqual([
      { id: "user-id", username: "user" },
    ]);
    expect(
      table.isRowDisabled({
        id: "restricted-id",
        access: { manage: false },
      }),
    ).toBe(true);
    act(() => {
      table.onSelect([
        { id: "user-id", username: "user" },
        { id: "restricted-id", access: { manage: false } },
      ]);
    });
    render(
      <MemoryRouter>
        {table.columns[0].cellRenderer({ id: "user-id", username: "user" })}
      </MemoryRouter>,
    );
    expect(screen.getByText("user")).toBeTruthy();

    fireEvent.click(screen.getByText("addOrganizationRoleUsers"));
    expect(
      await mocks.memberModals.at(-1).availableUsersQuery(0, 10, "a"),
    ).toEqual([{ id: "available-id", username: "available" }]);
    expect(mocks.listAvailableUsers).toHaveBeenCalledWith({
      orgId: "org-id",
      roleId: "role-id",
      first: 0,
      max: 10,
      search: "a",
      briefRepresentation: true,
    });
    expect(
      mocks.memberModals.at(-1).canSelectUser({
        id: "restricted-id",
        access: { manage: false },
      }),
    ).toBe(false);
    await act(async () => {
      await mocks.memberModals
        .at(-1)
        .onAdd([
          { id: "user-id", username: "user", membershipType: "MANAGED" },
        ]);
    });
    expect(mocks.addUsers).toHaveBeenCalledWith(
      { orgId: "org-id", roleId: "role-id" },
      [{ id: "user-id" }],
    );
    mocks.memberModals.at(-1).onClose();
    act(() => {
      mocks.emptyStates.at(-1).onPrimaryAction();
    });
    mocks.memberModals.at(-1).onClose();

    act(() => {
      table.actions[0].onRowClick({ id: "user-id", username: "user" });
    });
    await act(async () => {
      await mocks.confirms.at(-1).onConfirm();
    });
    expect(mocks.delUsers).toHaveBeenCalledWith(
      { orgId: "org-id", roleId: "role-id" },
      [{ id: "user-id" }],
    );
    mocks.confirms.at(-1).onCancel();
  });

  it("reports loader and mutation failures", async () => {
    renderUsers();
    const table = mocks.tables.at(-1);
    mocks.listUsers.mockRejectedValueOnce(new Error("load"));
    expect(await table.loader()).toEqual([]);

    fireEvent.click(screen.getByText("addOrganizationRoleUsers"));
    mocks.addUsers.mockRejectedValueOnce(new Error("add"));
    await act(async () => {
      await mocks.memberModals.at(-1).onAdd([{ id: "user-id" }]);
    });

    act(() => {
      table.actions[0].onRowClick({ id: "user-id" });
    });
    mocks.delUsers.mockRejectedValueOnce(new Error("remove"));
    await act(async () => {
      await mocks.confirms.at(-1).onConfirm();
    });
    expect(mocks.addError).toHaveBeenCalledTimes(3);
  });

  it("renders view-only controls", () => {
    renderUsers(false);
    const table = mocks.tables.at(-1);
    expect(table.toolbarItem).toBe(false);
    expect(table.onSelect).toBeUndefined();
    expect(table.canSelectAll).toBe(false);
    expect(table.actions).toBeUndefined();
    expect(mocks.emptyStates.at(-1).onPrimaryAction).toBeUndefined();
  });
});
