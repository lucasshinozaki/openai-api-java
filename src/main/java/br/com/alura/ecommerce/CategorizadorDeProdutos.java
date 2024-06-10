package br.com.alura.ecommerce;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;

import java.time.Duration;
import java.util.Arrays;

public class CategorizadorDeProdutos {
    public static void main(String[] args) {
        var user = "Escova de dentes";
        var system = """
            Você é um categorizador de produtos e deve responder apenas o nome da categoria do produto informado
            Escolha uma categoria dentro a lista abaixo:
            1. Higiene pessoal
            2. Eletronicos
            3. Esporte
            4. Outros
            
            ### exemplos de uso:
            
            Pergunta: Bola de futebol
            Resposta: Esportes    
        """;

        var chave = System.getenv("OPENAI_API_KEY");
        var service =  new OpenAiService(chave, Duration.ofSeconds(30));

        var completionRequest = ChatCompletionRequest
                .builder()
                .model("gpt-3.5-turbo-16k")
                .messages(Arrays.asList(
                        new ChatMessage(ChatMessageRole.USER.value(), user),
                        new ChatMessage(ChatMessageRole.SYSTEM.value(), system)
                ))
                .n(5) // número de respostas
                .build();
        service
                .createChatCompletion(completionRequest)
                .getChoices()
                .forEach(c -> {
                    System.out.println(c.getMessage().getContent());
                    System.out.println("\n---------------\n");
                });
    }
}
