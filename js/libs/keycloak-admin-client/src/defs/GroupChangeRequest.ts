import type RequestedChanges from "./RequestedChanges.js";

/** TIDECLOAK IMPLEMENTATION */

export default interface GroupChangeRequest extends RequestedChanges {
    groupName: string;
    roleName: string;
    userName: string;
}
