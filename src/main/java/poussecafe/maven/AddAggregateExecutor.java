package poussecafe.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import poussecafe.exception.PousseCafeException;
import poussecafe.source.generation.CoreCodeGenerator;
import poussecafe.source.generation.StorageAdaptersCodeGenerator;
import poussecafe.source.generation.internal.InternalStorageAdaptersCodeGenerator;
import poussecafe.source.model.Aggregate;
import poussecafe.spring.jpa.storage.SpringJpaStorage;
import poussecafe.spring.jpa.storage.codegeneration.JpaStorageAdaptersCodeGenerator;
import poussecafe.spring.mongo.storage.SpringMongoDbStorage;
import poussecafe.spring.mongo.storage.codegeneration.MongoStorageAdaptersCodeGenerator;
import poussecafe.storage.internal.InternalStorage;

public class AddAggregateExecutor implements MojoExecutor {

    public static class Builder {

        private AddAggregateExecutor executor = new AddAggregateExecutor();

        public Builder sourceDirectory(File sourceDirectory) {
            this.sourceDirectory = sourceDirectory;
            return this;
        }

        private File sourceDirectory;

        public Builder packageName(String packageName) {
            executor.packageName = packageName;
            executor.aggregateDirectory = aggregatePackageDirectory(packageName);
            return this;
        }

        private File aggregatePackageDirectory(String packageName) {
            String[] packageSubDirectories = packageName.split("\\.");
            File aggregatePackageDirectory = new File(sourceDirectory.getAbsolutePath());
            for(int i = 0; i < packageSubDirectories.length; ++i) {
                aggregatePackageDirectory = new File(aggregatePackageDirectory, packageSubDirectories[i]);
            }
            return aggregatePackageDirectory;
        }

        public Builder name(String name) {
            executor.name = name;
            return this;
        }

        public Builder storageAdapters(Set<String> storageAdapters) {
            executor.storageAdapters = storageAdapters;
            return this;
        }

        public Builder missingAdaptersOnly(boolean missingAdaptersOnly) {
            executor.missingAdaptersOnly = missingAdaptersOnly;
            return this;
        }

        public AddAggregateExecutor build() {
            Objects.requireNonNull(sourceDirectory);
            Objects.requireNonNull(executor.aggregateDirectory);
            Objects.requireNonNull(executor.name);

            executor.sourceDirectory = sourceDirectory.toPath();
            executor.aggregate = new Aggregate.Builder()
                    .packageName(executor.packageName)
                    .name(executor.name)
                    .build();

            return executor;
        }
    }

    private AddAggregateExecutor() {

    }

    private String packageName;

    private File aggregateDirectory;

    private String name;

    private Set<String> storageAdapters;

    private boolean missingAdaptersOnly;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            checkDoesNotExist();
            writeModelFiles();
            writeStorageAdaptersFiles();
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void checkDoesNotExist() {
        if(!missingAdaptersOnly && aggregateDirectory.exists()) {
            throw new PousseCafeException("Aggregate directory " + aggregateDirectory.getAbsolutePath() + " already exists");
        }
    }

    private void writeModelFiles() {
        if(!missingAdaptersOnly) {
            var coreCodeGenerator = new CoreCodeGenerator.Builder()
                    .sourceDirectory(sourceDirectory)
                    .build();
            coreCodeGenerator.generate(aggregate);
        }
    }

    private Path sourceDirectory;

    private Aggregate aggregate;

    private void writeStorageAdaptersFiles() {
        Map<String, StorageAdaptersCodeGenerator> availableGenerators = availableGenerators();
        for(Entry<String, StorageAdaptersCodeGenerator> entry : availableGenerators.entrySet()) {
            if(storageAdapters.contains(entry.getKey())) {
                StorageAdaptersCodeGenerator generator = entry.getValue();
                generator.generate(aggregate);
            }
        }
    }

    private Map<String, StorageAdaptersCodeGenerator> availableGenerators() {
        Map<String, StorageAdaptersCodeGenerator> availableGenerators = new HashMap<>();
        availableGenerators.put(InternalStorage.NAME, new InternalStorageAdaptersCodeGenerator.Builder()
                .sourceDirectory(sourceDirectory)
                .build());
        availableGenerators.put(SpringMongoDbStorage.NAME, new MongoStorageAdaptersCodeGenerator.Builder()
                .sourceDirectory(sourceDirectory)
                .build());
        availableGenerators.put(SpringJpaStorage.NAME, new JpaStorageAdaptersCodeGenerator.Builder()
                .sourceDirectory(sourceDirectory)
                .build());
        return availableGenerators;
    }
}
