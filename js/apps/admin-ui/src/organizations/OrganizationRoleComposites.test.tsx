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
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  addAlert: vi.fn(),
  addError: vi.fn(),
  listRoles: vi.fn(),
  findRealmRoles: vi.fn(),
  listComposites: vi.fn(),
  addComposites: vi.fn(),
  delComposites: vi.fn(),
  availableClientRoles: vi.fn(),
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
        listRoleComposites: mocks.listComposites,
        addRoleComposites: mocks.addComposites,
        delRoleComposites: mocks.delComposites,
      },
      roles: { find: mocks.findRealmRoles },
    },
  }),
}));

vi.mock("../components/role-mapping/resource", () => ({
  getAvailableClientRoles: mocks.availableClientRoles,
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

import {
  AddOrganizationRoleCompositeModal,
  OrganizationRoleComposites,
} from "./OrganizationRoleComposites";

describe("OrganizationRoleComposites", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
    mocks.tables.length = 0;
    mocks.emptyStates.length = 0;
    mocks.confirms.length = 0;
    mocks.listRoles.mockReset().mockResolvedValue([]);
    mocks.findRealmRoles.mockReset().mockResolvedValue([]);
    mocks.listComposites.mockReset().mockResolvedValue([]);
    mocks.addComposites.mockReset().mockResolvedValue(undefined);
    mocks.delComposites.mockReset().mockResolvedValue(undefined);
    mocks.availableClientRoles.mockReset().mockResolvedValue([]);
  });

  it("loads, adds, and removes composites", async () => {
    mocks.listComposites.mockResolvedValueOnce([
      { id: "org-child", containerId: "org-id", name: "org" },
      { id: "realm-child", containerId: "realm-id", name: "realm" },
      { id: "client-child", clientRole: true, name: "client" },
    ]);
    render(
      <OrganizationRoleComposites
        organizationId="org-id"
        roleId="role-id"
        roleName="parent"
        canManage
      />,
    );
    const table = mocks.tables.at(-1);
    expect((await table.loader()).map((role: any) => role.source)).toEqual([
      "organization",
      "realm",
      "client",
    ]);
    act(() => {
      table.onSelect([{ id: "org-child", source: "organization" }]);
    });
    table.columns[1].cellRenderer({ source: "realm" });

    mocks.listComposites.mockResolvedValueOnce([{ id: "assigned" }]);
    await act(async () => {
      await table.toolbarItem.props.children[0].props.children.props.onClick();
    });
    expect(screen.getByText(/addAssociatedRolesTo/)).toBeTruthy();

    const modalTable = mocks.tables.findLast(
      (props) => props.ariaLabelKey === "availableOrganizationRoleComposites",
    );
    mocks.listRoles.mockResolvedValueOnce([
      { id: "role-id", name: "self" },
      { id: "assigned", name: "assigned" },
      { id: "available", name: "available" },
      {
        id: "restricted",
        name: "restricted",
        access: { mapComposite: false },
      },
    ]);
    expect(await modalTable.loader(0, 10, "a")).toEqual([
      expect.objectContaining({ id: "available", source: "organization" }),
    ]);
    act(() => {
      modalTable.onSelect([
        {
          id: "available",
          name: "available",
          source: "organization",
          clientName: "not-sent",
        },
      ]);
    });
    await act(async () => {
      screen.getByTestId("assign-organization-role-composites").click();
    });
    expect(mocks.addComposites).toHaveBeenCalledWith(
      { orgId: "org-id", roleId: "role-id" },
      [{ id: "available", name: "available" }],
    );

    act(() => {
      table.actions[0].onRowClick({ id: "remove", source: "realm" });
    });
    await act(async () => {
      await mocks.confirms.at(-1).onConfirm();
    });
    expect(mocks.delComposites).toHaveBeenCalled();
    mocks.confirms.at(-1).onCancel();
  });

  it("loads every source in the assignment modal", async () => {
    const onAssign = vi.fn().mockResolvedValue(true);
    render(
      <AddOrganizationRoleCompositeModal
        organizationId="org-id"
        roleId="role-id"
        roleName="parent"
        assignedRoleIds={new Set()}
        onAssign={onAssign}
        onClose={vi.fn()}
      />,
    );
    expect(
      await mocks.tables
        .findLast(
          (props) =>
            props.ariaLabelKey === "availableOrganizationRoleComposites",
        )
        .loader(),
    ).toEqual([]);

    fireEvent.click(screen.getByText("realmRoles"));
    mocks.findRealmRoles.mockResolvedValueOnce([{ id: "realm-role" }]);
    expect(
      await mocks.tables
        .findLast(
          (props) =>
            props.ariaLabelKey === "availableOrganizationRoleComposites",
        )
        .loader(),
    ).toEqual([expect.objectContaining({ id: "realm-role", source: "realm" })]);

    fireEvent.click(screen.getByText("clientRoles"));
    mocks.availableClientRoles.mockResolvedValueOnce([
      {
        id: "client-role",
        role: "client role",
        client: "client",
        clientId: "client-id",
        description: "description",
      },
    ]);
    expect(
      await mocks.tables
        .findLast(
          (props) =>
            props.ariaLabelKey === "availableOrganizationRoleComposites",
        )
        .loader(),
    ).toEqual([
      expect.objectContaining({
        id: "client-role",
        source: "client",
        clientName: "client",
      }),
    ]);
  });

  it("reports loader and mutation failures", async () => {
    render(
      <OrganizationRoleComposites
        organizationId="org-id"
        roleId="role-id"
        roleName="parent"
        canManage
      />,
    );
    const table = mocks.tables.at(-1);
    mocks.listComposites.mockRejectedValueOnce(new Error("load"));
    expect(await table.loader()).toEqual([]);
    mocks.listComposites.mockRejectedValueOnce(new Error("open"));
    await act(async () => {
      await table.toolbarItem.props.children[0].props.children.props.onClick();
    });

    mocks.listComposites.mockResolvedValueOnce([]);
    await act(async () => {
      await table.toolbarItem.props.children[0].props.children.props.onClick();
    });
    mocks.addComposites.mockRejectedValueOnce(new Error("add"));
    const modalTable = mocks.tables.findLast(
      (props) => props.ariaLabelKey === "availableOrganizationRoleComposites",
    );
    act(() => {
      modalTable.onSelect([
        { id: "available", name: "available", source: "organization" },
      ]);
    });
    await act(async () => {
      screen.getByTestId("assign-organization-role-composites").click();
    });
    expect(mocks.addError).toHaveBeenCalledWith(
      "organizationRoleCompositesAddError",
      expect.any(Error),
    );

    act(() => {
      table.actions[0].onRowClick({ id: "remove", source: "realm" });
    });
    mocks.delComposites.mockRejectedValueOnce(new Error("remove"));
    await act(async () => {
      await mocks.confirms.at(-1).onConfirm();
    });
    expect(mocks.addError).toHaveBeenCalled();
  });

  it("renders view-only controls", () => {
    render(
      <OrganizationRoleComposites
        organizationId="org-id"
        roleId="role-id"
        roleName="parent"
        canManage={false}
      />,
    );
    const table = mocks.tables.at(-1);
    expect(table.toolbarItem).toBe(false);
    expect(table.onSelect).toBeUndefined();
    expect(table.actions).toBeUndefined();
    expect(mocks.emptyStates.at(-1).onPrimaryAction).toBeUndefined();
  });
});
