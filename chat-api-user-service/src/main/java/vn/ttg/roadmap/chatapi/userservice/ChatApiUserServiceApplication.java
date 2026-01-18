package vn.ttg.roadmap.chatapi.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ChatApiUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatApiUserServiceApplication.class, args);
    }
}
