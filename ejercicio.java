import java.util.Random;

public class ejercicio {
    public static void main(String[] args) {
        Random random = new Random();
        int cantidadNumeros = 500;
        int sumaTotal = 0;
        int min = 10;
        int max = 1000;

        // Genera los 500 numeros y los suma
        for (int i = 0; i < cantidadNumeros; i++) {
            // Genera un numero aleatorio entre 10 y 1000 (ambos inclusive)
            int numero = random.nextInt((max - min) + 1) + min;
            sumaTotal += numero;
        }

        // Calcula el promedio
        double promedio = (double) sumaTotal / cantidadNumeros;

        // Muestra los resultados
   
        System.out.println("Se generaron " + cantidadNumeros + " numeros aleatorios entre " + min + " y " + max + ".");
        System.out.println("La suma total es: " + sumaTotal);
        System.out.println("El promedio es: " + promedio);
    }
}