import { redirect } from "react-router-dom";

import type { AppRouteObject } from "../../../routes";
import { toUsers } from "../../../user/routes/Users";

/**
 * This patch is needed for bread crumbs to display properly for this page.
 * The attribute query routes must be rendered under '/attributeStore' instead
 * of '/users/attributeStore'. If they are rendered under "users", the attribute query
 * tab is identified as "User Details" in the bread crumbs.
 *
 * Adding this route makes allows the bread crumb to show properly and will redirect to the
 * correct users tab when it is clicked.
 */
export const AttributeStorePatchRoute: AppRouteObject = {
  path: "/:realm/attributeStore",
  breadcrumb: (t) => t("attributeStore.tab.breadcrumb"),
  loader: ({ params }) =>
    redirect(
      toUsers({ realm: params.realm!, tab: "attributeStore" }).pathname!,
    ),
  handle: {
    access: "view-realm",
  },
};
