package cz.skodape.taskrunner.storage.instance.model;

import cz.skodape.taskrunner.storage.SuppressFBWarnings;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds task configuration we allow only for a simple values for security
 * reasons.
 */
@SuppressFBWarnings(value = {"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
public class TaskConfiguration {

    public Map<String, String> configuration = new HashMap<>();

    public TaskConfiguration() {
    }

    public TaskConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }

}
