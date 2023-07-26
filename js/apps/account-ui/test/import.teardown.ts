import { test as setup } from "@playwright/test";
import { deleteRealm } from "./admin-client";

setup("delete realm", () => deleteRealm("photoz"));
