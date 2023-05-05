package org.freedesktop.dbus;

import java.util.HashSet;
import java.util.Set;

/**
 * @deprecated this class was used as map key internally and is no longer in use
 */
@Deprecated(forRemoval = true, since = "4.2.0 - 2022-08-18")
public class SignalTuple {
    private final String type;
    private final String name;
    private final String object;
    private final String source;

    public SignalTuple(String _type, String _name, String _object, String _source) {
        this.type = _type;
        this.name = _name;
        this.object = _object;
        this.source = _source;
    }

    @Override
    public boolean equals(Object _o) {
        if (!(_o instanceof SignalTuple)) {
            return false;
        }
        SignalTuple other = (SignalTuple) _o;
        if (null == this.type && null != other.type) {
            return false;
        } else if (null != this.type && !this.type.equals(other.type)) {
            return false;
        } else if (null == this.name && null != other.name) {
            return false;
        } else if (null != this.name && !this.name.equals(other.name)) {
            return false;
        } else if (null == this.object && null != other.object) {
            return false;
        } else if (null != this.object && !this.object.equals(other.object)) {
            return false;
        } else if (null == this.source && null != other.source) {
            return false;
        } else  if (null != this.source && !this.source.equals(other.source)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return (null == type ? 0 : type.hashCode()) + (null == name ? 0 : name.hashCode()) + (null == source ? 0 : source.hashCode()) + (null == object ? 0 : object.hashCode());
    }

    @Override
    public String toString() {
        return "SignalTuple(" + type + "," + name + "," + object + "," + source + ")";
    }

    /**
     * Get a {@link Set} of all possible SignalTuples that we can have, given the 4 parameters.
     * @param _type interface type
     * @param _name name
     * @param _object object
     * @param _source source
     * @return {@link Set} of {@link SignalTuple}, never null
     * @deprecated should no longer be used
     */
    @Deprecated
    public static Set<SignalTuple> getAllPossibleTuples(String _type, String _name, String _object, String _source) {
        Set<SignalTuple> allTuples = new HashSet<>();

        // Tuple with no null
        allTuples.add(new SignalTuple(_type, _name, _object, _source));

        // Tuples with one null
        allTuples.add(new SignalTuple(null, _name, _object, _source));
        allTuples.add(new SignalTuple(_type, null, _object, _source));
        allTuples.add(new SignalTuple(_type, _name, null, _source));
        allTuples.add(new SignalTuple(_type, _name, _object, null));

        // Tuples where type is null, and one other null
        allTuples.add(new SignalTuple(null, null, _object, _source));
        allTuples.add(new SignalTuple(null, _name, null, _source));
        allTuples.add(new SignalTuple(null, _name, _object, null));

        // Tuples where name is null, and one other null
        allTuples.add(new SignalTuple(_type, null, null, _source));
        allTuples.add(new SignalTuple(_type, null, _object, null));

        // Tuples where object is null, and one other null
        allTuples.add(new SignalTuple(null, _name, null, _source));
        allTuples.add(new SignalTuple(_type, _name, null, null));

        // Tuples where source is null, and one other null
        allTuples.add(new SignalTuple(null, _name, _object, null));
        allTuples.add(new SignalTuple(_type, _name, null, null));

        // Tuples with three nulls
        allTuples.add(new SignalTuple(_type, null, null, null));
        allTuples.add(new SignalTuple(null, _name, null, null));
        allTuples.add(new SignalTuple(null, null, _object, null));
        allTuples.add(new SignalTuple(null, null, null, _source));

        return allTuples;
    }
}
