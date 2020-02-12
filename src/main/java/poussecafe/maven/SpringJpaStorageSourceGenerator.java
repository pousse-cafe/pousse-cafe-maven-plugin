package poussecafe.maven;

import java.io.File;
import java.util.Objects;

public class SpringJpaStorageSourceGenerator implements StorageSourceGenerator {

    public static class Builder implements StorageSourceGeneratorBuilder {

        private SpringJpaStorageSourceGenerator generator = new SpringJpaStorageSourceGenerator();

        @Override
        public Builder sourceWriter(SourceWriter sourceWriter) {
            generator.sourceWriter = sourceWriter;
            return this;
        }

        @Override
        public Builder aggregateName(String aggregateName) {
            generator.aggregateName = aggregateName;
            return this;
        }

        @Override
        public Builder adaptersDirectory(File adaptersDirectory) {
            generator.adaptersDirectory = adaptersDirectory;
            return this;
        }

        @Override
        public SpringJpaStorageSourceGenerator build() {
            Objects.requireNonNull(generator.sourceWriter);
            Objects.requireNonNull(generator.aggregateName);
            Objects.requireNonNull(generator.adaptersDirectory);
            return generator;
        }
    }

    private SpringJpaStorageSourceGenerator() {

    }

    private SourceWriter sourceWriter;

    private String aggregateName;

    private File adaptersDirectory;

    @Override
    public void generate() {
        writeSpringMondoDataAccessSource();
        writeDataMongoRepositorySource();
    }

    private void writeSpringMondoDataAccessSource() {
        File outputFile = new File(adaptersDirectory, aggregateName + "JpaDataAccess.java");
        sourceWriter.writeSource(outputFile, "spring_jpa_data_access");
    }

    private void writeDataMongoRepositorySource() {
        File outputFile = new File(adaptersDirectory, aggregateName + "DataJpaRepository.java");
        sourceWriter.writeSource(outputFile, "data_jpa_repository");
    }
}
