import java.util.HashMap;
import java.util.Map;

// this class defines the protocol for communication between client and server
// it provides methods for parsing json-like messages and creating message strings
// the protocol uses a simple dictionary/map format with json-like syntax
// all network communication between client and server uses this protocol for serialization
public class Protocol {
    
    /**
     * Parse a JSON-like message string into a map
     * this method parses an incoming message string into a key-value map
     * it handles a simple json-like format with string keys and values
     * nested objects are also supported for complex data structures
     * returns an empty map if the message format is invalid
     * it goes through the string character by character to parse it
     */
    public static Map<String, Object> parseMessage(String message) {
        Map<String, Object> result = new HashMap<>();
        message = message.trim();
        
        // check that message starts and ends with curly braces
        if (!message.startsWith("{") || !message.endsWith("}")) {
            return result;
        }
        
        // remove the outer braces
        message = message.substring(1, message.length() - 1).trim();
        
        if (message.isEmpty()) {
            return result;
        }
        
        // go through the message character by character
        int i = 0;
        while (i < message.length()) {
            // skip spaces
            while (i < message.length() && Character.isWhitespace(message.charAt(i))) {
                i++;
            }
            if (i >= message.length()) break;
            
            // find the key (should be in quotes)
            if (message.charAt(i) != '"') break;
            int keyStart = i + 1;
            int keyEnd = message.indexOf('"', keyStart);
            if (keyEnd == -1) break;
            String key = message.substring(keyStart, keyEnd);
            
            // find the colon after the key
            i = keyEnd + 1;
            while (i < message.length() && message.charAt(i) != ':') {
                i++;
            }
            if (i >= message.length()) break;
            i++; // skip the colon
            
            // skip spaces after the colon
            while (i < message.length() && Character.isWhitespace(message.charAt(i))) {
                i++;
            }
            if (i >= message.length()) break;
            
            // parse the value
            Object value;
            if (message.charAt(i) == '"') {
                // string value - get text between quotes
                int valueStart = i + 1;
                int valueEnd = message.indexOf('"', valueStart);
                if (valueEnd == -1) break;
                value = message.substring(valueStart, valueEnd);
                i = valueEnd + 1;
            } else if (message.charAt(i) == '{') {
                // nested object - handle braces
                int braceCount = 1;
                int objStart = i + 1;
                i++;
                while (i < message.length() && braceCount > 0) {
                    if (message.charAt(i) == '{') braceCount++;
                    else if (message.charAt(i) == '}') braceCount--;
                    i++;
                }
                String objContent = message.substring(objStart, i - 1);
                Map<String, String> dataMap = parseNestedObject(objContent);
                value = dataMap;
            } else {
                // simple value
                int valueEnd = i;
                while (valueEnd < message.length() && message.charAt(valueEnd) != ',' && message.charAt(valueEnd) != '}') {
                    valueEnd++;
                }
                value = message.substring(i, valueEnd).trim();
                i = valueEnd;
            }
            
            result.put(key, value);
            
            // skip to next comma or end
            while (i < message.length() && message.charAt(i) != ',' && message.charAt(i) != '}') {
                i++;
            }
            if (i < message.length() && message.charAt(i) == ',') {
                i++;
            }
        }
        
        return result;
    }
    
    /**
     * Parse nested object (for data field)
     * this parses key-value pairs inside a nested object
     * similar to parsemessage but for the content inside curly braces
     * used for game move data and other nested structures
     * manually parses through the string charachter by charachter to build a map
     */
    private static Map<String, String> parseNestedObject(String content) {
        Map<String, String> result = new HashMap<>();
        content = content.trim();
        // if content is empty, return an empty map
        if (content.isEmpty()) {
            return result;
        }
        
        // go through the content character by character
        int i = 0;
        while (i < content.length()) {
            // skip any whitespace or spaces
            while (i < content.length() && Character.isWhitespace(content.charAt(i))) {
                i++;
            }
            if (i >= content.length()) break;
            
            // find the key which should be in quotes
            if (content.charAt(i) != '"') break;
            int keyStart = i + 1;
            // find where the key ends (at the next quote mark)
            int keyEnd = content.indexOf('"', keyStart);
            if (keyEnd == -1) break;
            String key = content.substring(keyStart, keyEnd);
            
            // find the colon that comes after the key
            i = keyEnd + 1;
            while (i < content.length() && content.charAt(i) != ':') {
                i++;
            }
            if (i >= content.length()) break;
            i++; // skip past the colon
            
            // skip spaces after the colon
            while (i < content.length() && Character.isWhitespace(content.charAt(i))) {
                i++;
            }
            if (i >= content.length()) break;
            
            // get the value which should be in quotes
            if (content.charAt(i) != '"') break;
            int valueStart = i + 1;
            // find where the value ends (at the next quote mark)
            int valueEnd = content.indexOf('"', valueStart);
            if (valueEnd == -1) break;
            String value = content.substring(valueStart, valueEnd);
            // store this key-value pair in the result map
            result.put(key, value);
            
            i = valueEnd + 1;
            
            // skip to the next comma or end
            while (i < content.length() && content.charAt(i) != ',') {
                i++;
            }
            if (i < content.length() && content.charAt(i) == ',') {
                i++;
            }
        }
        
        return result;
    }
    
    /**
     * Create a JSON message string from a map
     * this converts a map of data into a json-like string for sending over network
     * it handles nested maps for complex data
     * the result is a string that can be sent to other clients or the server
     */
    public static String createMessage(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        
        // go through each key-value pair in the map
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            
            // add the key in quotes with a colon
            sb.append("\"").append(entry.getKey()).append("\":");
            
            // add the value
            Object value = entry.getValue();
            if (value instanceof Map) {
                // if the value is a map, put it in braces
                sb.append("{");
                boolean firstData = true;
                @SuppressWarnings("unchecked")
                Map<String, String> dataMap = (Map<String, String>) value;
                for (Map.Entry<String, String> dataEntry : dataMap.entrySet()) {
                    if (!firstData) {
                        sb.append(",");
                    }
                    firstData = false;
                    // add each nested key-value pair
                    sb.append("\"").append(dataEntry.getKey()).append("\":")
                      .append("\"").append(dataEntry.getValue()).append("\"");
                }
                sb.append("}");
            } else {
                // quote regular values
                sb.append("\"").append(value.toString()).append("\"");
            }
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Create a simple message with type and optional fields
     * this is a helper method for creating messages with type and key-value pairs
     * it converts the arguments into a map and then calls createMessage
     * makes it easier to create messages without manually building a map
     */
    public static String createSimpleMessage(String type, String... keyValues) {
        // create a new map to store the message data
        Map<String, Object> map = new HashMap<>();
        // always include the message type
        map.put("type", type);
        
        // add key-value pairs from the varargs
        // go through pairs of values (key at i, value at i+1)
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i + 1 < keyValues.length) {
                map.put(keyValues[i], keyValues[i + 1]);
            }
        }
        
        // convert the map to a json string and return it
        return createMessage(map);
    }
    
    /**
     * Create an error message
     * this creates a standardized error message for sending to clients
     * error messages have type ERROR and include the error text
     */
    public static String createErrorMessage(String error) {
        // create a map for the error message
        Map<String, Object> map = new HashMap<>();
        // set the type to ERROR
        map.put("type", "ERROR");
        // include the error message text
        map.put("message", error);
        // convert and return the json message
        return createMessage(map);
    }
    
    /**
     * Create a success message
     * this creates a standardized success message for sending to clients
     * success messages have type SUCCESS and include a message
     */
    public static String createSuccessMessage(String message) {
        // create a map for the success message
        Map<String, Object> map = new HashMap<>();
        // set the type to SUCCESS
        map.put("type", "SUCCESS");
        // include the success message text
        map.put("message", message);
        // convert and return the json message
        return createMessage(map);
    }
}

