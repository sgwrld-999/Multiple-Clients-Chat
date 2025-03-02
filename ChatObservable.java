import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatObservable {
    private final List<ClientHandler> observers = new CopyOnWriteArrayList<>();

    public void addObserver(ClientHandler clientHandler) {
        observers.add(clientHandler);
    }

    public void removeObserver(ClientHandler clientHandler) {
        observers.remove(clientHandler);
    }

    public void broadcast(String message) {
        for (ClientHandler client : observers) {
            try {
                client.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
