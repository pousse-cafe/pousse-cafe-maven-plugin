package poussecafe.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import poussecafe.source.emil.parser.TreeAnalyzer;
import poussecafe.source.emil.parser.TreeParser;
import poussecafe.source.generation.CoreCodeGenerator;
import poussecafe.source.model.Model;

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
    public void execute()
            throws MojoExecutionException,
            MojoFailureException {
        configureClassPath();
        var model = buildModel();
        importProcess(model);
    }

    private void configureClassPath() throws MojoFailureException {
        try {
            List<String> runtimeClasspathElements = project.getRuntimeClasspathElements();
            ClassRealm realm = descriptor.getClassRealm();
            for (String element : runtimeClasspathElements) {
                File elementFile = new File(element);
                realm.addURL(elementFile.toURI().toURL());
            }
        } catch (Exception e) {
            throw new MojoFailureException("Unable to configure classpath", e);
        }
    }

    private Model buildModel() throws MojoExecutionException {
        try(var inputStream = new FileInputStream(emilFile)) {
            var tree = TreeParser.parseInputStream(inputStream);
            if(!tree.isValid()) {
                getLog().error("Unable to parse " + emilFile);
                for(String error : tree.errors()) {
                    getLog().error(error);
                }
                throw new MojoExecutionException("Unable to parse " + emilFile);
            }

            var analyzer = new TreeAnalyzer.Builder()
                    .tree(tree)
                    .basePackage(basePackage)
                    .build();
            analyzer.analyze();

            return analyzer.model();
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to build model from file " + emilFile, e);
        }
    }

    private void importProcess(Model model) {
        var generator = new CoreCodeGenerator.Builder()
                .sourceDirectory(sourceDirectory.toPath())
                .build();
        generator.generate(model);
    }

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
