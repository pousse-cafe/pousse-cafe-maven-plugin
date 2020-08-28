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

/**
 * <p>Updates a process. This goal is equivalent to calling export-process, edit the generated file
 * then call import-process.</p>
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
            var model = modelOperations.buildModelFromSource(project);
            var temporaryFile = File.createTempFile(processName, ".emil");
            modelOperations.exportProcess(model, Optional.ofNullable(processName), temporaryFile);
            var initialContent = Files.readString(temporaryFile.toPath());
            editEmil(temporaryFile);
            var newContent = Files.readString(temporaryFile.toPath());
            if(sameContent(initialContent, newContent)) {
                getLog().info("No change detected, skipping update");
            } else {
                var newModel = modelOperations.buildModelFromEmil(getLog(), temporaryFile, basePackage);
                modelOperations.importProcess(newModel, sourceDirectory);
            }
        } catch (IOException e) {
            throw new MojoFailureException("Unable to update process", e);
        }
    }

    private void editEmil(File temporaryFile) throws IOException, MojoFailureException {
        String defaultEditor = System.getenv("EDITOR");
        if(defaultEditor == null) {
            defaultEditor = "vim";
        }

        var editorProcess = new ProcessBuilder(defaultEditor, temporaryFile.getAbsolutePath())
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

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor descriptor;
}
