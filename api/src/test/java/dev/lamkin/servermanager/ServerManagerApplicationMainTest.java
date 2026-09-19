package dev.lamkin.servermanager;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class ServerManagerApplicationMainTest {

    @Test
    void mainDelegatesToSpringApplicationRun() {
        String[] args = { "--foo=bar" };

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            ServerManagerApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(ServerManagerApplication.class, args));
        }
    }
}
