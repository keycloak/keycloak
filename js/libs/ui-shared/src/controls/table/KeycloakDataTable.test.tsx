import {
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ErrorBoundaryProvider } from "../../utils/ErrorBoundary";
import { KeycloakDataTable } from "./KeycloakDataTable";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

describe("KeycloakDataTable", () => {
  afterEach(() => {
    cleanup();
    localStorage.removeItem("pageSize");
  });

  it("selects enabled rows once across pages", async () => {
    localStorage.setItem("pageSize", "3");
    const rows = Array.from({ length: 6 }, (_, index) => ({
      id: `user-${index + 1}`,
      disabled: index === 5,
    }));
    const onSelect = vi.fn();
    render(
      <KeycloakDataTable
        ariaLabelKey="users"
        loader={rows}
        columns={[{ name: "id" }]}
        isRowDisabled={(row) => row.disabled}
        canSelectAll
        onSelect={onSelect}
      />,
      { wrapper: ErrorBoundaryProvider },
    );

    const firstRow = await screen.findByRole("row", { name: /user-1/ });
    fireEvent.click(within(firstRow).getByRole("checkbox"));
    fireEvent.click(
      screen.getAllByRole("button", { name: "Go to next page" })[0],
    );

    const nextRow = await screen.findByRole("row", { name: /user-4/ });
    fireEvent.click(within(nextRow).getByRole("checkbox"));
    const selectAll = screen.getByRole("checkbox", { name: "Select all rows" });
    fireEvent.click(selectAll);

    expect(onSelect).toHaveBeenLastCalledWith([rows[0], rows[3], rows[4]]);

    fireEvent.click(selectAll);

    expect(onSelect).toHaveBeenLastCalledWith([rows[0]]);
  });

  it("selects and clears rows without ids independently", async () => {
    const rows = [{ name: "Profile one" }, { name: "Profile two" }];
    const onSelect = vi.fn();
    render(
      <KeycloakDataTable
        ariaLabelKey="profilesList"
        loader={rows}
        columns={[{ name: "name" }]}
        canSelectAll
        onSelect={onSelect}
      />,
      { wrapper: ErrorBoundaryProvider },
    );

    const firstRow = await screen.findByRole("row", { name: /Profile one/ });
    const secondRow = screen.getByRole("row", { name: /Profile two/ });
    const firstCheckbox = within(firstRow).getByRole("checkbox");
    const secondCheckbox = within(secondRow).getByRole("checkbox");
    const selectAll = screen.getByRole("checkbox", { name: "Select all rows" });

    fireEvent.click(firstCheckbox);
    expect(firstCheckbox).toHaveProperty("checked", true);
    expect(secondCheckbox).toHaveProperty("checked", false);

    fireEvent.click(selectAll);
    expect(selectAll).toHaveProperty("checked", true);
    expect(onSelect).toHaveBeenLastCalledWith(rows);

    fireEvent.click(firstCheckbox);
    expect(onSelect).toHaveBeenLastCalledWith([rows[1]]);
  });
});
