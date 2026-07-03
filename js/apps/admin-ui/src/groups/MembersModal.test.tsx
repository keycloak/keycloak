/**
 * @vitest-environment jsdom
 */
import { cleanup, render } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  addError: vi.fn(),
  listMembers: vi.fn(),
  findUsers: vi.fn(),
  tables: [] as any[],
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock("@patternfly/react-core", () => ({
  Button: ({ children, ...props }: any) => (
    <button {...props}>{children}</button>
  ),
  Label: ({ children }: any) => <span>{children}</span>,
  Modal: ({ children }: any) => <div>{children}</div>,
  ModalVariant: { large: "large" },
}));

vi.mock("@patternfly/react-icons", () => ({
  InfoCircleIcon: () => null,
}));

vi.mock("../admin-client", () => ({
  useAdminClient: () => ({
    adminClient: {
      organizations: { listMembers: mocks.listMembers },
      users: { find: mocks.findUsers },
    },
  }),
}));

vi.mock("@keycloak/keycloak-ui-shared", () => ({
  useAlerts: () => ({ addError: mocks.addError }),
  KeycloakDataTable: (props: any) => {
    mocks.tables.push(props);
    return <div data-testid="table" />;
  },
  ListEmptyState: () => null,
}));

vi.mock("../util", () => ({
  emptyFormatter: () => vi.fn(),
}));

import { MemberModal } from "./MembersModal";

describe("MemberModal", () => {
  beforeEach(() => {
    mocks.tables.length = 0;
    vi.clearAllMocks();
  });

  afterEach(cleanup);

  it("loads and paginates candidates from a supplied query", async () => {
    const availableUsersQuery = vi.fn().mockResolvedValue([
      { id: "first", username: "first" },
      { id: "second", username: "second" },
    ]);

    render(
      <MemberModal
        membersQuery={vi.fn()}
        availableUsersQuery={availableUsersQuery}
        onAdd={vi.fn()}
        onClose={vi.fn()}
      />,
    );

    expect(await mocks.tables.at(-1).loader(0, 1, "fir")).toEqual([
      { id: "first", username: "first" },
    ]);
    expect(availableUsersQuery).toHaveBeenCalledWith(0, 1, "fir");
  });

  it("filters fallback candidates by membership, email, and permission", async () => {
    const member = { id: "member", username: "member", email: "member@test" };
    const allowed = {
      id: "allowed",
      username: "allowed",
      email: "allowed@test",
    };
    mocks.findUsers.mockResolvedValue([
      member,
      { id: "missing-email", username: "missing-email" },
      allowed,
      { id: "restricted", username: "restricted", email: "restricted@test" },
    ]);

    render(
      <MemberModal
        membersQuery={vi.fn().mockResolvedValue([member])}
        filterEmptyEmail
        canSelectUser={(user) => user.id === allowed.id}
        onAdd={vi.fn()}
        onClose={vi.fn()}
      />,
    );

    expect(await mocks.tables.at(-1).loader(0, 10, "user")).toEqual([allowed]);
    expect(mocks.findUsers).toHaveBeenCalledWith({
      first: 0,
      max: 11,
      search: "user",
    });
  });
});
