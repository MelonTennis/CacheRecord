import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * LOG class, print log output to specific file
 */
public class LOG {
    public boolean keepLog;
    public final String logFile;

    public LOG(boolean log, String path) {
        this.keepLog = log;
        this.logFile = path;
    }

    public LOG(LOG log1) {
        if (log1 == null) {
            this.keepLog = false;
            this.logFile = null;
        } else {
            this.keepLog = log1.keepLog;
            if (log1.logFile != null && log1.keepLog) {
                this.logFile = new String(log1.logFile);
            } else {
                this.logFile = null;
            }
        }
    }

    public void logPrint(String str) {
        if (this.keepLog) {
            if (this.logFile != null) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(this.logFile))) {
                    bw.write(str);
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println(str);
            }
        }
    }
}
