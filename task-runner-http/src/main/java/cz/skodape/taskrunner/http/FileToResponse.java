package cz.skodape.taskrunner.http;

import org.apache.tika.Tika;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileToResponse {

    private final Tika tika = new Tika();

    public Response streamFile(File file) {
        if (!file.exists()) {
            return notFound();
        }
        return Response
                .ok((StreamingOutput) (output) -> fileToStream(file, output))
                .header("Content-Type", getContentType(file))
                .build();
    }

    private static Response notFound() {
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    private String getContentType(File file) {
        return tika.detect(file.toString());
    }

    private static void fileToStream(
            File file, OutputStream output) throws IOException {
        try (InputStream stream = new FileInputStream(file)) {
            stream.transferTo(output);
        }
    }

    public Response streamFileOrGzip(File file) {
        if (file.exists()) {
            return streamFile(file);
        }
        File gzFile = new File(file.getParentFile(), file.getName() + ".gz");
        if (!gzFile.exists()) {
            return notFound();
        }
        return Response
                .ok((StreamingOutput) (output) -> fileToStream(gzFile, output))
                .header("Content-Type", getContentType(file))
                .header("Content-Encoding", "gzip")
                .build();
    }

}
