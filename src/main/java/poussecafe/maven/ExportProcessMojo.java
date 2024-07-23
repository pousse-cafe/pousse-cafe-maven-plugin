package poussecafe.maven;

import java.io.File;
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
 * <p>Exports a selected process or all processes into
 * <a href="https://github.com/pousse-cafe/pousse-cafe/wiki/Introduction-to-EMIL" target="_blank">EMIL</a> language
 * and outputs the result to a given file.</p>
 */
@Mojo(
    name = "export-process",
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class ExportProcessMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        classPathConfigurator.configureClassPath(project, descriptor);
        var model = modelOperations.buildModelFromSource(getLog(), project);
        modelOperations.exportProcess(model, Optional.ofNullable(processName), emilFile);
    }

    @Inject
    private ClassPathConfigurator classPathConfigurator;

    @Inject
    private ModelOperations modelOperations;

    /**
     * The name of the process to export. If no name is provided, then all processes are exported.
     *
     * @since 0.15
     */
    @Parameter(property = "processName")
    private String processName;

    /**
     * The path to the output file.
     *
     * @since 0.17
     */
    @Parameter(property = "emilFile", required = true)
    private File emilFile;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor descriptor;
}
