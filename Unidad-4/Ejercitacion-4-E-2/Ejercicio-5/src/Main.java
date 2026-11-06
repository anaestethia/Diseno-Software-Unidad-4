import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    public static void main(String[] args) {
        SunlightSensor sensor = new SunlightSensor();
        sensor.addListener(new BlindController());
        sensor.addListener(new LightController());

        sensor.measure(900);
        sensor.measure(250);
    }
}

interface SunlightListener {
    void onLightLevelChanged(int lux);
}

final class SunlightSensor {
    private final List<SunlightListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(SunlightListener listener) {
        listeners.add(listener);
    }

    public void measure(int lux) {
        if (lux < 0) {
            throw new IllegalArgumentException("La luminosidad no puede ser negativa");
        }
        System.out.println("Sensor fotoeléctrico: " + lux + " lux");
        for (SunlightListener listener : listeners) {
            listener.onLightLevelChanged(lux);
        }
    }
}

final class BlindController implements SunlightListener {
    @Override
    public void onLightLevelChanged(int lux) {
        System.out.println("Persianas: " + (lux >= 700 ? "cerradas" : "abiertas"));
    }
}

final class LightController implements SunlightListener {
    @Override
    public void onLightLevelChanged(int lux) {
        int brightness = lux >= 700 ? 40 : 100;
        System.out.println("Luces interiores: brillo al " + brightness + "%");
    }
}
