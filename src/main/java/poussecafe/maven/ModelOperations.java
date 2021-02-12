package poussecafe.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import poussecafe.source.analysis.ClassLoaderClassResolver;
import poussecafe.source.analysis.SourceModelBuilder;
import poussecafe.source.emil.EmilExporter;
import poussecafe.source.emil.parser.TreeAnalyzer;
import poussecafe.source.emil.parser.TreeParser;
import poussecafe.source.generation.CoreCodeGenerator;
import poussecafe.source.generation.StorageAdaptersCodeGenerator;
import poussecafe.source.generation.internal.InternalStorageAdaptersCodeGenerator;
import poussecafe.source.model.Aggregate;
import poussecafe.source.model.ProcessModel;
import poussecafe.source.model.SourceModel;
import poussecafe.spring.jpa.storage.SpringJpaStorage;
import poussecafe.spring.jpa.storage.source.JpaStorageAdaptersCodeGenerator;
import poussecafe.spring.mongo.storage.SpringMongoDbStorage;
import poussecafe.spring.mongo.storage.source.MongoStorageAdaptersCodeGenerator;
import poussecafe.storage.internal.InternalStorage;

public class ModelOperations {

    public SourceModel buildModelFromSource(MavenProject project) throws MojoExecutionException {
        var builder = new SourceModelBuilder();
        for(String pathName : project.getCompileSourceRoots()) {
            Path path = Path.of(pathName);
            try {
                builder.includeTree(path);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to include " + path, e);
            }
        }
        return builder.build();
    }

    public void exportProcess(SourceModel model, Optional<String> processName, File outputFile) throws MojoExecutionException {
        EmilExporter exporter = new EmilExporter.Builder()
                .model(model)
                .processName(processName)
                .build();
        var emil = exporter.toEmil();
        try {
            Files.writeString(outputFile.toPath(), emil);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write to file " + outputFile, e);
        }
    }

    public SourceModel buildModelFromEmil(Log log, File emilFile, String basePackage) throws MojoExecutionException {
        try(var inputStream = new FileInputStream(emilFile)) {
            var tree = TreeParser.parseInputStream(inputStream);
            if(!tree.isValid()) {
                log.error("Unable to parse " + emilFile);
                for(String error : tree.errors()) {
                    log.error(error);
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

    public void importModel(
            Optional<SourceModel> currentModel,
            SourceModel newModel,
            File sourceDirectory,
            Set<String> storageAdapters,
            Optional<File> codeFormatterProfile) {
        var generatorBuilder = new CoreCodeGenerator.Builder()
                .sourceDirectory(sourceDirectory.toPath())
                .classResolver(new ClassLoaderClassResolver());
        if(currentModel.isPresent()) {
            generatorBuilder.currentModel(currentModel.get());
        }
        if(codeFormatterProfile.isPresent()) {
            generatorBuilder.codeFormatterProfile(codeFormatterProfile.get().toPath());
        }
        var generator = generatorBuilder.build();
        generator.generate(newModel);
        writeStorageAdaptersFiles(newModel, sourceDirectory, storageAdapters, codeFormatterProfile);
    }

    private void writeStorageAdaptersFiles(
            SourceModel newModel,
            File sourceDirectory,
            Set<String> storageAdapters,
            Optional<File> codeFormatterProfile) {
        Map<String, StorageAdaptersCodeGenerator> availableGenerators = availableGenerators(sourceDirectory, codeFormatterProfile);
        for(Aggregate aggregate : newModel.aggregates()) {
            for(Entry<String, StorageAdaptersCodeGenerator> entry : availableGenerators.entrySet()) {
                if(storageAdapters.contains(entry.getKey())) {
                    StorageAdaptersCodeGenerator generator = entry.getValue();
                    generator.generate(aggregate);
                }
            }
        }
    }

    private Map<String, StorageAdaptersCodeGenerator> availableGenerators(File sourceDirectory,
            Optional<File> codeFormatterProfile) {
        Map<String, StorageAdaptersCodeGenerator> availableGenerators = new HashMap<>();

        var internalGeneratorBuilder = new InternalStorageAdaptersCodeGenerator.Builder()
                .sourceDirectory(sourceDirectory.toPath());
        var mongoGeneratorBuilder = new MongoStorageAdaptersCodeGenerator.Builder()
                .sourceDirectory(sourceDirectory.toPath());
        var jpaGeneratorBuilder = new JpaStorageAdaptersCodeGenerator.Builder()
                .sourceDirectory(sourceDirectory.toPath());
        if(codeFormatterProfile.isPresent()) {
            internalGeneratorBuilder.codeFormatterProfile(codeFormatterProfile.get().toPath());
            mongoGeneratorBuilder.codeFormatterProfile(codeFormatterProfile.get().toPath());
            jpaGeneratorBuilder.codeFormatterProfile(codeFormatterProfile.get().toPath());
        }

        availableGenerators.put(InternalStorage.NAME, internalGeneratorBuilder.build());
        availableGenerators.put(SpringMongoDbStorage.NAME, mongoGeneratorBuilder.build());
        availableGenerators.put(SpringJpaStorage.NAME, jpaGeneratorBuilder.build());

        return availableGenerators;
    }

    public void listProcesses(Log log, SourceModel model) {
        log.info("Found " + model.processes().size() + " processes:");
        for(ProcessModel process : model.processes()) {
            log.info("- " + process.simpleName());
        }
    }
}
