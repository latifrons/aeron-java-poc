package latifrons;

public class EnvTool {

    public static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }

    public static String getEnv(String key) {
        return getEnv(key, null);
    }

    public static String mustGetEnv(String key){
        String value = System.getenv(key);
        if (value == null) {
            throw new IllegalArgumentException("Environment variable '" + key + "' is not set.");
        }
        return value;
    }
}
