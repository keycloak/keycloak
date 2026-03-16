/** TIDECLOAK IMPLEMENTATION */
/**
 * Expands a row in the KeycloakDataTable and scrolls to a target section
 * within the expanded detail panel (e.g., Reviews or Comments in ActivityPanel).
 */
export function expandRowAndScrollTo(
  event: React.MouseEvent,
  targetIdPrefix: string,
  draftRecordId: string,
) {
  event.stopPropagation();

  const cell = (event.currentTarget as HTMLElement).closest("td");
  const row = cell?.closest("tr");
  if (!row) return;

  const expandToggle = row.querySelector<HTMLButtonElement>(
    "button[id^='expandable-row-']"
  );

  const isExpanded = row.getAttribute("aria-expanded") === "true" ||
    expandToggle?.getAttribute("aria-expanded") === "true";

  const scrollToTarget = () => {
    const targetId = `${targetIdPrefix}-${draftRecordId}`;
    const attempts = [0, 100, 300, 600];
    let found = false;
    for (const delay of attempts) {
      setTimeout(() => {
        if (found) return;
        const el = document.getElementById(targetId);
        if (el) {
          found = true;
          el.scrollIntoView({ behavior: "smooth", block: "nearest" });
          el.style.outline = "2px solid var(--pf-v5-global--primary-color--100)";
          el.style.borderRadius = "4px";
          setTimeout(() => {
            el.style.outline = "";
            el.style.borderRadius = "";
          }, 2000);
        }
      }, delay);
    }
  };

  if (!isExpanded && expandToggle) {
    expandToggle.click();
    scrollToTarget();
  } else if (isExpanded) {
    scrollToTarget();
  } else if (!expandToggle) {
    // Fallback: try clicking the expand toggle from the parent tbody
    const tbody = row.closest("tbody");
    const toggle = tbody?.querySelector<HTMLButtonElement>(
      "button[id^='expandable-row-']"
    );
    if (toggle) {
      const toggleExpanded = toggle.getAttribute("aria-expanded") === "true";
      if (!toggleExpanded) toggle.click();
      scrollToTarget();
    }
  }
}
