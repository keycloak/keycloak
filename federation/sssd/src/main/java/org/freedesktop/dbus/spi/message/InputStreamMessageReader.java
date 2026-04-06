package org.freedesktop.dbus.spi.message;

import java.nio.channels.SocketChannel;
import java.util.List;

import org.freedesktop.dbus.FileDescriptor;

public class InputStreamMessageReader extends AbstractInputStreamMessageReader {

    public InputStreamMessageReader(final SocketChannel _in) {
        super(_in, DefaultSocketProvider.INSTANCE);
    }

    @Override
    protected List<FileDescriptor> readFileDescriptors(SocketChannel _inputChannel) {
        return null;
    }

}
