import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    record SmokeAlert(String sensorId, String zone, String floor) { }

    interface EmergencyResponse {
        void respond(SmokeAlert alert);
    }

    static final class SmokeSensor {
        private final String sensorId;
        private final String zone;
        private final String floor;
        private final List<EmergencyResponse> responses = new CopyOnWriteArrayList<>();

        SmokeSensor(String sensorId, String zone, String floor) {
            this.sensorId = sensorId;
            this.zone = zone;
            this.floor = floor;
        }

        void subscribe(EmergencyResponse response) {
            responses.add(response);
        }

        void detectSmoke() {
            SmokeAlert alert = new SmokeAlert(sensorId, zone, floor);
            System.out.printf("Humo detectado: sensor %s, %s, planta %s%n",
                    sensorId, zone, floor);
            responses.forEach(response -> response.respond(alert));
        }
    }

    static final class AlarmCenter implements EmergencyResponse {
        @Override
        public void respond(SmokeAlert alert) {
            System.out.printf("Central: alarma activada en %s, planta %s%n",
                    alert.zone(), alert.floor());
        }
    }

    static final class Sprinklers implements EmergencyResponse {
        private final Set<String> coveredZones;

        Sprinklers(Set<String> coveredZones) {
            this.coveredZones = Set.copyOf(coveredZones);
        }

        @Override
        public void respond(SmokeAlert alert) {
            if (coveredZones.contains(alert.zone())) {
                System.out.printf("Rociadores: activados en zona %s%n", alert.zone());
            }
        }
    }

    static final class Concierge implements EmergencyResponse {
        @Override
        public void respond(SmokeAlert alert) {
            System.out.printf("Conserjería: avisar evacuación en planta %s%n", alert.floor());
        }
    }

    public static void main(String[] args) {
        SmokeSensor thirdFloor = new SmokeSensor("H-3-07", "Ala norte", "3");
        SmokeSensor basement = new SmokeSensor("H-S-02", "Estacionamiento", "S");
        AlarmCenter alarm = new AlarmCenter();
        Sprinklers sprinklers = new Sprinklers(Set.of("Ala norte", "Laboratorio"));
        Concierge concierge = new Concierge();
        thirdFloor.subscribe(alarm);
        thirdFloor.subscribe(sprinklers);
        thirdFloor.subscribe(concierge);
        basement.subscribe(alarm);
        basement.subscribe(sprinklers);
        basement.subscribe(concierge);
        thirdFloor.detectSmoke();
        basement.detectSmoke();
    }
}
