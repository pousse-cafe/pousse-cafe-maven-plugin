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
import poussecafe.storage.internal.InternalStorage;

import static poussecafe.collection.Collections.asSet;

/**
 * <p>Imports a process described using
 * <a href="https://github.com/pousse-cafe/pousse-cafe/wiki/Introduction-to-EMIL" target="_blank">EMIL</a> language
 * i.e. generates missing types (aggregate classes, commands, events, adapters, etc.) and methods in the code base.</p>
 *
 * <p>Added method's implementation is initially empty.</p>
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
        var newModel = modelOperations.buildModelFromEmil(getLog(), emilFile, basePackage);
        modelOperations.importModel(Optional.empty(), newModel, sourceDirectory, asSet(storageAdapters),
                Optional.ofNullable(codeFormatterProfile));
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

    /**
     * List of storage adapters to create. Storage name is used to select them. By default, only internal storage
     * classes are generated. Currently, supported storage names are: "internal", "spring-mongo", "spring-jpa".
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
