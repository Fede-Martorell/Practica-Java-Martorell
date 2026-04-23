import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    // Palabra clave para autenticación (debe coincidir con el servidor)
    private static final String PALABRA_CLAVE = "SOCKET2026";

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int puerto = 5000;

        try {
            // Iniciamos conexión con el servidor
            Socket socket = new Socket(host, puerto);
            System.out.println("Conectado al servidor en " + host + ":" + puerto);

            // Puentes de comunicación
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            Scanner teclado = new Scanner(System.in);

            // Hilo para recibir mensajes del servidor (asincrono)
            Thread receptor = new Thread(() -> {
                try {
                    while (true) {
                        String mensaje = in.readUTF();
                        System.out.println("\n>>> " + mensaje);

                        // Si el mensaje indica desconexión, salimos
                        if (mensaje.contains("Desconectando") || mensaje.contains("Hasta luego")) {
                            break;
                        }
                    }
                } catch (EOFException e) {
                    System.out.println("\n[SERVIDOR] Conexión cerrada.");
                } catch (IOException e) {
                    // Socket cerrado
                }
            });
            receptor.start();

            // === FASE DE AUTENTICACIÓN ===
            System.out.println("\n--- AUTENTICACIÓN ---");

            // Enviamos la palabra clave
            System.out.println("Enviando palabra clave...");
            out.writeUTF(PALABRA_CLAVE);

            // Esperamos un pequeño delay para recibir confirmación
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {}

            // === FASE DE COMUNICACIÓN ===
            // Los comandos disponibles y nuestro nombre asignado nos los enviará el servidor.

            boolean conectado = true;
            while (conectado) {
                System.out.print("Tú: ");
                String mensaje = teclado.nextLine();

                // Enviamos el mensaje al servidor
                out.writeUTF(mensaje);

                // Comandos locales que terminan la conexión
                if (mensaje.equalsIgnoreCase("/salir") || mensaje.equalsIgnoreCase("salida")) {
                    conectado = false;
                }
            }

            // Cerramos recursos
            socket.close();
            receptor.join();
            System.out.println("Conexión finalizada.");

        } catch (IOException e) {
            System.out.println("Error al conectar. ¿Está el servidor encendido?");
            System.out.println("Detalle: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
