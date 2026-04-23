import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Servidor {
    private static final int PUERTO = 5000;
    private static final String PALABRA_CLAVE = "SOCKET2026"; // Palabra clave para autenticación

    // Lista de clientes conectados
    private static final ConcurrentHashMap<String, ClienteHandler> clientes = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("=== SERVIDOR DE SOCKETS CON HILOS ===");
        System.out.println("Palabra clave requerida: " + PALABRA_CLAVE);
        System.out.println("Esperando conexiones en el puerto " + PUERTO + "...\n");

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            while (true) {
                // Aceptamos cada cliente en un hilo separado
                Socket socketCliente = serverSocket.accept();
                System.out.println("[SERVIDOR] Nuevo cliente conectado desde: " + socketCliente.getInetAddress());

                // Creamos un nuevo hilo para atender a este cliente
                ClienteHandler handler = new ClienteHandler(socketCliente);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }

    // Método para enviar mensaje a un cliente específico
    public static void enviarMensajePrivado(String destino, String mensaje) {
        ClienteHandler handler = clientes.get(destino);
        if (handler != null) {
            handler.enviar(mensaje);
        }
    }

    // Método para listar clientes conectados
    public static String listarClientes() {
        if (clientes.isEmpty()) {
            return "No hay clientes conectados.";
        }
        StringBuilder lista = new StringBuilder("=== CLIENTES CONECTADOS ===\n");
        for (String nombre : clientes.keySet()) {
            lista.append("  - ").append(nombre).append("\n");
        }
        return lista.toString();
    }

    // Método para enviar mensaje a todos los clientes
    public static void broadcast(String mensaje, String excluido) {
        for (Map.Entry<String, ClienteHandler> entry : clientes.entrySet()) {
            if (!entry.getKey().equals(excluido)) {
                entry.getValue().enviar(mensaje);
            }
        }
    }

    // Clase interna que maneja cada cliente en un hilo independiente
    static class ClienteHandler implements Runnable {
        private Socket socket;
        private DataInputStream in;
        private DataOutputStream out;
        private String nombreCliente;
        private boolean autenticado = false;
        private boolean conectado = true;

        public ClienteHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                System.out.println("Error al crear streams: " + e.getMessage());
            }
        }

        public void enviar(String mensaje) {
            try {
                out.writeUTF(mensaje);
            } catch (IOException e) {
                System.out.println("Error al enviar mensaje a " + nombreCliente);
            }
        }

        @Override
        public void run() {
            try {
                // === FASE DE AUTENTICACIÓN ===
                out.writeUTF("BIENVENIDO - Ingrese la palabra clave para autenticarse:");

                while (!autenticado && conectado) {
                    String respuesta = in.readUTF();

                    if (respuesta.equalsIgnoreCase(PALABRA_CLAVE)) {
                        // El usuario envía/elige su nombre
                        out.writeUTF("AUTENTICACION EXITOSA - Ingrese el nombre de usuario que desea utilizar:");
                        nombreCliente = in.readUTF().trim();

                        // Verificamos que el nombre no esté duplicado
                        String baseName = nombreCliente;
                        int intento = 1;
                        boolean modificado = false;

                        // Si el usuario existe, el servidor nombra un cliente único con el agregado de
                        // un número
                        while (clientes.containsKey(nombreCliente)) {
                            nombreCliente = baseName + "_" + intento;
                            intento++;
                            modificado = true;
                        }

                        // Registramos el cliente
                        clientes.put(nombreCliente, this);
                        autenticado = true;

                        System.out.println("[LOG] Nuevo cliente registrado con éxito como: " + nombreCliente);

                        // Enviamos menú de bienvenida dando nombre asignado y ayuda
                        out.writeUTF("--------------------------------------------------");
                        out.writeUTF("¡BIENVENIDO AL SERVIDOR!");
                        if (modificado) {
                            out.writeUTF("Aviso: El nombre '" + baseName + "' ya estaba en uso. Te asignamos: "
                                    + nombreCliente);
                        } else {
                            out.writeUTF("Nombre asignado con éxito: " + nombreCliente);
                        }
                        out.writeUTF("\nCOMANDOS DISPONIBLES:");
                        out.writeUTF("  /lista                  - Ver clientes conectados");
                        out.writeUTF("  /hora                   - Consultar la fecha y hora");
                        out.writeUTF(
                                "  /calc <ej: 2 + 2>       - Resolver expresión matemática (Suma, resta, mult, div)");
                        out.writeUTF("  *Usuario1,Usuario2 msj  - Enviar mensajes privados a uno o varios clientes");
                        out.writeUTF("  *ALL msj                - Enviar mensaje a absolutamente todos");
                        out.writeUTF("  /salir                  - Desconectarse del servidor");
                        out.writeUTF("--------------------------------------------------");

                        // Avisamos a los demás en broadccast
                        broadcast("[SISTEMA] " + nombreCliente + " se ha unido al servidor.", nombreCliente);

                    } else {
                        out.writeUTF("PALABRA CLAVE INCORRECTA - Intente nuevamente:");
                        // El servidor loquea todo lo que sucede
                        System.out.println("[LOG] Intento fallido de autenticación desde " + socket.getInetAddress());
                    }
                }

                // === FASE DE COMUNICACIÓN ===
                while (conectado && autenticado) {
                    try {
                        String mensaje = in.readUTF();
                        System.out.println("[" + nombreCliente + "]: " + mensaje);

                        // Procesamos diferentes tipos de comandos y logueamos y resolvemos cada entrada
                        if (mensaje.startsWith("/")) {
                            System.out.println("[LOG COMANDO] " + nombreCliente + " usó: " + mensaje);
                            procesarComando(mensaje);
                        } else if (mensaje.startsWith("*")) {
                            System.out.println(
                                    "[LOG MENSAJERIA] " + nombreCliente + " envió destino especial: " + mensaje);
                            procesarMensajeEspecial(mensaje);
                        } else {
                            // Si habla normal, no es un formato esperado
                            System.out.println("[LOG ADVERTENCIA] " + nombreCliente + " envió formato inválido.");
                            enviar("Error: Debes usar un comando ej: '/lista', o mandar msj con '*', ej: '*ALL Hola'");
                        }

                    } catch (EOFException e) {
                        // Cliente se desconectó abruptamente
                        conectado = false;
                    }
                }

            } catch (IOException e) {
                System.out.println("Error con cliente " + nombreCliente + ": " + e.getMessage());
            } finally {
                // Limpieza al desconectar
                desconectar();
            }
        }

        private void procesarComando(String comando) {
            String[] partes = comando.split(" ", 2); // Separamos solo la primera palabra de comando
            String cmd = partes[0].toLowerCase();

            switch (cmd) {
                case "/lista":
                    enviar(listarClientes());
                    break;

                case "/hora":
                    LocalDateTime ahora = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                    enviar("Fecha y hora actual: " + ahora.format(formatter));
                    break;

                case "/calc":
                    if (partes.length < 2) {
                        enviar("USO: /calc <expresión matemática>. Ejemplo: /calc 5 + 3");
                    } else {
                        String expresion = partes[1];
                        String resultado = resolverExpresionMatematica(expresion);
                        enviar("El resultado de [" + expresion + "] es = " + resultado);
                    }
                    break;

                case "/salir":
                    enviar("Desconectando... ¡Hasta luego!");
                    conectado = false;
                    break;

                default:
                    enviar("COMANDO NO RECONOCIDO. Intenta /lista, /hora o /calc.");
            }
        }

        // Función auxiliar del servidor para enviar a 1, 2 o a todos los clientes
        // especificados
        private void procesarMensajeEspecial(String comando) {
            // Busca el primer espacio para separar usuarios de msj (Ej: *Juan,Maria Hola
            // que hacen)
            int primerEspacio = comando.indexOf(" ");
            if (primerEspacio == -1) {
                enviar("ERROR de formato. USO: *Usuario1,Usuario2 tu mensaje. O bien: *ALL tu mensaje.");
                return;
            }

            // Los destinatarios están despues del *, y antes del primer espacio
            String usuariosObjetivoStr = comando.substring(1, primerEspacio).trim();
            String mensajeAEnviar = comando.substring(primerEspacio + 1).trim();

            if (usuariosObjetivoStr.equalsIgnoreCase("ALL")) {
                broadcast("[De " + nombreCliente + " a TODOS]: " + mensajeAEnviar, nombreCliente);
                enviar("[Enviaste a TODOS]: " + mensajeAEnviar);
            } else {
                // Separamos por coma porque nos pueden enviar "1, 2 o todos"
                String[] destinatarios = usuariosObjetivoStr.split(",");

                for (String destino : destinatarios) {
                    destino = destino.trim();
                    ClienteHandler handler = clientes.get(destino);

                    if (handler != null) {
                        handler.enviar("[Mensaje de " + nombreCliente + "]: " + mensajeAEnviar);
                        enviar("[Enviaste a " + destino + "]: " + mensajeAEnviar);
                    } else {
                        // El servidor resuelve errores: Si el 2ndo usuario no existe le manda al
                        // primero y avisa
                        enviar("ERROR: No se pudo enviar a '" + destino + "'. El usuario no existe o se desconectó.");
                        System.out.println("[LOG ERROR] " + nombreCliente + " falló al enviar a: " + destino);
                    }
                }
            }
        }

        // Resolutor de matemáticas universitario sencillo, sin librerias de scripting
        // propensas a error
        private String resolverExpresionMatematica(String expr) {
            try {
                // Limpiamos la expresion de posibles espacios " 2 + 2 " -> "2+2"
                expr = expr.trim().replace(" ", "");
                String operador = "";

                if (expr.contains("+"))
                    operador = "\\+"; // Escapamos para el regex del split
                else if (expr.contains("-"))
                    operador = "-";
                else if (expr.contains("*"))
                    operador = "\\*";
                else if (expr.contains("/"))
                    operador = "/";
                else
                    return "Operador no soportado (+, -, *, /)";

                String[] piezas = expr.split(operador);
                if (piezas.length != 2)
                    return "Formato no valido (Ejemplo: 10 / 2)";

                double n1 = Double.parseDouble(piezas[0]);
                double n2 = Double.parseDouble(piezas[1]);
                double res = 0;

                switch (operador) {
                    case "\\+":
                        res = n1 + n2;
                        break;
                    case "-":
                        res = n1 - n2;
                        break;
                    case "\\*":
                        res = n1 * n2;
                        break;
                    case "/":
                        if (n2 == 0)
                            return "No se puede dividir por infinito/cero";
                        res = n1 / n2;
                        break;
                }

                // Si el número es redondo (ej 10.0), lo mostramos sin la coma decimal por
                // prolijidad
                if (res % 1 == 0) {
                    return String.valueOf((int) res);
                }
                return String.valueOf(res);

            } catch (Exception e) {
                return "Error al calcular, verifica que sean números válidos.";
            }
        }

        private void desconectar() {
            if (nombreCliente != null) {
                clientes.remove(nombreCliente);
                System.out.println("[SERVIDOR] " + nombreCliente + " se ha desconectado.");
                broadcast("[SISTEMA] " + nombreCliente + " ha abandonado el chat.", null);
            }
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error al cerrar socket: " + e.getMessage());
            }
        }
    }
}
