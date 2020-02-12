package <adaptersPackage>;

<if(demoAttribute)>import java.util.List;<endif>
import org.springframework.beans.factory.annotation.Autowired;
import poussecafe.discovery.DataAccessImplementation;
import poussecafe.spring.jpa.storage.JpaDataAccess;
import poussecafe.spring.jpa.storage.SpringJpaStorage;

import <modelPackage>.<name>;
import <modelPackage>.<name>DataAccess;
import <modelPackage>.<name>Id;

@DataAccessImplementation(
    aggregateRoot = <name>.class,
    dataImplementation = <name>Attributes.class,
    storageName = SpringJpaStorage.NAME
)
public class <name>JpaDataAccess extends JpaDataAccess\<<name>Id, <name>Attributes, String> implements <name>DataAccess\<<name>Attributes> {

<if(demoAttribute)>
    @Override
    public List\<<name>Attributes> findByName(String name) {
        return repository.findByName(name);
    }
<endif>

    @Override
    protected String convertId(<name>Id key) {
        return key.stringValue();
    }

    @Override
    protected <name>DataJpaRepository jpaRepository() {
        return repository;
    }

    @Autowired
    private <name>DataJpaRepository repository;
}
