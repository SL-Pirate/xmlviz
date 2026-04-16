package dev.isira.xmlviz.parsing;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream wrapper that counts the number of bytes read.
 * Used to track approximate byte offsets during StAX parsing.
 */
public class CountingInputStream extends FilterInputStream {
    private long bytesRead = 0;

    public CountingInputStream(InputStream in) {
        super(in);
    }

    public long getBytesRead() {
        return bytesRead;
    }

    @Override
    public int read() throws IOException {
        int result = super.read();
        if (result != -1) bytesRead++;
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = super.read(b, off, len);
        if (result > 0) bytesRead += result;
        return result;
    }

    @Override
    public long skip(long n) throws IOException {
        long result = super.skip(n);
        bytesRead += result;
        return result;
    }
}
