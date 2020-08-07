package <modelPackage>;

import poussecafe.discovery.Aggregate;
import poussecafe.discovery.DefaultModule;
import poussecafe.domain.AggregateRoot;
import poussecafe.domain.EntityAttributes;
<if(demoAttribute)>import poussecafe.attribute.Attribute;<endif>

@Aggregate(
    factory = <name>Factory.class,
    repository = <name>Repository.class,
    module = DefaultModule.class // TODO attach this aggregate to another module
)
public class <name> extends AggregateRoot\<<name>Id, <name>.Attributes> {

    public static interface Attributes extends EntityAttributes\<<name>Id> {

<if(demoAttribute)>
        Attribute\<String> name();
<endif>
    }
}
