/**
 * @vitest-environment jsdom
 */
import { render, screen, cleanup } from "@testing-library/react";
import type { ReactNode } from "react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  adminClient: {
    roles: { findOneById: vi.fn() },
    clients: { findOne: vi.fn() },
  },
  attributesFormProps: undefined as Record<string, unknown> | undefined,
  showBoundary: vi.fn(),
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock("../../admin-client", () => ({
  useAdminClient: () => ({ adminClient: mocks.adminClient }),
}));

vi.mock("../../context/realm-context/RealmContext", () => ({
  useRealm: () => ({
    realm: { realm: "master" },
    realmRepresentation: { realm: "master" },
  }),
}));

vi.mock("../../context/access/Access", () => ({
  useAccess: () => ({ hasAccess: () => true }),
}));

vi.mock("../../utils/useIsFeatureEnabled", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../utils/useIsFeatureEnabled")>();
  return { ...actual, default: () => () => false };
});

vi.mock("@keycloak/keycloak-ui-shared", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@keycloak/keycloak-ui-shared")>();
  return {
    ...actual,
    useAlerts: () => ({ addAlert: vi.fn(), addError: vi.fn() }),
  };
});

vi.mock("@patternfly/react-core", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@patternfly/react-core")>();
  return {
    ...actual,
    Tab: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
  };
});

vi.mock("../../components/routable-tabs/RoutableTabs", () => ({
  RoutableTabs: ({ children }: { children?: ReactNode }) => (
    <div>{children}</div>
  ),
  useRoutableTab: () => ({}),
}));

vi.mock("../../components/view-header/ViewHeader", () => ({
  ViewHeader: ({ titleKey }: { titleKey?: string }) => (
    <div data-testid="view-header">{titleKey}</div>
  ),
}));

vi.mock("../../components/confirm-dialog/ConfirmDialog", () => ({
  useConfirmDialog: () => [vi.fn(), () => null],
}));

vi.mock("../../components/role-form/RoleForm", () => ({
  RoleForm: () => null,
}));

vi.mock("../../components/role-mapping/RoleMapping", () => ({
  RoleMapping: () => null,
}));

vi.mock("../../components/key-value-form/AttributeForm", () => ({
  AttributesForm: (props: Record<string, unknown>) => {
    mocks.attributesFormProps = props;
    return null;
  },
}));

vi.mock("../../components/permission-tab/PermissionTab", () => ({
  PermissionsTab: () => null,
}));

vi.mock("../../events/AdminEvents", () => ({
  AdminEvents: () => null,
}));

vi.mock("../UsersInRoleTab", () => ({
  UsersInRoleTab: () => null,
}));

import RealmRoleTabs from "../RealmRoleTabs";
import { RealmRoleRoute } from "../routes/RealmRole";
import { ClientRoleRoute } from "../../clients/routes/ClientRole";
import { ErrorBoundaryProvider } from "@keycloak/keycloak-ui-shared";

function renderRealmRole() {
  return render(
    <ErrorBoundaryProvider>
      <MemoryRouter initialEntries={["/master/roles/role-id-1/details"]}>
        <Routes>
          <Route path={RealmRoleRoute.path} element={<RealmRoleTabs />} />
        </Routes>
      </MemoryRouter>
    </ErrorBoundaryProvider>,
  );
}

function renderClientRole() {
  return render(
    <ErrorBoundaryProvider>
      <MemoryRouter
        initialEntries={["/master/clients/my-client/roles/role-id-2/details"]}
      >
        <Routes>
          <Route path={ClientRoleRoute.path} element={<RealmRoleTabs />} />
        </Routes>
      </MemoryRouter>
    </ErrorBoundaryProvider>,
  );
}

describe("RealmRoleTabs", () => {
  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
    mocks.attributesFormProps = undefined;
  });

  it("does not look up the client for a realm role", async () => {
    mocks.adminClient.roles.findOneById.mockResolvedValue({
      id: "role-id-1",
      name: "realm-role-a",
    });
    // The lookup must not happen for realm roles; if it did, users without
    // the view-clients permission would get a 403 that trips the error
    // boundary and makes the whole page unusable (#55225).
    mocks.adminClient.clients.findOne.mockRejectedValue(
      new Error("must not be called for realm roles"),
    );

    renderRealmRole();

    const header = await screen.findByTestId("view-header");
    expect(header.textContent).toContain("realm-role-a");

    expect(mocks.adminClient.clients.findOne).not.toHaveBeenCalled();
    expect(mocks.adminClient.roles.findOneById).toHaveBeenCalledWith({
      id: "role-id-1",
    });
    expect(mocks.showBoundary).not.toHaveBeenCalled();
    expect(mocks.attributesFormProps?.fineGrainedAccess).toBe(false);
  });

  it("looks up the client for a client role and keeps the manage flag", async () => {
    mocks.adminClient.roles.findOneById.mockResolvedValue({
      id: "role-id-2",
      name: "client-role-a",
    });
    mocks.adminClient.clients.findOne.mockResolvedValue({
      id: "my-client",
      access: { manage: true },
    });

    renderClientRole();

    const header = await screen.findByTestId("view-header");
    expect(header.textContent).toContain("client-role-a");

    expect(mocks.adminClient.clients.findOne).toHaveBeenCalledWith({
      id: "my-client",
    });
    expect(mocks.attributesFormProps?.fineGrainedAccess).toBe(true);
  });
});
