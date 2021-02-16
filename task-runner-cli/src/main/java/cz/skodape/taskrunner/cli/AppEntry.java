package cz.skodape.taskrunner.cli;

import cz.skodape.taskrunner.cli.action.RunTaskCommand;
import cz.skodape.taskrunner.cli.action.StartServices;
import cz.skodape.taskrunner.storage.SuppressFBWarnings;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;

public class AppEntry {

    public static void main(String[] args) {
        (new AppEntry()).run(args);
    }

    public void run(String[] args) {
        AppConfiguration configuration = loadConfiguration(parseArgs(args));
        // Execute task if there is any.
        if (configuration.taskToRun != null) {
            (new RunTaskCommand(configuration)).execute();
            return;
        }
        // In every case start all services that we should start.
        (new StartServices(configuration)).execute();
    }

    @SuppressFBWarnings(value = {"DM_EXIT"})
    private CommandLine parseArgs(String[] args) {
        Options options = new Options();

        options.addOption(Option.builder()
                .longOpt("WorkingDirectory")
                .hasArg(true)
                .desc("Working directory used for temporary and working files.")
                .required(true)
                .build());

        options.addOption(Option.builder()
                .longOpt("TemplatesDirectory")
                .hasArg(true)
                .desc("Directory with task templates.")
                .required(true)
                .build());

        options.addOption(Option.builder()
                .longOpt("RunTaskDirectory")
                .hasArg(true)
                .desc("Run task in given directory.")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("TaskDirectory")
                .hasArg(true)
                .desc("Task storage directory.")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("WorkerCount")
                .hasArg(true)
                .desc("Number of worker threads.")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("HttpPort")
                .hasArg(true)
                .desc("Port to start HTTP server on.")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("RestartTasks")
                .hasArg(false)
                .desc("Re-queue all running tasks.")
                .required(false)
                .build());

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
            // Some IDEs fail to detect above as application end.
            throw new RuntimeException();
        }
    }

    private AppConfiguration loadConfiguration(CommandLine cmd) {
        AppConfiguration configuration = new AppConfiguration();
        configuration.templateDirectory =
                new File(cmd.getOptionValue("TemplatesDirectory"));
        if (cmd.hasOption("WorkingDirectory")) {
            configuration.workingDirectory =
                    new File(cmd.getOptionValue("WorkingDirectory"));
        }
        if (cmd.hasOption("RunTaskDirectory")) {
            configuration.taskToRun =
                    new File(cmd.getOptionValue("RunTaskDirectory"));
        }
        if (cmd.hasOption("TaskDirectory")) {
            configuration.taskDirectory =
                    new File(cmd.getOptionValue("TaskDirectory"));
        }
        if (cmd.hasOption("WorkerCount")) {
            configuration.executorThreads =
                    Integer.parseInt(cmd.getOptionValue("WorkerCount"));
        }
        if (cmd.hasOption("HttpPort")) {
            configuration.httpPort =
                    Integer.parseInt(cmd.getOptionValue("HttpPort"));
        }
        if (cmd.hasOption("RestartTasks")) {
            configuration.restartRunningTasks = true;
        }
        return configuration;
    }

}
