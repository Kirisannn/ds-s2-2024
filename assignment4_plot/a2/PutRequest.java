import org.json.JSONArray;
import org.json.JSONObject;

public class PutRequest {
    public int LamportClockValue;
    // JSON array contains request data
    public JSONArray JsonArray;
    
    // Initialize lamport clock value and JSON array
    public PutRequest(int LamportClockValue, JSONArray JsonArray) {
        this.LamportClockValue = LamportClockValue;
        this.JsonArray = JsonArray;
    }
    
    // Initialize from a JSONObject
    public PutRequest(JSONObject putRequest) {
        this.LamportClockValue = putRequest.getInt("LamportClockValue");
        this.JsonArray = putRequest.getJSONArray("JsonArray");
    }
    
    public int getLamportClockValue() {
        return LamportClockValue;
    }

    public JSONArray getJsonArray() {
        return JsonArray;
    }
}
