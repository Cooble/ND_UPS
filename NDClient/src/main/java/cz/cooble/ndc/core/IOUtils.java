package cz.cooble.ndc.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static cz.cooble.ndc.Asserter.ASSERT;
import static cz.cooble.ndc.Asserter.ND_RESLOC;
import static org.lwjgl.BufferUtils.createByteBuffer;
import static org.lwjgl.system.MemoryUtil.memSlice;

public class IOUtils {
    public static String readFile(String filePath) {
        try(var stream = new BufferedReader(new InputStreamReader(Utils.class.getClassLoader().getResourceAsStream(ND_RESLOC(filePath))))) {
            return stream.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            ASSERT(false,"File not found: "+filePath);
            e.printStackTrace();
        }
        return null;
    }
    public static List<String> readFileLines(String filePath) {
        try(var stream = new BufferedReader(new InputStreamReader(Utils.class.getClassLoader().getResourceAsStream(ND_RESLOC(filePath))))) {
            return stream.lines().collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }

    /**
     * Reads the specified resource and returns the raw data as a ByteBuffer.
     *
     * @param resource   the resource to read
     * @param bufferSize the initial buffer size
     *
     * @return the resource data
     *
     * @throws IOException if an IO error occurs
     */
    public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer=null;

        Path path = Paths.get(resource);
        if (Files.isReadable(path)) {
            try (SeekableByteChannel fc = Files.newByteChannel(path)) {
                buffer = createByteBuffer((int)fc.size() + 1);
                while (fc.read(buffer) != -1) {
                    ;
                }
            }
        } else {
            try (
                    InputStream source = IOUtils.class.getClassLoader().getResourceAsStream(resource);
                    ReadableByteChannel rbc = Channels.newChannel(source)
            ) {
                buffer = createByteBuffer(bufferSize);

                while (true) {
                    int bytes = rbc.read(buffer);
                    if (bytes == -1) {
                        break;
                    }
                    if (buffer.remaining() == 0) {
                        buffer = resizeBuffer(buffer, buffer.capacity() * 3 / 2); // 50%
                    }
                }
            }
            catch (Exception e){
                System.err.println("Cannot load file  "+resource);
            }
        }
        if(buffer==null)
            return null;

        buffer.flip();
        return memSlice(buffer);
    }
}
