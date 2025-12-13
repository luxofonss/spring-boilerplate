package com.ds.ticketmaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@ComponentScan("com.ds.ticketmaster")
public class TicketmasterApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketmasterApplication.class, args);
    }

}
