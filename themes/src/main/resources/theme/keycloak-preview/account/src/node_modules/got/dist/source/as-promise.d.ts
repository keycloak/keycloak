import { CancelableRequest, NormalizedOptions } from './types';
export declare function createRejection(error: Error): CancelableRequest<never>;
export default function asPromise<T>(options: NormalizedOptions): CancelableRequest<T>;
