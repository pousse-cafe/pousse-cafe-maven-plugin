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
import poussecafe.source.Scanner;
import poussecafe.source.emil.EmilExporter;
import poussecafe.source.emil.parser.TreeAnalyzer;
import poussecafe.source.emil.parser.TreeParser;
import poussecafe.source.generation.CoreCodeGenerator;
import poussecafe.source.generation.StorageAdaptersCodeGenerator;
import poussecafe.source.generation.internal.InternalStorageAdaptersCodeGenerator;
import poussecafe.source.model.Aggregate;
import poussecafe.source.model.Model;
import poussecafe.source.model.ProcessModel;
import poussecafe.spring.jpa.storage.SpringJpaStorage;
import poussecafe.spring.jpa.storage.codegeneration.JpaStorageAdaptersCodeGenerator;
import poussecafe.spring.mongo.storage.SpringMongoDbStorage;
import poussecafe.spring.mongo.storage.codegeneration.MongoStorageAdaptersCodeGenerator;
import poussecafe.storage.internal.InternalStorage;

public class ModelOperations {

    public Model buildModelFromSource(MavenProject project) throws MojoExecutionException {
        var scanner = new Scanner();
        for(String pathName : project.getCompileSourceRoots()) {
            Path path = Path.of(pathName);
            try {
                scanner.includeTree(path);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to include " + path, e);
            }
        }
        return scanner.model();
    }

    public void exportProcess(Model model, Optional<String> processName, File outputFile) throws MojoExecutionException {
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

    public Model buildModelFromEmil(Log log, File emilFile, String basePackage) throws MojoExecutionException {
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

    public void importProcess(
            Optional<Model> currentModel,
            Model newModel,
            File sourceDirectory,
            Set<String> storageAdapters) {
        var generatorBuilder = new CoreCodeGenerator.Builder()
                .sourceDirectory(sourceDirectory.toPath());
        if(currentModel.isPresent()) {
            generatorBuilder.currentModel(currentModel.get());
        }
        var generator = generatorBuilder.build();
        generator.generate(newModel);
        writeStorageAdaptersFiles(currentModel, newModel, sourceDirectory, storageAdapters);
    }

    private void writeStorageAdaptersFiles(
            Optional<Model> currentModel,
            Model newModel,
            File sourceDirectory,
            Set<String> storageAdapters) {
        Map<String, StorageAdaptersCodeGenerator> availableGenerators = availableGenerators(sourceDirectory);
        for(Aggregate aggregate : newModel.aggregates()) {
            if(currentModel.isEmpty()
                    || currentModel.get().aggregate(aggregate.simpleName()).isEmpty()) {
                for(Entry<String, StorageAdaptersCodeGenerator> entry : availableGenerators.entrySet()) {
                    if(storageAdapters.contains(entry.getKey())) {
                        StorageAdaptersCodeGenerator generator = entry.getValue();
                        generator.generate(aggregate);
                    }
                }
            }
        }
    }

    private Map<String, StorageAdaptersCodeGenerator> availableGenerators(File sourceDirectory) {
        Map<String, StorageAdaptersCodeGenerator> availableGenerators = new HashMap<>();
        availableGenerators.put(InternalStorage.NAME, new InternalStorageAdaptersCodeGenerator.Builder()
                .sourceDirectory(sourceDirectory.toPath())
                .build());
        availableGenerators.put(SpringMongoDbStorage.NAME, new MongoStorageAdaptersCodeGenerator.Builder()
                .sourceDirectory(sourceDirectory.toPath())
                .build());
        availableGenerators.put(SpringJpaStorage.NAME, new JpaStorageAdaptersCodeGenerator.Builder()
                .sourceDirectory(sourceDirectory.toPath())
                .build());
        return availableGenerators;
    }

    public void listProcesses(Log log, Model model) {
        log.info("Found " + model.processes().size() + " processes:");
        for(ProcessModel process : model.processes()) {
            log.info("- " + process.simpleName());
        }
    }
}
