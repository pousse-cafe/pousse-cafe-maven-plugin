package poussecafe.maven;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import poussecafe.storage.internal.InternalStorage;

import static poussecafe.collection.Collections.asSet;

/**
 * <p>Updates a process. This goal is equivalent to calling <code>export-process</code>, edit the generated file
 * then call <code>import-process</code>.</p>
 *
 * <p>The text editor is selected using $EDITOR environment variable. If the variable is empty, then vim is being used
   as the default text editor.</p>
 */
@Mojo(
    name = "update-process",
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDirectInvocation = true
)
public class UpdateProcessMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        classPathConfigurator.configureClassPath(project, descriptor);

        try {
            var currentModel = modelOperations.buildModelFromSource(project);
            var temporaryFile = File.createTempFile(processName, ".emil");
            modelOperations.exportProcess(currentModel, Optional.ofNullable(processName), temporaryFile);
            var initialContent = Files.readString(temporaryFile.toPath());
            editEmil(temporaryFile);
            var newContent = Files.readString(temporaryFile.toPath());
            if(sameContent(initialContent, newContent)) {
                getLog().info("No change detected, skipping update");
            } else {
                var newModel = modelOperations.buildModelFromEmil(getLog(), temporaryFile, basePackage);
                modelOperations.importModel(Optional.of(currentModel), newModel, sourceDirectory,
                        asSet(storageAdapters), Optional.ofNullable(codeFormatterProfile));
            }
        } catch (IOException e) {
            throw new MojoFailureException("Unable to update process", e);
        }
    }

    private void editEmil(File temporaryFile) throws IOException, MojoFailureException {
        String editor = System.getenv("EDITOR");
        if(editor == null) {
            editor = "vim";
        }

        var editorProcess = new ProcessBuilder(editor, temporaryFile.getAbsolutePath())
                .redirectError(Redirect.INHERIT)
                .redirectOutput(Redirect.INHERIT)
                .redirectInput(Redirect.INHERIT)
                .start();
        try {
            editorProcess.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoFailureException("Interrupted while waiting for editor process", e);
        }
    }

    @Inject
    private ClassPathConfigurator classPathConfigurator;

    @Inject
    private ModelOperations modelOperations;

    private boolean sameContent(String initialContent, String newContent) {
        var initialNormalized = initialContent.strip().replaceAll("[\\s]+", " ");
        var newNormalized = newContent.strip().replaceAll("[\\s]+", " ");
        return initialNormalized.equals(newNormalized);
    }

    /**
     * The name of the process to update.
     *
     * @since 0.17
     */
    @Parameter(property = "processName", required = true)
    private String processName;

    /**
     * The base package for generated code.
     *
     * @since 0.17
     */
    @Parameter(property = "basePackage", required = true)
    private String basePackage;

    /**
     * Path of the folder containing the source code. Classes and packages will be created in this folder.
     *
     * @since 0.17
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}", property = "sourceDirectory", required = true)
    private File sourceDirectory;

    /**
     * List of storage adapters to create. Storage name is used to select them. By default, only internal storage
     * classes are generated. Currently, supported storage names are: "Internal", "SpringMongo", "SpringJpa".
     *
     * @since 0.17
     */
    @Parameter(defaultValue = InternalStorage.NAME, property = "storageAdapters", required = true)
    private String[] storageAdapters;

    /**
     * Path to a JDT code formatter profile file. This kind of file may be exported directly using Eclipse.
     *
     * @since 0.17
     */
    @Parameter(property = "codeFormatterProfile")
    private File codeFormatterProfile;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor descriptor;
}
