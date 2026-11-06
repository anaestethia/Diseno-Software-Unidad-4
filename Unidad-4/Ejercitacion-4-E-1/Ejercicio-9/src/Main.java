import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    record Event(String type, Map<String, String> payload) {
        Event {
            if (type == null || type.isBlank()) {
                throw new IllegalArgumentException("El tipo de evento es obligatorio");
            }
            payload = Map.copyOf(payload);
        }
    }

    interface EventObserver {
        void onEvent(Event event);
    }

    static final class EventBus implements AutoCloseable {
        private final Map<String, CopyOnWriteArrayList<EventObserver>> subscriptions =
                new ConcurrentHashMap<>();
        private final ExecutorService dispatcher = Executors.newFixedThreadPool(2);

        void subscribe(String eventType, EventObserver observer) {
            subscriptions.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>())
                    .addIfAbsent(observer);
        }

        CompletableFuture<Void> publish(Event event) {
            System.out.printf("Publicado: %s %s%n", event.type(), event.payload());
            return CompletableFuture.runAsync(() -> {
                CopyOnWriteArrayList<EventObserver> listeners = subscriptions.get(event.type());
                if (listeners == null) {
                    return;
                }
                for (EventObserver listener : listeners) {
                    try {
                        listener.onEvent(event);
                    } catch (RuntimeException exception) {
                        System.out.printf("Error aislado en oyente: %s%n", exception.getMessage());
                    }
                }
            }, dispatcher);
        }

        @Override
        public void close() {
            dispatcher.shutdown();
        }
    }

    static EventObserver namedService(String name) {
        return event -> System.out.printf("Servicio %s procesó %s para %s%n",
                name, event.type(), event.payload().get("id"));
    }

    public static void main(String[] args) {
        try (EventBus bus = new EventBus()) {
            bus.subscribe("UsuarioRegistrado", namedService("Bienvenida"));
            bus.subscribe("UsuarioRegistrado", event -> {
                throw new IllegalStateException("servicio de preferencias no disponible");
            });
            bus.subscribe("UsuarioRegistrado", namedService("Analítica"));
            bus.subscribe("PagoAprobado", namedService("Facturación"));
            CompletableFuture.allOf(
                    bus.publish(new Event("UsuarioRegistrado", Map.of("id", "U-208"))),
                    bus.publish(new Event("PagoAprobado", Map.of("id", "P-991")))
            ).join();
        }
    }
}
