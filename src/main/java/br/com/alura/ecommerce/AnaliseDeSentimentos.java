package br.com.alura.ecommerce;

import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AnaliseDeSentimentos {
    private static final Logger logger = Logger.getLogger(AnaliseDeSentimentos.class.getName());
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String PROMPT_SISTEMA = """
            Você é um analisador de sentimentos de avaliação de produtos.
            Escreva um parágrafo com até 50 palavras resumindo as avaliações e depois atribua qual o sentimento geral para o produto
            Identifique também 3 pontos fortes e 3 pontos fracos identificados a partir das avaliações
            
            #### Formato de saída
            Nome do produto:
            Resumo das avaliações: [resuma até 50 palavras]
            Sentimentos geral: [deve ser: POSITIVO, NEUTRO ou NEGATIVO]
            Pontos fortes: [3 bullets points]
            Pontos fracos: [3 bullets points]
            """;
    private static final Path DIRETORIO_AVALIACOES = Path.of("src/main/resources/avaliacoes");
    private static final Path DIRETORIO_ANALISES = Path.of("src/main/resources/analises");
    private static final int MAX_TENTATIVAS = 5;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    public static void main(String[] args) {
        try {
            var service = new OpenAiService(OPENAI_API_KEY, TIMEOUT);
            var arquivosDeAvaliacoes = carregarArquivosDeAvaliacoes();

            for (Path arquivo : arquivosDeAvaliacoes) {
                processarArquivo(arquivo, service);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ocorreu um erro ao fazer análise de sentimento", e);
        }
    }

    private static List<Path> carregarArquivosDeAvaliacoes() throws IOException {
        return Files.walk(DIRETORIO_AVALIACOES, 1)
                .filter(path -> path.toString().endsWith(".txt"))
                .collect(Collectors.toList());
    }

    private static void processarArquivo(Path arquivo, OpenAiService service) {
        try {
            var promptUsuario = carregarArquivo(arquivo);

            var request = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(List.of(
                            new ChatMessage(ChatMessageRole.SYSTEM.value(), PROMPT_SISTEMA),
                            new ChatMessage(ChatMessageRole.USER.value(), promptUsuario)))
                    .build();

            var tentativas = 0;
            while (tentativas < MAX_TENTATIVAS) {
                try {
                    var resposta = service.createChatCompletion(request)
                            .getChoices().get(0).getMessage().getContent();
                    salvarAnalise(arquivo.getFileName().toString().replace(".txt", ""), resposta);
                    return;  // Sucesso, sair do loop
                } catch (OpenAiHttpException ex) {
                    handleOpenAiHttpException(ex);
                }
                tentativas++;
                Thread.sleep(5000);  // 5 segundos entre tentativas
            }
            throw new RuntimeException("API fora do ar. Tentativas finalizadas sem sucesso");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar arquivo " + arquivo, e);
        }
    }

    private static String carregarArquivo(Path arquivo) {
        try {
            return Files.readString(arquivo);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar arquivo " + arquivo, e);
        }
    }

    private static void salvarAnalise(String nomeArquivo, String analise) {
        try {
            var path = DIRETORIO_ANALISES.resolve("analise-sentimentos-" + nomeArquivo + ".txt");
            Files.writeString(path, analise, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar o arquivo " + nomeArquivo, e);
        }
    }

    private static void handleOpenAiHttpException(OpenAiHttpException ex) {
        var errorCode = ex.statusCode;
        if (errorCode == 401) {
            throw new RuntimeException("Erro com a chave da API", ex);
        } else if (errorCode == 500 || errorCode == 503) {
            logger.log(Level.WARNING, "API fora do ar. Nova tentativa em instantes");
        } else {
            throw new RuntimeException("Erro na API OpenAI: " + errorCode, ex);
        }

    }
}