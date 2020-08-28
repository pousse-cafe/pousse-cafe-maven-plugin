package poussecafe.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
import poussecafe.source.Scanner;
import poussecafe.source.model.Model;
import poussecafe.source.model.ProcessModel;

/**
 * <p>Lists all process names detected in a given source tree.</p>
 */
@Mojo(
    name = "list-processes",
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDirectInvocation = true
)
public class ListProcessesMojo extends AbstractMojo {

    @Override
    public void execute()
            throws MojoExecutionException,
            MojoFailureException {
        configureClassPath();
        var model = buildModel();
        listProcess(model);
    }

    private void configureClassPath() throws MojoExecutionException {
        try {
            List<String> runtimeClasspathElements = project.getRuntimeClasspathElements();
            ClassRealm realm = descriptor.getClassRealm();
            for (String element : runtimeClasspathElements) {
                File elementFile = new File(element);
                realm.addURL(elementFile.toURI().toURL());
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to configure classpath", e);
        }
    }

    private Model buildModel() throws MojoExecutionException {
        var scanner = new Scanner.Builder().build();
        for(String pathName : project.getCompileSourceRoots()) {
            Path path = Path.of(pathName);
            if(getLog().isDebugEnabled()) {
                getLog().debug("Including tree " + path);
            }
            try {
                scanner.includeTree(path);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to include tree " + pathName, e);
            }
        }

        return scanner.model();
    }

    private void listProcess(Model model) {
        getLog().info("Found " + model.processes().size() + " processes:");
        for(ProcessModel process : model.processes()) {
            getLog().info("- " + process.simpleName());
        }
    }

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor descriptor;
}
