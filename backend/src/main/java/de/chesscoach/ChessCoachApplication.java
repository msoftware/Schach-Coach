package de.chesscoach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ChessCoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChessCoachApplication.class, args);
    }
}
