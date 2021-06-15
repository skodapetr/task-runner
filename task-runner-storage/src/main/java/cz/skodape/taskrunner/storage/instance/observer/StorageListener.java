package cz.skodape.taskrunner.storage.instance.observer;

import cz.skodape.taskrunner.storage.instance.TaskReference;

public interface StorageListener {

    default void onNewTask(TaskReference reference) {
        // Do nothing.
    }

    default void onDeleteTask(TaskReference reference) {
        // Do nothing.
    }

}
