import type RequestedChanges from "./RequestedChanges.js";

/** TIDECLOAK IMPLEMENTATION */

export default interface RoleChangeRequest extends RequestedChanges {
    role: string;
}

