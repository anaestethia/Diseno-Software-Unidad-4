import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    record AuditEvent(String employeeId, String operation, String details) { }

    interface AuditObserver {
        void onAudit(AuditEvent event);
    }

    static final class EmployeeRegistry {
        private final List<AuditObserver> observers = new CopyOnWriteArrayList<>();

        void subscribe(AuditObserver observer) {
            observers.add(observer);
        }

        void unsubscribe(AuditObserver observer) {
            observers.remove(observer);
        }

        void updateEmployee(String id, String details) {
            AuditEvent event = new AuditEvent(id, "MODIFICACIÓN", details);
            System.out.printf("Registro actualizado: empleado %s%n", id);
            observers.forEach(observer -> observer.onAudit(event));
        }
    }

    static final class ConsoleAuditor implements AuditObserver {
        @Override
        public void onAudit(AuditEvent event) {
            System.out.printf("Consola: %s empleado %s (%s)%n",
                    event.operation(), event.employeeId(), event.details());
        }
    }

    static final class FileAuditor implements AuditObserver {
        private final Path file;

        FileAuditor(Path file) {
            this.file = file;
        }

        @Override
        public void onAudit(AuditEvent event) {
            try {
                Files.writeString(file, String.format("%s empleado %s (%s)%n",
                        event.operation(), event.employeeId(), event.details()),
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
                System.out.printf("Archivo: auditoría guardada en %s%n", file);
            } catch (IOException exception) {
                throw new IllegalStateException("No se pudo escribir la auditoría", exception);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Path auditFile = Path.of("audit.log");
        Files.deleteIfExists(auditFile);
        EmployeeRegistry registry = new EmployeeRegistry();
        ConsoleAuditor console = new ConsoleAuditor();
        FileAuditor file = new FileAuditor(auditFile);
        registry.subscribe(console);
        registry.subscribe(file);
        registry.updateEmployee("E-104", "actualización de área");
        registry.unsubscribe(file);
        registry.updateEmployee("E-104", "actualización de turno");
        System.out.printf("Entradas persistidas: %d%n", Files.readAllLines(auditFile).size());
        Files.deleteIfExists(auditFile);
    }
}
