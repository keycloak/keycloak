package org.freedesktop.dbus.spi.message;

import java.nio.channels.SocketChannel;
import java.util.List;

import org.freedesktop.dbus.FileDescriptor;

public class OutputStreamMessageWriter extends AbstractOutputStreamMessageWriter {

    public OutputStreamMessageWriter(SocketChannel _out) {
        super(_out, DefaultSocketProvider.INSTANCE);
    }

    @Override
    protected void writeFileDescriptors(SocketChannel _outputChannel, List<FileDescriptor> _filedescriptors) {
        // not supported
    }

}
