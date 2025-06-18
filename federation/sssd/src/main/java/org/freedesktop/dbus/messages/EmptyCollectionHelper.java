package org.freedesktop.dbus.messages;

import java.util.Arrays;

final class EmptyCollectionHelper {

    private EmptyCollectionHelper() {}

    /**
     * This function determine the new offset in signature for empty Dictionary/Map collections. Normally the element
     * inside a collection determines the new offset, however in case of empty collections there is no element to
     * determine the sub signature of the list so this function determines which part of the signature to skip
     *
     * @param _sigb the total signature
     * @param _currentOffset the current offset within the signature
     * @return the index of the last element of the collection (subtype)
     */
    static int determineSignatureOffsetDict(byte[] _sigb, int _currentOffset) {
        return determineEndOfBracketStructure(_sigb, _currentOffset, '{', '}');
    }

    /**
     * This function determine the new offset in signature for empty Array/List collections.
     * Normally the element inside a collection determines the new offset,
     * however in case of empty collections there is no element to determine the sub signature of the list
     * so this function determines which part of the signature to skip
     *
     * @param _sigb the total signature
     * @param _currentOffset the current offset within the signature
     * @return the index of the last element of the collection (subtype)
     */
    static int determineSignatureOffsetArray(byte[] _sigb, int _currentOffset) {
        String sigSubString = determineSubSignature(_sigb, _currentOffset);

        // End of string so can't have any more offset
        if (sigSubString.isEmpty()) {
            return _currentOffset;
        }

        ECollectionSubType newtype = determineCollectionSubType((char) _sigb[_currentOffset]);
        switch (newtype) {
            case ARRAY:
                // array in array so look at the next type
                return determineSignatureOffsetArray(_sigb, _currentOffset + 1);
            case DICT:
                return determineSignatureOffsetDict(_sigb, _currentOffset);
            case STRUCT:
                return determineSignatureOffsetStruct(_sigb, _currentOffset);
            case PRIMITIVE:
                //primitive is always one element so no need to skip more
                return _currentOffset;
            default:
                break;

        }
        throw new IllegalStateException("Unable to parse signature for empty collection");
    }

    private static int determineSignatureOffsetStruct(byte[] _sigb, int _currentOffset) {
        return determineEndOfBracketStructure(_sigb, _currentOffset, '(', ')');
    }

    /**
     * This is a generic function to determine the end of a structure that has opening and closing characters.
     * Currently used for Struct () and Dict {}
     *
     */
    private static int determineEndOfBracketStructure(byte[] _sigb, int _currentOffset, char _openChar, char _closeChar) {
        String sigSubString = determineSubSignature(_sigb, _currentOffset);

        // End of string so can't have any more offset
        if (sigSubString.isEmpty()) {
            return _currentOffset;
        }
        int i = 0;
        int depth = 0;

        for (char chr : sigSubString.toCharArray()) {
            //book keeping of depth of nested structures to solve opening closing bracket problem
            if (chr == _openChar) {
                depth++;
            } else if (chr == _closeChar) {
                depth--;
            }
            if (depth == 0) {
                return _currentOffset + i;
            }
            i++;
        }
        throw new IllegalStateException("Unable to parse signature for empty collection");
    }

    private static String determineSubSignature(byte[] _sigb, int _currentOffset) {
        byte[] restSigbytes = Arrays.copyOfRange(_sigb, _currentOffset, _sigb.length);
        return new String(restSigbytes);
    }

    /**
     * The starting type determines of a collection determines when it ends
     * @param _sig the signature letter of the type
     */
    private static ECollectionSubType determineCollectionSubType(char _sig) {
        switch (_sig) {
            case '(':
                return ECollectionSubType.STRUCT;
            case '{':
                return ECollectionSubType.DICT;
            case 'a':
                return ECollectionSubType.ARRAY;
            default:
                // of course there can be other types but those shouldn't be allowed in this part of the signature
                return ECollectionSubType.PRIMITIVE;
        }
    }

    /**
     * Internal Enumeration used to group the types of element
     */
    enum ECollectionSubType {
        STRUCT,
        DICT,
        ARRAY,
        PRIMITIVE
    }
}
