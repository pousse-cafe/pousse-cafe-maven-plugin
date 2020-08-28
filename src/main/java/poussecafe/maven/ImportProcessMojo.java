package poussecafe.maven;

import java.io.File;
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
 * <p>Imports a process described using
 * <a href="https://github.com/pousse-cafe/pousse-cafe/wiki/Introduction-to-EMIL" target="_blank">EMIL</a> language
 * i.e. generates missing types and methods in the code base.</p>
 */
@Mojo(
    name = "import-process",
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDirectInvocation = true
)
public class ImportProcessMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        classPathConfigurator.configureClassPath(project, descriptor);
        var model = modelOperations.buildModelFromEmil(getLog(), emilFile, basePackage);
        modelOperations.importProcess(model, sourceDirectory);
    }

    @Inject
    private ClassPathConfigurator classPathConfigurator;

    @Inject
    private ModelOperations modelOperations;

    /**
     * The path to an EMIL file describing the process to import.
     *
     * @since 0.17
     */
    @Parameter(property = "emilFile", required = true)
    private File emilFile;

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
