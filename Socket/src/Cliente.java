import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        String host = "127.0.0.1"; // Localhost (Minuto 12:20 del video)
        int puerto = 5000;

        try {
            // Iniciamos conexión con el servidor
            Socket socket = new Socket(host, puerto);
            System.out.println("Conectado al servidor en " + host + ":" + puerto);
            System.out.println("Escribe un mensaje, usa RESOLVE \"ecuacion\", o 'salida' para terminar.");

            // Puentes de comunicación
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            Scanner teclado = new Scanner(System.in);

            boolean conectado = true;

            while (conectado) {
                System.out.print("Tú: ");
                String mensaje = teclado.nextLine();

                // Enviamos el mensaje al servidor (Minuto 14:15)
                out.writeUTF(mensaje);

                // Esperamos la respuesta del servidor (Efecto Ping-Pong, Minuto 19:00)
                String respuesta = in.readUTF();
                System.out.println(respuesta);

                if (mensaje.equalsIgnoreCase("salida")) {
                    conectado = false;
                }
            }

            socket.close();
            System.out.println("Conexión finalizada.");

        } catch (IOException e) {
            System.out.println("Error al conectar. ¿Está el servidor encendido?");
        }
    }
}