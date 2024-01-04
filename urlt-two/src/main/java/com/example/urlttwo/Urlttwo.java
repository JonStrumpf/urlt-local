package com.example.urlttwo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

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
    public RedirectView trackUrl() throws IOException {


        // send ping to pendo that URL was hit
        String protocal = "HTTPS";
        String domain = "app.pendo.io";
        String path = "/data/track";
        String sharedsecret = "69571379f12304d798affa1cba82f2453d71c14e346aad143cbd4ee850724bd120a24665b426d6f41bfe95a7e203daf5";

        String url = "https://postman-rest-api-learner.glitch.me/info";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> map = new HashMap<>();
        map.put("name","Jon");

        HttpEntity<String> request =
                new HttpEntity<String>(map.toString(), headers);

        String resp = restTemplate.postForObject(url, request, String.class);
        System.out.println(resp);




//        URL pendoUrl = new URL(protocal + "://" + domain + path);
//
//        HttpURLConnection con = (HttpURLConnection) pendoUrl.openConnection();
//        con.setRequestMethod("POST");
//
//        Map<String, String> parameters = new HashMap<>();
//        parameters.put("event", "test event");
//        parameters.put("visitorId", "3294828429");
//        parameters.put("type", "track");
//        parameters.put("accountId", "testaccount-demo"); // make sure -demo is always on this so the data is excluded
//
//        con.setDoOutput(true);
//        DataOutputStream out = (DataOutputStream) con.getOutputStream();
//        out.writeBytes(getParamsString(parameters));
//        out.flush();
//        out.close();
//
//        con.setRequestProperty("Content-Type", "application/json");
//        con.setRequestProperty("x-pendo-integration-key", sharedsecret);
//
//        con.setConnectTimeout(5000);
//        con.setReadTimeout(5000);
//
//        int status = con.getResponseCode();
//
//        BufferedReader in = new BufferedReader(
//                new InputStreamReader(con.getInputStream()));
//        String inputLine;
//        StringBuffer content = new StringBuffer();
//        while ((inputLine = in.readLine()) != null) {
//            content.append(inputLine);
//        }
//        in.close();
//
//        con.disconnect();



        // redirct the user to benefitsolver
        return new RedirectView("https://team6.benefitsolver.com");
    }

    public static String getParamsString(Map<String, String> params)
            throws UnsupportedEncodingException, UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0
                ? resultString.substring(0, resultString.length() - 1)
                : resultString;
    }

}