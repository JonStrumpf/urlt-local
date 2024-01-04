package com.example.urlttwo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@SpringBootApplication
@RestController
public class Urlttwo {

    public static void main(String[] args) {
        SpringApplication.run(Urlttwo.class, args);
    }

    @GetMapping("/hello")
    public String sayHello(@RequestParam(value = "debug", defaultValue = "World") String name) {
        return String.format("Hello %s!", name);
    }

    @GetMapping("/track")
    public RedirectView trackUrl() {


        // send ping to pendo that URL was hit
        String protocal = "HTTPS";
        String domain = "app.pendo.io";
        String path = "/data/track";
        String sharedsecret = "no2";


        // redirct the user to benefitsolver
        return new RedirectView("https://team6.benefitsolver.com");
    }

}