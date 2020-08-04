package poussecafe.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
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
import poussecafe.source.emil.EmilExporter;
import poussecafe.source.model.MessageListener;
import poussecafe.source.model.Model;

/**
 * <p>Exports a selected process or all processes into
 * <a href="https://github.com/pousse-cafe/pousse-cafe/wiki/Introduction-to-EMIL" target="_blank">EMIL</a> language
 * and outputs the result in Maven logs.</p>
 */
@Mojo(
    name = "export-process",
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDirectInvocation = true
)
public class ExportProcessMojo extends AbstractMojo {

    @Override
    public void execute()
            throws MojoExecutionException,
            MojoFailureException {
        configureClassPath();
        var model = buildModel();
        exportProcess(model);
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

        var model = scanner.model();
        if(getLog().isDebugEnabled()) {
            var allListeners = model.messageListeners();
            getLog().debug("Detected " + allListeners.size() + " listeners:");
            for(MessageListener listener : allListeners) {
                getLog().debug("- " + listener.consumedMessage() + " -> " + listener.container() + "[" + listener.methodName()+ "]");
            }
            getLog().debug("");

            var processListeners = model.processListeners(processName);
            getLog().debug("Detected " + processListeners.size() + " process listeners:");
            for(MessageListener listener : processListeners) {
                getLog().debug("- " + listener.consumedMessage() + " -> " + listener.container() + "[" + listener.methodName()+ "]");
            }
        }
        return model;
    }

    private void exportProcess(Model model) {
        EmilExporter exporter = new EmilExporter.Builder()
                .model(model)
                .processName(Optional.ofNullable(processName))
                .build();
        getLog().info("Process: " + Optional.ofNullable(processName).orElse("all") + "\n" + exporter.toEmil() + "\n");
    }

    /**
     * The name of the process to export. If no name is provided, then all processes are exported.
     *
     * @since 0.15
     */
    @Parameter(property = "processName")
    private String processName;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor descriptor;
}