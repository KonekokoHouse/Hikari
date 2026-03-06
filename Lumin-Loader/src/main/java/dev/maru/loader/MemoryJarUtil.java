package dev.maru.loader;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MemoryJarUtil {

    /**
     * 将 Mod 的字节数组内容"解压"到一个内存文件系统中
     *
     * @param jarData Mod 的 jar 文件字节数据
     * @return 内存文件系统的根路径 (Path)
     */
    public static Path loadJarToMemoryFileSystem(byte[] jarData) throws IOException {
        // 创建一个基于内存的文件系统 (模拟 Unix 路径结构)
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        Path root = fs.getPath("/");

        // 读取字节流为 Zip
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(jarData))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = root.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    // 确保父目录存在
                    Path parent = entryPath.getParent();
                    if (parent != null && !Files.exists(parent)) {
                        Files.createDirectories(parent);
                    }
                    // 将文件内容写入内存路径
                    Files.copy(zis, entryPath);
                }
            }
        }

        return root;
    }
}
