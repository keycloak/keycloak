import { Merge } from 'type-fest';
export default function merge<Target extends {
    [key: string]: any;
}, Source extends {
    [key: string]: any;
}>(target: Target, ...sources: Source[]): Merge<Source, Target>;
