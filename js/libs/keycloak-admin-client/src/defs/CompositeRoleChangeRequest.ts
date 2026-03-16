import type RequestedChanges from "./RequestedChanges.js";
/** TIDECLOAK IMPLEMENTATION */
export default interface CompositeRoleChangeRequest extends RequestedChanges {
    role: string;
    compositeRole: string;
}

