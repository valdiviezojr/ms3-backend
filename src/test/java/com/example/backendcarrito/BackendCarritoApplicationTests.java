package com.example.backendcarrito;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.mongodb.ConnectionString;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BackendCarritoApplicationTests {
    @Autowired MongoTemplate mongo;
    @Autowired Environment environment;

    @Test
    void contextLoads() {
        String uri = environment.getRequiredProperty("spring.data.mongodb.uri");
        assertThat(mongo.getDb().getName()).isEqualTo(new ConnectionString(uri).getDatabase());
    }

}
