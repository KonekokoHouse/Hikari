package dev.maru.loader.protocols.jimfs;

import jimfs.PathURLConnection;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new PathURLConnection(url);
    }

}
