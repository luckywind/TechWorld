import org.apache.log4j.Logger;

public class LogApp {
    static Logger log = Logger.getLogger(LogApp.class.getName());
    public static void main(String[] args) {
        testLog();

    }

    private static void testLog() {
        log.debug("debug msg ");
        log.info("info msg");
        log.warn("warn msg");
        log.error("error msg");
        log.trace("trace msg");
    }

}
