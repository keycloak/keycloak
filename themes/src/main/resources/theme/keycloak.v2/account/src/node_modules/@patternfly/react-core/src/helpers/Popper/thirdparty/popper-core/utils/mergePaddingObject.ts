// @ts-nocheck
import { SideObject } from '../types';
import getFreshSideObject from './getFreshSideObject';

/**
 * @param paddingObject
 */
export default function mergePaddingObject(paddingObject: Partial<SideObject>): SideObject {
  return {
    ...getFreshSideObject(),
    ...paddingObject
  };
}
