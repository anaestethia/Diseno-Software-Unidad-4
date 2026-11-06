import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    enum RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

    record RiskProfile(String customerCode, RiskLevel level, String reason) {
        RiskProfile {
            if (customerCode == null || !customerCode.matches("[A-Z0-9-]{3,20}")) {
                throw new IllegalArgumentException("Código de cliente inválido");
            }
            if (level == null || reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("El nivel y el motivo son obligatorios");
            }
        }
    }

    record ObserverResult(String observer, boolean successful, String detail) { }

    record AuditRecord(Instant timestamp, RiskProfile profile, List<ObserverResult> results) {
        AuditRecord {
            results = List.copyOf(results);
        }
    }

    interface RegulatoryObserver {
        String name();
        void onRiskChanged(RiskProfile profile);
    }

    static final class AuditLedger {
        private final List<AuditRecord> records = new ArrayList<>();

        synchronized void append(AuditRecord record) {
            records.add(record);
        }

        synchronized List<AuditRecord> snapshot() {
            return List.copyOf(records);
        }
    }

    static final class RiskProfileService {
        private final CopyOnWriteArrayList<RegulatoryObserver> observers =
                new CopyOnWriteArrayList<>();
        private final ReentrantLock processingLock = new ReentrantLock(true);
        private final AuditLedger ledger;

        RiskProfileService(AuditLedger ledger) {
            this.ledger = ledger;
        }

        void registerMandatory(RegulatoryObserver observer) {
            if (observer == null || observer.name() == null || observer.name().isBlank()) {
                throw new IllegalArgumentException("El observador regulatorio debe tener nombre");
            }
            processingLock.lock();
            try {
                if (observers.stream().anyMatch(existing -> existing.name().equals(observer.name()))) {
                    throw new IllegalArgumentException("Observador duplicado: " + observer.name());
                }
                observers.add(observer);
            } finally {
                processingLock.unlock();
            }
        }

        AuditRecord changeRisk(RiskProfile profile) {
            processingLock.lock();
            try {
                List<ObserverResult> results = new ArrayList<>();
                for (RegulatoryObserver observer : observers) {
                    try {
                        observer.onRiskChanged(profile);
                        results.add(new ObserverResult(observer.name(), true, "procesado"));
                    } catch (RuntimeException exception) {
                        results.add(new ObserverResult(observer.name(), false,
                                exception.getMessage() == null ? "error sin detalle" : exception.getMessage()));
                    }
                }
                AuditRecord record = new AuditRecord(Instant.now(), profile, results);
                ledger.append(record);
                System.out.printf("Auditoría %s: %d éxitos, %d incidencias%n",
                        profile.customerCode(),
                        results.stream().filter(ObserverResult::successful).count(),
                        results.stream().filter(result -> !result.successful()).count());
                for (ObserverResult result : record.results()) {
                    System.out.printf("  %s: %s (%s)%n", result.observer(),
                            result.successful() ? "OK" : "INCIDENCIA", result.detail());
                }
                return record;
            } finally {
                processingLock.unlock();
            }
        }
    }

    static RegulatoryObserver regulator(String name, boolean failOnHighRisk) {
        return new RegulatoryObserver() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void onRiskChanged(RiskProfile profile) {
                System.out.printf("%s procesa cliente %s: %s%n",
                        name, profile.customerCode(), profile.level());
                if (failOnHighRisk && profile.level().ordinal() >= RiskLevel.HIGH.ordinal()) {
                    throw new IllegalStateException("sistema no disponible");
                }
            }
        };
    }

    public static void main(String[] args) throws InterruptedException {
        AuditLedger ledger = new AuditLedger();
        RiskProfileService service = new RiskProfileService(ledger);
        service.registerMandatory(regulator("Cumplimiento legal", false));
        service.registerMandatory(regulator("Prevención de lavado", true));
        service.registerMandatory(regulator("Reportes regulatorios", false));

        try {
            new RiskProfile("cliente/104", RiskLevel.HIGH, "operación inusual");
            throw new IllegalStateException("La validación debía rechazar el código");
        } catch (IllegalArgumentException expected) {
            System.out.println("Entrada de riesgo rechazada: código inválido");
        }
        try {
            service.registerMandatory(regulator("Cumplimiento legal", false));
            throw new IllegalStateException("El registro duplicado debía rechazarse");
        } catch (IllegalArgumentException expected) {
            System.out.println("Registro regulatorio duplicado rechazado");
        }

        service.changeRisk(new RiskProfile("C-104", RiskLevel.HIGH, "operación inusual"));
        service.changeRisk(new RiskProfile("C-208", RiskLevel.MEDIUM, "revisión periódica"));

        ExecutorService updates = Executors.newFixedThreadPool(2);
        updates.submit(() -> service.changeRisk(
                new RiskProfile("C-301", RiskLevel.LOW, "verificación concurrente")));
        updates.submit(() -> service.changeRisk(
                new RiskProfile("C-302", RiskLevel.MEDIUM, "verificación concurrente")));
        updates.shutdown();
        if (!updates.awaitTermination(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Las notificaciones concurrentes no finalizaron");
        }

        List<AuditRecord> immutableSnapshot = ledger.snapshot();
        System.out.printf("Registros inmutables de auditoría: %d%n", immutableSnapshot.size());
    }
}
