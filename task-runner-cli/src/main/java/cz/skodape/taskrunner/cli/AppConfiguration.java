package cz.skodape.taskrunner.cli;

import cz.skodape.taskrunner.storage.SuppressFBWarnings;

import java.io.File;

@SuppressFBWarnings(value = {"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
public class AppConfiguration {

    public File templateDirectory;

    /**
     * If set task in given directory is executed.
     */
    public File taskToRun;

    public File taskDirectory;

    /**
     * Should be on the same file system as taskDirectory.
     */
    public File workingDirectory;

    /**
     * If set executor for given storage directory is started.
     */
    public Integer executorThreads;

    /**
     * If set HTTP server for given storage directory is started.
     */
    public Integer httpPort = 8050;

}
