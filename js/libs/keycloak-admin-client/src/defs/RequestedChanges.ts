import type RequestChangesUserRecord from "./RequestChangesUserRecord.js";
/** TIDECLOAK IMPLEMENTATION */

export default interface RequestedChanges {
    action: string;
    changeSetType: string;
    requestType: string;
    clientId: string;
    actionType: string;
    draftRecordId: string;
    userRecord: RequestChangesUserRecord[];
    status: string;
    deleteStatus: string;
}

