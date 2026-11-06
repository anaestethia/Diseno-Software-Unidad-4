import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    public static void main(String[] args) {
        Set<String> authorizedOperators = Collections.singleton("analista-01");
        RiskAccount account = new RiskAccount("CORP-204", RiskLevel.LOW, authorizedOperators);
        account.addObserver(new RegulatoryObserver("Prevención AML") {
            @Override
            public void onRiskChanged(RiskChangeEvent event) {
                System.out.println(name + ": caso actualizado a " + event.getNewRisk());
            }
        });
        account.addObserver(new RegulatoryObserver("Análisis legal") {
            @Override
            public void onRiskChanged(RiskChangeEvent event) throws Exception {
                throw new Exception("servicio legal no disponible");
            }
        });
        account.addObserver(new RegulatoryObserver("Organismo regulador") {
            @Override
            public void onRiskChanged(RiskChangeEvent event) {
                System.out.println(name + ": notificación recibida para " + event.getAccountId());
            }
        });

        try {
            account.changeRiskLevel(RiskLevel.HIGH, "usuario-no-autorizado");
        } catch (SecurityException exception) {
            System.out.println("Acceso rechazado; no se modificó el perfil de riesgo");
        }

        RiskAuditReport report = account.changeRiskLevel(RiskLevel.HIGH, "analista-01");
        System.out.println("Riesgo actual: " + account.getRiskLevel());
        System.out.println("Auditoría inmutable:");
        for (RiskAuditRecord record : report.getRecords()) {
            System.out.println("  " + record.getObserver() + " -> "
                    + (record.isSuccessful() ? "ÉXITO" : "INCIDENCIA: " + record.getDetail()));
        }
        try {
            report.getRecords().clear();
        } catch (UnsupportedOperationException exception) {
            System.out.println("Informe de auditoría protegido contra modificaciones");
        }
    }
}

enum RiskLevel {
    LOW, MEDIUM, HIGH
}

interface RiskObserver {
    void onRiskChanged(RiskChangeEvent event) throws Exception;
    String getName();
}

abstract class RegulatoryObserver implements RiskObserver {
    protected final String name;
    RegulatoryObserver(String name) { this.name = name; }
    @Override
    public String getName() { return name; }
}

final class RiskChangeEvent {
    private final String accountId;
    private final RiskLevel oldRisk;
    private final RiskLevel newRisk;
    private final String operatorId;
    private final Instant occurredAt;

    RiskChangeEvent(String accountId, RiskLevel oldRisk, RiskLevel newRisk, String operatorId) {
        this.accountId = accountId;
        this.oldRisk = oldRisk;
        this.newRisk = newRisk;
        this.operatorId = operatorId;
        this.occurredAt = Instant.now();
    }

    public String getAccountId() { return accountId; }
    public RiskLevel getOldRisk() { return oldRisk; }
    public RiskLevel getNewRisk() { return newRisk; }
    public String getOperatorId() { return operatorId; }
    public Instant getOccurredAt() { return occurredAt; }
}

final class RiskAuditRecord {
    private final String observer;
    private final boolean successful;
    private final String detail;

    RiskAuditRecord(String observer, boolean successful, String detail) {
        this.observer = observer;
        this.successful = successful;
        this.detail = detail;
    }

    public String getObserver() { return observer; }
    public boolean isSuccessful() { return successful; }
    public String getDetail() { return detail; }
}

final class RiskAuditReport {
    private final List<RiskAuditRecord> records;

    RiskAuditReport(List<RiskAuditRecord> records) {
        this.records = Collections.unmodifiableList(new ArrayList<>(records));
    }

    public List<RiskAuditRecord> getRecords() { return records; }
}

final class RiskAccount {
    private final String accountId;
    private final Set<String> authorizedOperators;
    private final List<RiskObserver> observers = new CopyOnWriteArrayList<>();
    private RiskLevel riskLevel;

    RiskAccount(String accountId, RiskLevel initialRisk, Set<String> authorizedOperators) {
        if (accountId == null || accountId.trim().isEmpty()
                || initialRisk == null || authorizedOperators == null) {
            throw new IllegalArgumentException("Datos de cuenta inválidos");
        }
        this.accountId = accountId;
        this.riskLevel = initialRisk;
        this.authorizedOperators = Collections.unmodifiableSet(
                new java.util.HashSet<>(authorizedOperators));
    }

    public void addObserver(RiskObserver observer) { observers.add(observer); }

    public synchronized RiskAuditReport changeRiskLevel(RiskLevel newRisk, String operatorId) {
        if (!authorizedOperators.contains(operatorId)) {
            throw new SecurityException("Operador sin autorización");
        }
        if (newRisk == null) {
            throw new IllegalArgumentException("El riesgo no puede ser nulo");
        }
        RiskChangeEvent event = new RiskChangeEvent(accountId, riskLevel, newRisk, operatorId);
        riskLevel = newRisk;
        List<RiskAuditRecord> records = new ArrayList<>();
        System.out.println("Cambio de riesgo: " + event.getOldRisk() + " -> "
                + event.getNewRisk() + " (" + event.getOccurredAt() + ")");
        for (RiskObserver observer : observers) {
            try {
                observer.onRiskChanged(event);
                records.add(new RiskAuditRecord(observer.getName(), true, "procesado"));
            } catch (Exception exception) {
                records.add(new RiskAuditRecord(observer.getName(), false,
                        exception.getClass().getSimpleName() + ": " + exception.getMessage()));
            }
        }
        return new RiskAuditReport(records);
    }

    public synchronized RiskLevel getRiskLevel() { return riskLevel; }
}
