package poussecafe.maven;

import java.io.IOException;
import java.nio.file.Path;
import javax.inject.Inject;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import poussecafe.source.validation.ValidationMessage;
import poussecafe.source.validation.ValidationMessageType;
import poussecafe.source.validation.ValidationResult;
import poussecafe.source.validation.Validator;

/**
 * <p>Validates the project source code.</p>
 * <p>Error or warning messages are generated. The build fails if errors are detected.</p>
 */
@Mojo(
    name = "validate",
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class ValidateMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        classPathConfigurator.configureClassPath(project, descriptor);

        var validator = new Validator();
        for(String pathName : project.getCompileSourceRoots()) {
            Path path = Path.of(pathName);
            try {
                validator.includeTree(path);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to include " + path, e);
            }
        }
        validator.validate();
        var result = validator.result();
        var logger = getLog();
        if(result.messages().isEmpty()) {
            logger.info("No validation message.");
        } else {
            for(ValidationMessage message : result.messages()) {
                if(message.type() == ValidationMessageType.WARNING) {
                    logger.warn(prefix(message) + message.message());
                } else if(message.type() == ValidationMessageType.ERROR) {
                    logger.error(prefix(message) + message.message());
                } else {
                    throw new MojoExecutionException("Unsupported message type " + message.type());
                }
            }
            if(buildShouldFail(result)) {
                throw new MojoFailureException("Validation errors were detected");
            }
        }
    }

    private String prefix(ValidationMessage message) {
        var path = message.location().sourceFile().id();
        var line = message.location().line();
        return path + " at line " + line + ": ";
    }

    @Inject
    private ClassPathConfigurator classPathConfigurator;

    private boolean buildShouldFail(ValidationResult result) {
        return result.hasError() || (failOnWarn && result.hasWarning());
    }

    /**
     * If true, makes the build fail with warnings.
     *
     * @since 0.19
     */
    @Parameter(defaultValue = "false")
    private boolean failOnWarn;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor descriptor;
}
