package net.tkausl.pump;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InputStreamPumper implements Runnable {

    private final InputStream is;
    private final OutputStream os;

    public InputStreamPumper(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
    }

    /**
     * Copies data from the input stream to the output stream.
     *
     * Terminates as soon as the input stream is closed or an error occurs.
     */
    public void run() {
        try {
            byte[] buf = new byte[256];
            int len;
            while ((len = is.read(buf)) != -1) {
                os.write(buf, 0, len);
                os.flush();
            }
        } catch (Exception e) {
        } finally {
            try {
                os.close();
                is.close();
            } catch (IOException ex) {
            }
        }
    }
}
