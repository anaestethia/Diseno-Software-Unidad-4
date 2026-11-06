import java.util.ArrayList;
import java.util.List;

public class Main {
    interface Observer<T> {
        void update(T value);
    }

    static final class TemperatureSensor {
        private final List<Observer<Double>> observers = new ArrayList<>();
        private double temperature;

        void subscribe(Observer<Double> observer) {
            observers.add(observer);
        }

        void unsubscribe(Observer<Double> observer) {
            observers.remove(observer);
        }

        void setTemperature(double temperature) {
            this.temperature = temperature;
            System.out.printf("Sensor: %.1f °C%n", temperature);
            for (Observer<Double> observer : List.copyOf(observers)) {
                observer.update(temperature);
            }
        }

        double getTemperature() {
            return temperature;
        }
    }

    static final class Display implements Observer<Double> {
        private final String name;

        Display(String name) {
            this.name = name;
        }

        @Override
        public void update(Double temperature) {
            System.out.printf("%s recibe %.1f °C%n", name, temperature);
        }
    }

    public static void main(String[] args) {
        TemperatureSensor sensor = new TemperatureSensor();
        Display lobby = new Display("Pantalla del hall");
        Display cafeteria = new Display("Pantalla del comedor");
        sensor.subscribe(lobby);
        sensor.subscribe(cafeteria);
        sensor.setTemperature(22.5);
        sensor.unsubscribe(cafeteria);
        sensor.setTemperature(23.0);
        System.out.printf("Última lectura: %.1f °C%n", sensor.getTemperature());
    }
}
