package cz.skodape.taskrunner.storage.template.model;

import cz.skodape.taskrunner.storage.SuppressFBWarnings;
import cz.skodape.taskrunner.storage.instance.model.TaskStep;

import java.util.ArrayList;
import java.util.List;

@SuppressFBWarnings(value = {"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
public class TaskTemplate {

    public String id;

    /**
     * Named used in the URL.
     */
    public String urlPath;

    /**
     * If true user can upload input files.
     */
    public boolean allowInputFiles = false;

    /**
     * If set a new task is created on GET request if requested task
     * does not exist. Can be used only for tasks with not input. The GET
     * request must be for the template status ./{template}/{id}.
     */
    public boolean createOnGet = false;

    /**
     * If true, then error output is merged to standard output.
     */
    public boolean mergeErrOutToStdOut = false;

    /**
     * Prevent any changes to database content.
     */
    public boolean readOnly = false;

    /**
     * Disable listing of tasks. List of all tasks can not be retrieved
     * using HTTP endpoint. Use for large number of tasks.
     */
    public boolean disableListing = false;

    /**
     * If set define in seconds the expiration time of the task after the
     * task is finished. The task can be deleted any time after the time to
     * live expires. Use null to never delete the task.
     */
    public Integer timeToLive = null;

    /**
     * Normalize task identification created using HTTP GET interface, does
     * not apply to other requests.
     */
    public StringTransformation taskGetIdentificationTransformation
            = StringTransformation.None;

    /**
     * If true and user request file named "file.txt" and the file does not
     * exists in the public directory, then we should try to serve
     * "file.txt.gz" instead.
     */
    public boolean allowGzipPublicFiles = false;

    /**
     * If set the response to POST is redirect on given URL. The URL
     * constructed from a template.
     * See {@link cz.skodape.taskrunner.http.CreateRedirectUrl} for details
     * on the placeholders.
     */
    public String postResponseRedirectUrl = null;

    public List<TaskStep> steps = new ArrayList<>();

}
