import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Button button = new Button();
        button.addListener(event -> System.out.println("Ventana principal: clic recibido en " + event));
        button.addListener(event -> System.out.println("Contador: se registró un clic en " + event));

        button.click();
    }
}

interface ButtonClickListener {
    void onClick(String buttonName);
}

final class Button {
    private final List<ButtonClickListener> listeners = new ArrayList<>();
    private final String name = "Guardar";

    public void addListener(ButtonClickListener listener) {
        listeners.add(listener);
    }

    public void click() {
        System.out.println("Clic simulado en el botón " + name);
        for (ButtonClickListener listener : new ArrayList<>(listeners)) {
            listener.onClick(name);
        }
    }
}
