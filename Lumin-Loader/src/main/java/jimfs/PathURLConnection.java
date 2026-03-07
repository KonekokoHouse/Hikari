package jimfs;

import com.google.common.base.Ascii;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.jspecify.annotations.Nullable;

public final class PathURLConnection extends URLConnection {
    private static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss 'GMT'";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private InputStream stream;
    private ImmutableListMultimap<String, String> headers = ImmutableListMultimap.of();

    public PathURLConnection(URL url) {
        super(Preconditions.checkNotNull(url));
    }

    @Override
    public void connect() throws IOException {
        long length;
        if (this.stream != null) {
            return;
        }
        Path path = Paths.get(PathURLConnection.toUri(this.url));
        if (Files.isDirectory(path, new LinkOption[0])) {
            StringBuilder builder = new StringBuilder();
            try (DirectoryStream<Path> files = Files.newDirectoryStream(path);){
                for (Path file : files) {
                    builder.append(file.getFileName()).append('\n');
                }
            }
            byte[] bytes = builder.toString().getBytes(StandardCharsets.UTF_8);
            this.stream = new ByteArrayInputStream(bytes);
            length = bytes.length;
        } else {
            this.stream = Files.newInputStream(path, new OpenOption[0]);
            length = Files.size(path);
        }
        FileTime lastModified = Files.getLastModifiedTime(path, new LinkOption[0]);
        String contentType = MoreObjects.firstNonNull(Files.probeContentType(path), DEFAULT_CONTENT_TYPE);
        ImmutableListMultimap.Builder builder = ImmutableListMultimap.builder();
        builder.put("content-length", "" + length);
        builder.put("content-type", contentType);
        if (lastModified != null) {
            SimpleDateFormat format = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            builder.put("last-modified", format.format(new Date(lastModified.toMillis())));
        }
        this.headers = builder.build();
    }

    private static URI toUri(URL url) throws IOException {
        try {
            return url.toURI();
        }
        catch (URISyntaxException e) {
            throw new IOException("URL " + url + " cannot be converted to a URI", e);
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        this.connect();
        return this.stream;
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        try {
            this.connect();
        }
        catch (IOException e) {
            return ImmutableMap.of();
        }
        return (Map<String, List<String>>) (Map<?, ?>) this.headers.asMap();
    }

    @Override
    public @Nullable String getHeaderField(String name) {
        try {
            this.connect();
        }
        catch (IOException e) {
            return null;
        }
        return Iterables.getFirst(this.headers.get(Ascii.toLowerCase(name)), null);
    }
}
