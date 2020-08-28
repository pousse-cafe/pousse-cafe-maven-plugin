package poussecafe.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import poussecafe.source.Scanner;
import poussecafe.source.emil.EmilExporter;
import poussecafe.source.emil.parser.TreeAnalyzer;
import poussecafe.source.emil.parser.TreeParser;
import poussecafe.source.generation.CoreCodeGenerator;
import poussecafe.source.model.Model;
import poussecafe.source.model.ProcessModel;

public class ModelOperations {

    public Model buildModelFromSource(MavenProject project) throws MojoExecutionException {
        var scanner = new Scanner.Builder().build();
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

    public void importProcess(Model model, File sourceDirectory) {
        var generator = new CoreCodeGenerator.Builder()
                .sourceDirectory(sourceDirectory.toPath())
                .build();
        generator.generate(model);
    }

    public void listProcesses(Log log, Model model) {
        log.info("Found " + model.processes().size() + " processes:");
        for(ProcessModel process : model.processes()) {
            log.info("- " + process.simpleName());
        }
    }
}
