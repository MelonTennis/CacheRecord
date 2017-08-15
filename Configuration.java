/**
 * Configuration
 */
public class Configuration {
    public boolean keepLog;
    public LOG log;

    // max cache size
    public long CACHE_SIZE;

    // preset occupy of three buckets
    public final float SINGLE_FACTOR;
    public final float MULTI_FACTOR ;

    // if fill rate of cache > MAX_FILE_RATE, the program will evict sth
    // if fill rate of cache < MIN_FILE_RATE, no eviction will happen
    public final float MIN_FILL_RATE;
    public final float SOFT_FILL_RATE;
    public final float HARD_FILL_RATE;

    public final boolean bgThread;

    public Configuration(long size, boolean keepLog, String logPath, float singleFactor, float multiFactor,
                         float minFillRate, float softFillRate, float hardFillRate, boolean bgThread){
        this.CACHE_SIZE = size;
        if(keepLog){
            this.keepLog = true;
            this.log = new LOG(keepLog, logPath);
        }else{
            log = null;
            this.keepLog = false;
        }
        SINGLE_FACTOR = singleFactor;
        MULTI_FACTOR = multiFactor;
        MIN_FILL_RATE = minFillRate;
        SOFT_FILL_RATE = softFillRate;
        HARD_FILL_RATE = hardFillRate;
        this.bgThread = bgThread;
    }
}
