package dev.maru.api.jimfs;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;

final class JimfsFileSystems {
    private static final Runnable DO_NOTHING = new Runnable() {

        @Override
        public void run() {
        }
    };

    private JimfsFileSystems() {
    }

    private static Runnable removeFileSystemRunnable(URI uri) {
        if (Jimfs.systemProvider == null) {
            return DO_NOTHING;
        }
        try {
            Method method = Jimfs.systemProvider.getClass().getDeclaredMethod("removeFileSystemRunnable", URI.class);
            return (Runnable) method.invoke(null, uri);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException("Unable to get Runnable for removing the FileSystem from the cache when it is closed", e);
        }
    }

    public static JimfsFileSystem newFileSystem(JimfsFileSystemProvider provider, URI uri, Configuration config) throws IOException {
        PathService pathService = new PathService(config);
        FileSystemState state = new FileSystemState(config.fileTimeSource, JimfsFileSystems.removeFileSystemRunnable(uri));
        JimfsFileStore fileStore = JimfsFileSystems.createFileStore(config, pathService, state);
        FileSystemView defaultView = JimfsFileSystems.createDefaultView(config, fileStore, pathService);
        WatchServiceConfiguration watchServiceConfig = config.watchServiceConfig;
        JimfsFileSystem fileSystem = new JimfsFileSystem(provider, uri, fileStore, pathService, defaultView, watchServiceConfig);
        pathService.setFileSystem(fileSystem);
        return fileSystem;
    }

    private static JimfsFileStore createFileStore(Configuration config, PathService pathService, FileSystemState state) {
        AttributeService attributeService = new AttributeService(config);
        HeapDisk disk = new HeapDisk(config);
        FileFactory fileFactory = new FileFactory(disk, config.fileTimeSource);
        HashMap<Name, Directory> roots = new HashMap<Name, Directory>();
        for (String root : config.roots) {
            JimfsPath path = pathService.parsePath(root, new String[0]);
            if (!path.isAbsolute() && path.getNameCount() == 0) {
                throw new IllegalArgumentException("Invalid root path: " + root);
            }
            Name rootName = path.root();
            Directory rootDir = fileFactory.createRootDirectory(rootName);
            attributeService.setInitialAttributes(rootDir, new FileAttribute[0]);
            roots.put(rootName, rootDir);
        }
        return new JimfsFileStore(new FileTree(roots), fileFactory, disk, attributeService, config.supportedFeatures, state);
    }

    private static FileSystemView createDefaultView(Configuration config, JimfsFileStore fileStore, PathService pathService) throws IOException {
        JimfsPath workingDirPath = pathService.parsePath(config.workingDirectory, new String[0]);
        Directory dir = fileStore.getRoot(workingDirPath.root());
        if (dir == null) {
            throw new IllegalArgumentException("Invalid working dir path: " + workingDirPath);
        }
        for (Name name : workingDirPath.names()) {
            Directory newDir = fileStore.directoryCreator().get();
            fileStore.setInitialAttributes(newDir, new FileAttribute[0]);
            dir.link(name, newDir);
            dir = newDir;
        }
        return new FileSystemView(fileStore, dir, workingDirPath);
    }
}
