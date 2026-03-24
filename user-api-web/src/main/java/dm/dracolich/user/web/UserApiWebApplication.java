package dm.dracolich.user.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"dm.dracolich.user.web", "dm.dracolich.forge"})
@EnableReactiveMongoRepositories(basePackages = {"dm.dracolich.user.web.repository"})
public class UserApiWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApiWebApplication.class, args);
    }
}
