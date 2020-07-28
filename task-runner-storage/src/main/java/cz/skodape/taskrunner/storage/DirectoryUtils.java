package cz.skodape.taskrunner.storage;

import java.io.File;

public class DirectoryUtils {

    public static boolean delete(File directoryToBeDeleted) {
        File[] files = directoryToBeDeleted.listFiles();
        if (files != null) {
            for (File file : files) {
                delete(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

}
