package poussecafe.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import poussecafe.doc.PousseCafeDocletConfiguration;
import poussecafe.doc.PousseCafeDocletExecutor;

import static java.util.Collections.emptyList;

/**
 * <p>Generates an expert-readable documentation of the Model. The documentation is generated in HTML and as a PDF file.</p>
 * <p>Documentation is generated by analyzing source code. Further instructions about how to enable documentation generation
 * can be found <a href="https://www.pousse-cafe-framework.org/doc/reference-guide/#generating-ddd-documentation">here</a>.</p>
 */
@Mojo(
    name = "generate-doc",
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class GenerateDocMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(!Boolean.parseBoolean(skipDoc)) {
            List<String> sourcePath = getSourcePath();
            List<String> classPath = getClassPath();

            PousseCafeDocletConfiguration configuration = new PousseCafeDocletConfiguration.Builder()
                    .domainName(domainName)
                    .version(version)
                    .sourcePath(sourcePath)
                    .outputDirectory(outputDirectory.getAbsolutePath())
                    .pdfFileName(pdfFileName)
                    .basePackage(basePackage)
                    .classPath(classPath)
                    .customDotExecutable(Optional.ofNullable(customDotExecutable))
                    .customFdpExecutable(Optional.ofNullable(customFdpExecutable))
                    .build();

            new PousseCafeDocletExecutor(configuration).execute();
        }
    }

    private List<String> getSourcePath() {
        List<String> sourcePath = new ArrayList<>();
        sourcePath.addAll(project.getCompileSourceRoots());
        sourcePath.addAll(sourceDependenciesFiles());
        return sourcePath;
    }

    private List<String> sourceDependenciesFiles() {
        List<String> sourceDependenciesFiles = new ArrayList<>();
        for(Artifact artifact : project.getArtifacts()) {
            if(artifact.hasClassifier() && artifact.getClassifier().equals("sources")) {
                sourceDependenciesFiles.add(artifact.getFile().getAbsolutePath());
            }
        }
        return sourceDependenciesFiles;
    }

    private List<String> getClassPath() {
        List<String> classPath;
        try {
            classPath = project.getCompileClasspathElements();
        } catch (DependencyResolutionRequiredException e) {
            classPath = emptyList();
        }
        return classPath;
    }

    /**
     * The name of the domain represented by the Model. The name is essentially used in the title of the document.
     *
     * @since 0.6
     */
    @Parameter(property = "domainName", required = true)
    private String domainName;

    /**
     * The version of the documentation. It is generally synchronized with the version of the source code. The version
     * is displayed in the subtitle of the document.
     *
     * @since 0.6
     */
    @Parameter(defaultValue = "${project.version}", property = "version", required = true)
    private String version;

    /**
     * The output directory for generated documentation files (HTML, PDF, ...).
     *
     * @since 0.6
     */
    @Parameter(defaultValue = "${basedir}/target/ddd-doc/", property = "outputDirectory", required = true)
    private File outputDirectory;

    /**
     * The base package in which domain components are looked for. Any class not being in the base package or one of
     * its sub-packages will be ignored.
     *
     * @since 0.6
     */
    @Parameter(property = "basePackage", required = true)
    private String basePackage;

    /**
     * The path to <a href="http://www.graphviz.org">Graphviz</a> 'dot' executable. Dot is used to generate process
     * graphs.
     *
     * @since 0.7
     */
    @Parameter(property = "customDotExecutable")
    private String customDotExecutable;

    /**
     * The path to <a href="http://www.graphviz.org">Graphviz</a> 'fdp' executable. Fdp is used to generate relational
     * graphs (i.e. graphs showing the relation between Entities, Value Objects, ...).
     *
     * @since 0.7
     */
    @Parameter(property = "customFdpExecutable")
    private String customFdpExecutable;

    /**
     * The file name for generated PDF file.
     *
     * @since 0.9
     */
    @Parameter(defaultValue = "${project.artifactId}-${project.version}.pdf", property = "pdfFileName", required = true)
    private String pdfFileName;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Tells not to generate documentation. This flag can be used when the goal execution has been bound to a phase
     * in the POM but one would like to skip it.
     *
     * @since 0.9
     */
    @Parameter(property = "skipDoc", required = true, defaultValue = "false")
    private String skipDoc;
}
