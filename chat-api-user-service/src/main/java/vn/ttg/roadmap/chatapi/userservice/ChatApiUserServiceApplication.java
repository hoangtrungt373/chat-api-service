package vn.ttg.roadmap.chatapi.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ComponentScan(basePackages = {
    "vn.ttg.roadmap.chatapi.common",
    "vn.ttg.roadmap.chatapi.userservice"
})
@EntityScan(basePackages = {
    "vn.ttg.roadmap.chatapi.common.entity",
    "vn.ttg.roadmap.chatapi.userservice.entity"
})
public class ChatApiUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatApiUserServiceApplication.class, args);
    }
}
