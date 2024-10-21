import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class LamportClock {
    private int clock;

    public LamportClock() {
        this.clock = 0;
    }

    // Increment when sending a message
    public synchronized int increment() {
        return clock++;
    }

    // Update the clock when receiving a message
    public synchronized void update(int receivedTime) {
        clock = Math.max(clock, receivedTime) + 1;
    }

    // Get the current time
    public synchronized int getTime() {
        return clock;
    }
}
