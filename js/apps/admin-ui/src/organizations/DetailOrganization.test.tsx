/**
 * @vitest-environment jsdom
 */
import { render, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  roles: vi.fn(),
  findOne: vi.fn().mockResolvedValue({ id: "org-id", name: "organization" }),
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock("@patternfly/react-core", async () => {
  const actual = await vi.importActual<any>("@patternfly/react-core");
  return {
    ...actual,
    Tab: ({ children }: any) => <div>{children}</div>,
    Tabs: ({ children }: any) => <div>{children}</div>,
    TabTitleText: ({ children }: any) => children,
  };
});

vi.mock("@keycloak/keycloak-ui-shared", async () => {
  const { useEffect } = await import("react");
  return {
    FormSubmitButton: ({ children }: any) => <button>{children}</button>,
    useAlerts: () => ({ addAlert: vi.fn(), addError: vi.fn() }),
    useFetch: (
      request: () => Promise<unknown>,
      success: (value: any) => void,
    ) => {
      useEffect(() => {
        void request().then(success);
      }, []);
    },
  };
});

vi.mock("../admin-client", () => ({
  useAdminClient: () => ({
    adminClient: {
      organizations: { findOne: mocks.findOne, updateById: vi.fn() },
    },
  }),
}));

vi.mock("../components/form/FormAccess", () => ({
  FormAccess: ({ children }: any) => <div>{children}</div>,
}));

vi.mock("../components/key-value-form/AttributeForm", () => ({
  AttributesForm: () => null,
}));

vi.mock("../components/routable-tabs/RoutableTabs", () => ({
  RoutableTabs: ({ children }: any) => <div>{children}</div>,
  useRoutableTab: () => ({}),
}));

vi.mock("../context/access/Access", () => ({
  useAccess: () => ({ hasAccess: () => false }),
}));

vi.mock("../context/realm-context/RealmContext", () => ({
  useRealm: () => ({
    realm: "test-realm",
    realmRepresentation: { adminEventsEnabled: false },
  }),
}));

vi.mock("../utils/useParams", () => ({
  useParams: () => ({ id: "org-id" }),
}));

vi.mock("./DetailOraganzationHeader", () => ({
  DetailOrganizationHeader: () => null,
}));
vi.mock("./IdentityProviders", () => ({ IdentityProviders: () => null }));
vi.mock("./MembersSection", () => ({ MembersSection: () => null }));
vi.mock("../groups/GroupsSection", () => ({ default: () => null }));
vi.mock("./OrganizationForm", () => ({
  OrganizationForm: () => null,
  convertToOrg: (value: unknown) => value,
}));
vi.mock("../events/AdminEvents", () => ({ AdminEvents: () => null }));
vi.mock("./OrganizationRoles", () => ({
  OrganizationRoles: () => {
    mocks.roles();
    return <div>organization-roles</div>;
  },
}));

import DetailOrganization from "./DetailOrganization";

it("renders the organization roles tab", async () => {
  render(
    <MemoryRouter>
      <DetailOrganization />
    </MemoryRouter>,
  );
  await waitFor(() => expect(mocks.findOne).toHaveBeenCalled());
  expect(mocks.roles).toHaveBeenCalled();
});
