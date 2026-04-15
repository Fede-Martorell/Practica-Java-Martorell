import java.io.*;
import java.net.*;

public class Servidor {
    public static void main(String[] args) {
        int puerto = 5000;

        try {
            // Creamos el servidor esperando en el puerto 5000 (Minuto 4:00 del video)
            ServerSocket serverSocket = new ServerSocket(puerto);
            System.out.println("Servidor iniciado. Esperando conexión...");

            // Se queda a la espera (bloqueado) hasta que llegue un cliente (Minuto 6:30)
            Socket socketCliente = serverSocket.accept();
            System.out.println("¡Cliente conectado!");

            // Puentes de comunicación exactos a los del video (Minuto 7:00)
            DataInputStream in = new DataInputStream(socketCliente.getInputStream());
            DataOutputStream out = new DataOutputStream(socketCliente.getOutputStream());

            boolean conectado = true;

            // Bucle del chat (Ping-Pong de mensajes)
            while (conectado) {
                try {
                    // Leemos el mensaje del cliente (Minuto 9:50)
                    String mensajeCliente = in.readUTF();
                    System.out.println("LOG (Recibido del cliente): " + mensajeCliente);

                    if (mensajeCliente.equalsIgnoreCase("salida")) {
                        out.writeUTF("Desconectando del servidor. ¡Adiós!");
                        conectado = false; // Rompemos el bucle
                    } else if (mensajeCliente.toUpperCase().startsWith("RESOLVE")) {
                        // Lógica para la ecuación matemática pedida en la consigna
                        try {
                            String ecuacion = mensajeCliente.substring(7).replaceAll("[\"\\s]", "");
                            double resultado = evaluarExpresion(ecuacion);
                            out.writeUTF("Resultado del Servidor: " + resultado);
                        } catch (Exception e) {
                            out.writeUTF("Error: No se pudo resolver la ecuación.");
                        }
                    } else {
                        // Respuesta normal de chat
                        out.writeUTF("Servidor recibió: " + mensajeCliente);
                    }
                } catch (EOFException e) {
                    // Si el cliente corta de golpe
                    conectado = false;
                }
            }

            socketCliente.close();
            System.out.println("Cliente desconectado. Servidor apagado.");

        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }

    // Método para resolver el String matemático sin librerías externas
    public static double evaluarExpresion(final String str) {
        return new Object() {
            int pos = -1, ch;
            void nextChar() { ch = (++pos < str.length()) ? str.charAt(pos) : -1; }
            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) { nextChar(); return true; }
                return false;
            }
            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Inesperado: " + (char)ch);
                return x;
            }
            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }
            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }
            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();
                double x;
                int startPos = this.pos;
                if (eat('(')) { x = parseExpression(); eat(')'); }
                else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else { throw new RuntimeException("Inesperado: " + (char)ch); }
                return x;
            }
        }.parse();
    }
}