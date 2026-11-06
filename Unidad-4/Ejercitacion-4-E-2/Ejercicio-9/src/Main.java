import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        try (AlertEventBus bus = new AlertEventBus()) {
            bus.subscribe(AlertType.CPU, event -> {
                throw new IllegalStateException("simulación de fallo del servicio");
            });
            bus.subscribe(AlertType.CPU, event -> System.out.println(
                    "Escalamiento CPU: severidad " + event.getSeverity()
                            + ", " + event.getDescription()));
            bus.subscribe(AlertType.MEMORY, event -> System.out.println(
                    "Servicio de memoria: " + event.getDescription()));

            bus.publish(new SystemAlert(AlertType.CPU, 5, "CPU al 90%")).join();
            bus.publish(new SystemAlert(AlertType.MEMORY, 4, "Memoria insuficiente")).join();
        }
    }
}

enum AlertType {
    CPU, MEMORY, DISK
}

interface AlertObserver {
    void onAlert(SystemAlert alert) throws Exception;
}

final class SystemAlert {
    private final AlertType type;
    private final int severity;
    private final String description;

    SystemAlert(AlertType type, int severity, String description) {
        if (type == null || severity < 1 || severity > 5
                || description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Alerta inválida");
        }
        this.type = type;
        this.severity = severity;
        this.description = description;
    }

    public AlertType getType() { return type; }
    public int getSeverity() { return severity; }
    public String getDescription() { return description; }
}

final class AlertEventBus implements AutoCloseable {
    private final ConcurrentHashMap<AlertType, CopyOnWriteArrayList<AlertObserver>> observers =
            new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public void subscribe(AlertType type, AlertObserver observer) {
        observers.computeIfAbsent(type, ignored -> new CopyOnWriteArrayList<>()).add(observer);
    }

    public CompletableFuture<Void> publish(SystemAlert alert) {
        List<AlertObserver> subscribers = observers.get(alert.getType());
        System.out.println("Bus: publica " + alert.getType() + " (severidad "
                + alert.getSeverity() + ")");
        if (subscribers == null || subscribers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<?>[] tasks = subscribers.stream()
                .map(observer -> CompletableFuture.runAsync(() -> {
                    try {
                        observer.onAlert(alert);
                    } catch (Exception exception) {
                        System.out.println("Bus: oyente de " + alert.getType()
                                + " aislado tras " + exception.getClass().getSimpleName());
                    }
                }, executor))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(tasks);
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
