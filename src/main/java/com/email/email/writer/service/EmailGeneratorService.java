package com.email.email.writer.service;


import com.email.email.writer.model.EmailRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Objects;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder  webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {

        // build prompt
        String prompt = buildPrompt(emailRequest);
        //craft reqest
        Map<String, Object> request = Map.of("contents",new Object[] {
                Map.of("parts", new Object[]{
                        Map.of("text", prompt)
                })
        });
        //Do req and get Res
        String response = webClient.post().uri(geminiApiUrl+geminiApiKey)
                .header("Content-Type","application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        //extract and return response
        return extractResponseContent(response);

    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);

            return rootNode.path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

        }catch (Exception e){
            return "error processing request: "+ e.getMessage();
        }

    }

    // build prompt
    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email replay for the following content./ please don't generate the subject line ");
        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("use a").append(emailRequest.getTone()).append("tone");
        }
        prompt.append("\nOriginal email: \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }

}
