import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class ConsumoLavadoraWeb {
    private static final String[] MESES = {
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    public static void main(String[] args) throws IOException {
        int porta = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer servidor = HttpServer.create(new InetSocketAddress("0.0.0.0", porta), 0);

        servidor.createContext("/", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                responderHtml(exchange, 200, paginaFormulario(null));
            } else {
                responderHtml(exchange, 405, paginaErro("Método não permitido."));
            }
        });

        servidor.createContext("/calcular", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                responderHtml(exchange, 405, paginaErro("Método não permitido."));
                return;
            }

            try {
                Map<String, String> dados = lerFormulario(exchange);
                String nome = dados.getOrDefault("nome", "").trim();

                if (nome.isEmpty()) {
                    responderHtml(exchange, 400, paginaFormulario("Informe o nome do aluno."));
                    return;
                }

                double[] consumos = new double[12];
                for (int i = 0; i < 12; i++) {
                    String valorRecebido = dados.getOrDefault("mes" + i, "").trim().replace(',', '.');
                    if (valorRecebido.isEmpty()) {
                        throw new IllegalArgumentException("Preencha o consumo de todos os 12 meses.");
                    }
                    consumos[i] = Double.parseDouble(valorRecebido);
                    if (consumos[i] < 0) {
                        throw new IllegalArgumentException("Os valores de consumo não podem ser negativos.");
                    }
                }

                responderHtml(exchange, 200, paginaResultado(nome, consumos));
            } catch (NumberFormatException e) {
                responderHtml(exchange, 400, paginaFormulario("Digite somente valores numéricos válidos para o consumo."));
            } catch (IllegalArgumentException e) {
                responderHtml(exchange, 400, paginaFormulario(e.getMessage()));
            }
        });

        servidor.setExecutor(null);
        servidor.start();
        System.out.println("Aplicação iniciada em http://localhost:" + porta);
    }

    private static Map<String, String> lerFormulario(HttpExchange exchange) throws IOException {
        String corpo = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> dados = new LinkedHashMap<>();

        if (!corpo.isBlank()) {
            for (String item : corpo.split("&")) {
                String[] partes = item.split("=", 2);
                String chave = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
                String valor = partes.length > 1
                        ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8)
                        : "";
                dados.put(chave, valor);
            }
        }
        return dados;
    }

    private static String paginaResultado(String nome, double[] consumos) {
        int indiceMaior = 0;
        int indiceMenor = 0;
        double total = 0;

        for (int i = 0; i < consumos.length; i++) {
            total += consumos[i];
            if (consumos[i] > consumos[indiceMaior]) {
                indiceMaior = i;
            }
            if (consumos[i] < consumos[indiceMenor]) {
                indiceMenor = i;
            }
        }

        DecimalFormat formato = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.of("pt", "BR")));

        StringBuilder linhas = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            String classe = i == indiceMaior ? "maior" : (i == indiceMenor ? "menor" : "");
            linhas.append("<tr class='").append(classe).append("'>")
                    .append("<td>").append(MESES[i]).append("</td>")
                    .append("<td>").append(formato.format(consumos[i])).append(" kWh</td>")
                    .append("</tr>");
        }

        return estruturaPagina("Resultado - Consumo da Lavadora", """
                <main class="container">
                    <section class="card">
                        <div class="topo">
                            <span class="tag">Programação em Java para Web</span>
                            <h1>Resultado do consumo anual</h1>
                            <p>Resumo calculado a partir dos 12 valores mensais informados.</p>
                        </div>

                        <div class="resultado-grid">
                            <article class="resultado">
                                <span>Nome do aluno</span>
                                <strong>%s</strong>
                            </article>
                            <article class="resultado">
                                <span>Valor total gasto no ano</span>
                                <strong>%s kWh</strong>
                            </article>
                            <article class="resultado destaque-maior">
                                <span>Mês com maior consumo</span>
                                <strong>%s</strong>
                                <small>%s kWh</small>
                            </article>
                            <article class="resultado destaque-menor">
                                <span>Mês com menor consumo</span>
                                <strong>%s</strong>
                                <small>%s kWh</small>
                            </article>
                        </div>

                        <h2>Comparação mensal de consumo</h2>
                        <div class="tabela-wrap">
                            <table>
                                <thead><tr><th>Mês</th><th>Consumo</th></tr></thead>
                                <tbody>%s</tbody>
                            </table>
                        </div>

                        <div class="legenda">
                            <span><i class="bolinha maior-bolinha"></i> Maior consumo</span>
                            <span><i class="bolinha menor-bolinha"></i> Menor consumo</span>
                        </div>

                        <a class="botao secundario" href="/">Calcular novamente</a>
                        <footer>Desenvolvido para fins acadêmicos - João Carlos de Souza Espinato</footer>
                    </section>
                </main>
                """.formatted(
                escaparHtml(nome),
                formato.format(total),
                MESES[indiceMaior], formato.format(consumos[indiceMaior]),
                MESES[indiceMenor], formato.format(consumos[indiceMenor]),
                linhas
        ));
    }

    private static String paginaFormulario(String mensagemErro) {
        StringBuilder campos = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            campos.append("<label class='campo'>")
                    .append("<span>").append(MESES[i]).append(" (kWh)</span>")
                    .append("<input type='number' name='mes").append(i)
                    .append("' min='0' step='0.01' required placeholder='0,00'>")
                    .append("</label>");
        }

        String erro = mensagemErro == null ? "" : "<div class='erro'>" + escaparHtml(mensagemErro) + "</div>";

        return estruturaPagina("Consumo Anual da Lavadora", """
                <main class="container">
                    <section class="card">
                        <div class="topo">
                            <span class="tag">Programação em Java para Web</span>
                            <h1>Consumo anual de uma lavadora de roupas</h1>
                            <p>Informe o nome do aluno e o consumo da lavadora em cada mês do ano.</p>
                        </div>
                        %s
                        <form method="post" action="/calcular">
                            <label class="campo campo-nome">
                                <span>Nome do aluno</span>
                                <input type="text" name="nome" required value="João Carlos de Souza Espinato">
                            </label>
                            <div class="meses-grid">%s</div>
                            <button class="botao" type="submit">Calcular consumo anual</button>
                        </form>
                        <footer>Desenvolvido para fins acadêmicos - João Carlos de Souza Espinato</footer>
                    </section>
                </main>
                """.formatted(erro, campos));
    }

    private static String paginaErro(String mensagem) {
        return estruturaPagina("Erro", "<main class='container'><section class='card'><h1>Erro</h1><p>"
                + escaparHtml(mensagem) + "</p><a class='botao secundario' href='/'>Voltar</a></section></main>");
    }

    private static String estruturaPagina(String titulo, String conteudo) {
        String pagina = """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>__TITLE__</title>
                    <style>
                        *{box-sizing:border-box} body{margin:0;font-family:Arial,Helvetica,sans-serif;background:#eef2f7;color:#172033}
                        .container{max-width:1050px;margin:38px auto;padding:0 18px}.card{background:#fff;border-radius:18px;box-shadow:0 10px 30px rgba(0,0,0,.08);padding:34px}
                        .topo{text-align:center;margin-bottom:28px}.tag{display:inline-block;background:#e7efff;color:#1b4b9b;padding:7px 12px;border-radius:999px;font-size:13px;font-weight:700}
                        h1{margin:14px 0 8px;font-size:30px}h2{margin-top:30px;font-size:21px}.topo p{margin:0;color:#5c6678}.erro{padding:12px 14px;margin-bottom:18px;background:#fff0f0;border:1px solid #efb0b0;border-radius:10px;color:#8b1d1d}
                        form{display:block}.campo{display:flex;flex-direction:column;gap:7px}.campo span{font-size:14px;font-weight:700;color:#3b465a}.campo input{width:100%;padding:12px 13px;border:1px solid #cdd5df;border-radius:10px;font-size:15px;outline:none}.campo input:focus{border-color:#4779d7;box-shadow:0 0 0 3px rgba(71,121,215,.12)}
                        .campo-nome{margin-bottom:20px}.meses-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:14px}.botao{display:inline-block;border:0;margin-top:24px;background:#255db8;color:#fff;padding:13px 20px;border-radius:10px;font-size:15px;font-weight:700;text-decoration:none;cursor:pointer}.botao:hover{filter:brightness(.95)}.secundario{background:#465166}
                        .resultado-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:14px}.resultado{border:1px solid #d9e0ea;border-radius:13px;padding:18px;background:#f8fafc}.resultado span{display:block;color:#667085;font-size:13px;margin-bottom:8px}.resultado strong{font-size:20px;display:block}.resultado small{display:block;margin-top:6px;color:#667085}.destaque-maior{border-left:5px solid #b64545}.destaque-menor{border-left:5px solid #2c7f5a}
                        .tabela-wrap{overflow-x:auto}table{width:100%;border-collapse:collapse;margin-top:10px}th,td{text-align:left;padding:11px 13px;border-bottom:1px solid #e6e9ee}th{background:#f2f5f9}.maior{background:#fff3f3}.menor{background:#effaf4}.legenda{display:flex;gap:20px;margin:14px 0;color:#5f6979;font-size:13px}.bolinha{display:inline-block;width:10px;height:10px;border-radius:50%;margin-right:6px}.maior-bolinha{background:#b64545}.menor-bolinha{background:#2c7f5a}
                        footer{margin-top:28px;padding-top:18px;border-top:1px solid #e7eaf0;text-align:center;color:#7a8494;font-size:13px}
                        @media(max-width:760px){.meses-grid,.resultado-grid{grid-template-columns:1fr}.card{padding:22px}h1{font-size:24px}}
                    </style>
                </head>
                <body>__CONTENT__</body>
                </html>
                """;
        return pagina.replace("__TITLE__", escaparHtml(titulo)).replace("__CONTENT__", conteudo);
    }

    private static String escaparHtml(String texto) {
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void responderHtml(HttpExchange exchange, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
