package com.example.fintrack;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.config.location=classpath:/application-test.yml"
})
class FinTrackApplicationTests {

    @Test
    void contextLoads() {
    }

}
