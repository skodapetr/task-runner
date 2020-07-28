package cz.skodape.taskrunner.storage.template;

import cz.skodape.taskrunner.storage.TestResources;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class SpecificationStorageTest {

    @Test
    public void load() {
        File directory = TestResources.file("./specification");
        TaskTemplateStorage storage = new TaskTemplateStorage();
        storage.load(directory);
        Assertions.assertNotNull(storage.getTemplate("add-integer"));
        Assertions.assertNotNull(storage.getTemplate("add-string"));
        //
        TaskTemplate template = storage.getTemplate("add-string");
        Assertions.assertEquals("add-string", template.urlPath);
        Assertions.assertTrue(template.allowInputFiles);
        Assertions.assertTrue(template.createOnGet);
        Assertions.assertTrue(template.mergeErrOutToStdOut);
    }

}
