package commonly.commonlybe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CommonlyBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommonlyBeApplication.class, args);
    }

}
