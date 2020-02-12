package <adaptersPackage>;

<if(demoAttribute)>
import java.util.List;
<endif>
import org.springframework.data.jpa.repository.JpaRepository;

public interface <name>DataJpaRepository extends JpaRepository\<<name>Attributes, String> {

<if(demoAttribute)>    List\<<name>Attributes> findByName(String name);<endif>
}
