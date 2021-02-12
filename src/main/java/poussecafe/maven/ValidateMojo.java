package poussecafe.maven;

import java.io.IOException;
import java.nio.file.Path;
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
import poussecafe.discovery.ReflectionsWrapper;
import poussecafe.source.analysis.ClassLoaderClassResolver;
import poussecafe.source.validation.ReflectionsClassPathExplorer;
import poussecafe.source.validation.ValidationMessage;
import poussecafe.source.validation.ValidationMessageType;
import poussecafe.source.validation.ValidationModelBuilder;
import poussecafe.source.validation.ValidationResult;
import poussecafe.source.validation.Validator;
import poussecafe.source.validation.types.InteralStorageTypesValidator;
import poussecafe.spring.jpa.storage.SpringJpaStorage;
import poussecafe.spring.jpa.storage.source.JpaTypesValidator;
import poussecafe.spring.mongo.storage.SpringMongoDbStorage;
import poussecafe.spring.mongo.storage.source.MongoTypesValidator;
import poussecafe.storage.internal.InternalStorage;

import static java.util.Arrays.asList;

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

        var resolver = new ClassLoaderClassResolver();
        var modelBuilder = new ValidationModelBuilder(resolver);
        for(String pathName : project.getCompileSourceRoots()) {
            Path path = Path.of(pathName);
            try {
                modelBuilder.includeTree(path);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to include " + path, e);
            }
        }

        var validatorBuilder = new Validator.Builder()
                .model(modelBuilder.build());
        if(basePackages.length > 0) {
            var reflections = new ReflectionsWrapper(asList(basePackages));
            var classPathExplorer = Optional.of(new ReflectionsClassPathExplorer.Builder()
                    .reflections(reflections)
                    .resolver(resolver)
                    .build());
            validatorBuilder.classPathExplorer(classPathExplorer.orElseThrow());
        }

        for(String storageAdapterName : storageAdapters) {
            if(InternalStorage.NAME.equals(storageAdapterName)) {
                validatorBuilder.storageTypesValidator(new InteralStorageTypesValidator());
            } else if(SpringMongoDbStorage.NAME.equals(storageAdapterName)) {
                validatorBuilder.storageTypesValidator(new MongoTypesValidator());
            } else if(SpringJpaStorage.NAME.equals(storageAdapterName)) {
                validatorBuilder.storageTypesValidator(new JpaTypesValidator());
            } else {
                throw new MojoExecutionException("Unsupported storage " + storageAdapterName);
            }
        }

        var validator = validatorBuilder.build();
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
        var path = message.location().source().id();
        var line = message.location().line();
        return path + " at line " + line + ": ";
    }

    @Inject
    private ClassPathConfigurator classPathConfigurator;

    private boolean buildShouldFail(ValidationResult result) {
        return result.hasError() || (failOnWarn && result.hasWarning());
    }

    /**
     * If true, makes the build fail with warnings. False by default.
     *
     * @since 0.19
     */
    @Parameter(defaultValue = "false")
    private boolean failOnWarn;

    /**
     * Base packages used for classpath exploration. No base package implies no classpath exploration (default behavior).
     *
     * @since 0.19
     */
    @Parameter(defaultValue = "", property = "basePackages")
    private String[] basePackages;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor descriptor;

    /**
     * List of storage types validators to use. Storage name is used to select them. By default, only internal storage
     * classes are generated. Currently, supported storage names are: "Internal", "SpringMongo", "SpringJpa".
     *
     * @since 0.21
     */
    @Parameter(defaultValue = InternalStorage.NAME, property = "storageAdapters", required = true)
    private String[] storageAdapters;
}
