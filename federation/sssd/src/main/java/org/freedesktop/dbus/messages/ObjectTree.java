package org.freedesktop.dbus.messages;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps track of the exported objects for introspection data */
public class ObjectTree {
    public static final Pattern SLASH_PATTERN = Pattern.compile("/");

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private TreeNode     root;

    public ObjectTree() {
        root = new TreeNode("");
    }

    private TreeNode recursiveFind(TreeNode _current, String _path) {
        if ("/".equals(_path)) {
            return _current;
        }
        String[] elements = _path.split("/", 2);
        // this is us or a parent node
        if (_path.startsWith(_current.name)) {
            // this is us
            if (_path.equals(_current.name)) {
                return _current;
            } else { // recurse down
                if (_current.down == null) {
                    return null;
                } else {
                    return recursiveFind(_current.down, elements[1]);
                }
            }
        } else if (_current.right == null) {
            return null;
        } else if (0 > _current.right.name.compareTo(elements[0])) {
            return null;
        } else { // recurse right
            return recursiveFind(_current.right, _path);
        }
    }

    private TreeNode recursiveAdd(TreeNode _current, String _path, ExportedObject _object, String _data) {
        String[] elements = SLASH_PATTERN.split(_path, 2);
        // this is us or a parent node
        if (_path.startsWith(_current.name)) {
            // this is us
            if (1 == elements.length || "".equals(elements[1])) {
                _current.object = _object;
                _current.data = _data;
            } else { // recurse down
                if (_current.down == null) {
                    String[] el = elements[1].split("/", 2);
                    _current.down = new TreeNode(el[0]);
                }
                _current.down = recursiveAdd(_current.down, elements[1], _object, _data);
            }
        } else if (_current.right == null) { // need to create a new sub-tree on the end
            _current.right = new TreeNode(elements[0]);
            _current.right = recursiveAdd(_current.right, _path, _object, _data);
        } else if (0 > _current.right.name.compareTo(elements[0])) { // need to insert here
            TreeNode t = new TreeNode(elements[0]);
            t.right = _current.right;
            _current.right = t;
            _current.right = recursiveAdd(_current.right, _path, _object, _data);
        } else { // recurse right
            _current.right = recursiveAdd(_current.right, _path, _object, _data);
        }
        return _current;
    }

    public synchronized void add(String _path, ExportedObject _object, String _data) {
        logger.debug("Adding {} to object tree", _path);
        root = recursiveAdd(root, _path, _object, _data);
    }

    private TreeNode recursiveRemove(TreeNode _current, String _path) {
        String[] elements = _path.split("/", 2);
        if (elements[0].equals(_current.name)) {
            // this is us or a parent node
            if (1 == elements.length || "".equals(elements[1])) {
                // this is us
                _current.object = null;
                _current.data = null;
                if (_current.down != null) {
                    // This node has a child node so it needs to be kept
                    return _current;
                }
                return _current.right;
            }

            if (_current.down != null) {
                // recurse down
                _current.down = recursiveRemove(_current.down, elements[1]);
                if (_current.down == null && _current.data == null) {
                    // This node has no children anymore and is not exported itself so it can be removed
                    return _current.right;
                }
            }
            return _current;
        } else if (_current.right == null) {
            return _current;
        } else if (0 > _current.right.name.compareTo(elements[0])) {
            return _current;
        } else {
            // recurse right
            _current.right = recursiveRemove(_current.right, _path);
            return _current;
        }
    }

    public synchronized void remove(String _path) {
        logger.debug("Removing {} from object tree", _path);
        recursiveRemove(root, _path);
    }

    // CHECKSTYLE:OFF
    public String Introspect(String _path) {
    // CHECKSTYLE:ON
        TreeNode t = recursiveFind(root, _path);
        if (null == t) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

        sb.append("<node name=\"");
        sb.append(_path);
        sb.append("\">\n");

        if (null != t.data) {
            sb.append(t.data);
        }
        t = t.down;
        while (null != t) {
            sb.append("<node name=\"");
            sb.append(t.name);
            sb.append("\"/>\n");
            t = t.right;
        }
        sb.append("</node>");
        return sb.toString();
    }

    private String recursivePrint(TreeNode _current) {
        String s = "";
        if (null != _current) {
            s += _current.name;
            if (null != _current.object) {
                s += "*";
            }
            if (null != _current.down) {
                s += "/{" + recursivePrint(_current.down) + "}";
            }
            if (null != _current.right) {
                s += ", " + recursivePrint(_current.right);
            }
        }
        return s;
    }

    @Override
    public String toString() {
        return recursivePrint(root);
    }

    static class TreeNode {
        // CHECKSTYLE:OFF
        String         name;
        ExportedObject object;
        String         data;
        TreeNode       right;
        TreeNode       down;
        // CHECKSTYLE:ON

        TreeNode(String _name) {
            name = _name;
        }

        TreeNode(String _name, ExportedObject _object, String _data) {
            name = _name;
            object = _object;
            data = _data;
        }
    }
}
