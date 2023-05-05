package org.freedesktop.dbus.utils;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Sample ErrorHandlers for XML Parsing.
 *
 * @author hypfvieh
 */
public class XmlErrorHandlers {

    /**
     * XML Error Handler which will silently ignore all thrown Exceptions.
     *
     * @author hypfvieh
     * @since v1.0.3 - 2018-01-10
     */
    public static class XmlErrorHandlerQuiet implements ErrorHandler {

        @Override
        public void warning(SAXParseException _exception) throws SAXException {
        }

        @Override
        public void error(SAXParseException _exception) throws SAXException {
        }

        @Override
        public void fatalError(SAXParseException _exception) throws SAXException {
        }

    }

    /**
     * XML Error Handler which will throw RuntimeException if any Exception was thrown.
     *
     * @author hypfvieh
     * @since v1.0.3 - 2018-01-10
     */
    public static class XmlErrorHandlerRuntimeException implements ErrorHandler {

        @Override
        public void warning(SAXParseException _exception) throws SAXException {
            throw new RuntimeException(_exception);
        }

        @Override
        public void error(SAXParseException _exception) throws SAXException {
            throw new RuntimeException(_exception);
        }

        @Override
        public void fatalError(SAXParseException _exception) throws SAXException {
            throw new RuntimeException(_exception);
        }

    }
}
