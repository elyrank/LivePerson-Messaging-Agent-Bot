package com.liveperson.tutorial.ws;

import com.liveperson.tutorial.ws.bot.base.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author elyran
 * @since 5/30/16.
 */
@SpringBootApplication
@ComponentScan("com.liveperson.tutorial.ws")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}