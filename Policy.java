import java.lang.*;

/**
 * This class is a eviction policy for file system
 * The whole space of cache is divided to three buckets
 * onceAccess, multiAccess, inMemory, they have preset percent
 * 0.25p, 0.5p, 0.25p percent of whole cache respectively
 * <p>
 * If a file's priority is set as inMemory, it will be save in inMemory
 * the first time a file is accessed, it will be put in onceAccess
 * if a hit happens in onceAccess, file will be moved to Multi_memory
 * <p>
 * if the cache space is not enough for new files, evict enough space
 * in onceAccess and inMemory, evict based on LRU policy
 * in multiAccess, evict based on LFU policy
 * if inMemroy policy is set, evict from onceAccess and multiAccess first,
 * when evicting, try to keep their space ratio as 1:2 after evicting
 * else treat the three chunks equally, evict the part over preset percent,
 * and keep their ratio approach 1:2:1 after eviction if possible
 * No eviction will happen when cache space is enough
 */
public class Policy {

    // if LOG = true, info is print to logPath
    private boolean keepLog = false;
    private LOG log;

    // max cache size
    private long CACHE_SIZE;

    // preset occupy of three buckets
    private final float SINGLE_FACTOR;
    private final float MULTI_FACTOR;

    // if fill rate of cache > MAX_FILE_RATE, the program will evict sth
    // if fill rate of cache < MIN_FILE_RATE, no eviction will happen
    private final float MIN_FILL_RATE;
    private final float SOFT_FILL_RATE;
    private final float HARD_FILL_RATE;

    // three file bucket, each contains file information with different priority
    protected FileBucket multiAccess;
    protected FileBucket onceAccess;

    // static of this cache policy
    private int hitCount;
    private int missCount;

    // current cacheFile, should not be evicted currently
    private String currentFileName = null;
    // eviction thread
    private final boolean bgThread;
    private final EvictThread evictThread;

    public Policy(Configuration conf) {
        this.keepLog = conf.keepLog;
        this.log = new LOG(conf.log);
        this.CACHE_SIZE = conf.CACHE_SIZE;
        this.SINGLE_FACTOR = conf.SINGLE_FACTOR;
        this.MULTI_FACTOR = conf.MULTI_FACTOR;
        this.MIN_FILL_RATE = conf.MIN_FILL_RATE;
        this.HARD_FILL_RATE = conf.HARD_FILL_RATE;
        this.SOFT_FILL_RATE = conf.SOFT_FILL_RATE;
        this.bgThread = conf.bgThread;
        long maxSingle = (long) (CACHE_SIZE * SINGLE_FACTOR);
        long maxMulti = (long) (CACHE_SIZE * MULTI_FACTOR);
        this.onceAccess = new FileBucket(maxSingle, "onceAccess", "LRU");
        this.multiAccess = new FileBucket(maxMulti, "multiAccess", "LFU");
        this.onceAccess.log = log;
        this.multiAccess.log = log;
        this.hitCount = 0;
        this.missCount = 0;
        if (!bgThread) {
            evictThread = null;
        } else {
            evictThread = new EvictThread(this);
            evictThread.start();
        }
    }

    public void setCacheSize(long size) {
        this.CACHE_SIZE = size;
    }

    public long overflow() {
        long totalSize = onceAccess.getBucketSize() + multiAccess.getBucketSize();
        long hardMax = (long) (HARD_FILL_RATE * CACHE_SIZE);
        long softMax = (long) (SOFT_FILL_RATE * CACHE_SIZE);
        return totalSize > hardMax ? totalSize - softMax : 0;
    }

    public synchronized void cacheFileOpen(String filePath) {
        dbgUtility.dbg_print("cacheFileOpen " + filePath);
        dbgUtility.dbg_requires(overflow() == 0, dbgUtility.curLine());
        FileInfo curFile;
        currentFileName = filePath;
        if (onceAccess.contains(filePath)) {
            // move curFile from onceAccess to multiAccess
            curFile = onceAccess.getFile(filePath);
            onceAccess.remove(curFile);
            curFile.access();
            multiAccess.addFile(curFile);
            hitCount++;
        } else if (multiAccess.contains(filePath)) {
            // renew file info
            curFile = multiAccess.getFile(filePath);
            multiAccess.accessFile(curFile);
            hitCount++;
        } else {
            curFile = new FileInfo(filePath);
            onceAccess.addFile(curFile);
            missCount++;
        }
        currentFileName = null;
    }

    public synchronized void cacheFileRead(String filePath, long size) {
        dbgUtility.dbg_print("cacheFileRead " + filePath + " " + size);
        currentFileName = filePath;
        if (onceAccess.contains(filePath)) {
            FileInfo curFile = onceAccess.getFile(filePath);
            onceAccess.renewFile(curFile, size);
        } else if (multiAccess.contains(filePath)) {
            FileInfo curFile = multiAccess.getFile(filePath);
            multiAccess.renewFile(curFile, size);
        } else {
            cacheFileOpen(filePath);
            cacheFileRead(filePath, size);
        }
        dbgUtility.dbg_print("cache cur size: " + this.curFillSize());
        evict(overflow());
        currentFileName = null;
    }

    public synchronized void evict(long toFree) {
        dbgUtility.dbg_print("evict cache: " + toFree);
        if (toFree == 0) {
            return;
        }
        long freed = 0;
        long once_size = onceAccess.getBucketSize();
        long multi_size = multiAccess.getBucketSize();
        dbgUtility.dbg_requires(once_size + multi_size - toFree > 0, dbgUtility.curLine());
        long remain = once_size + multi_size - toFree;
        // evict due to the ratio of once/multi, keep ratio stable after eviction
        // evict from multiAccess
        if (once_size < remain * SINGLE_FACTOR) {
            freed += multiAccess.evict(toFree, currentFileName);
            // evict from onceAccess
        } else if (multi_size < remain * MULTI_FACTOR) {
            freed += onceAccess.evict(toFree, currentFileName);
        } else {
            freed += onceAccess.evict(once_size - (long) (remain * SINGLE_FACTOR), currentFileName);
            freed += multiAccess.evict(toFree - freed, currentFileName);
        }
        dbgUtility.dbg_requires(freed >= toFree, dbgUtility.curLine());
    }

    public void setLog(boolean setLog, String logPath) {
        this.log = new LOG(setLog, logPath);
        this.onceAccess.log = log;
        this.onceAccess.log = log;
    }

    public boolean checkBucket(FileBucket bucket) {
        int totalSize = 0;
        int cnt = 0;
        for (FileInfo file : bucket.files) {
            System.out.println(file.getFilePath());
            cnt++;
            totalSize += file.getFileSize();
        }
        if (totalSize != bucket.getBucketSize()) return false;
        if (cnt != bucket.files.size()) return false;
        dbgUtility.dbg_requires(totalSize == bucket.getBucketSize(), dbgUtility.curLine());
        dbgUtility.dbg_requires(cnt == bucket.files.size(), dbgUtility.curLine());
        return true;
    }

    public boolean checkCache() {
        if (SINGLE_FACTOR + MULTI_FACTOR != 1) return false;
        if (MIN_FILL_RATE >= 1) return false;
        if (HARD_FILL_RATE < 0 || SOFT_FILL_RATE > 1) return false;
        if (!checkBucket(onceAccess)) return false;
        if (!checkBucket(multiAccess)) return false;
        dbgUtility.dbg_requires(SINGLE_FACTOR + MULTI_FACTOR == 1.0f, dbgUtility.curLine());
        return true;
    }

    private boolean cacheConatins(String fileName) {
        return onceAccess.contains(fileName) || multiAccess.contains(fileName);
    }

    private FileInfo getCacheFile(String fileName) {
        FileInfo curFile = onceAccess.getFile(fileName);
        if (curFile != null) {
            return curFile;
        } else {
            return multiAccess.getFile(fileName);
        }
    }

    public void printCache() {
        this.onceAccess.printBucket();
        this.multiAccess.printBucket();
    }

    public void printConfig() {
        System.out.println("Cache size: " + CACHE_SIZE);
        System.out.println("SINGLE factor: " + SINGLE_FACTOR + " MULTI factor: " + MULTI_FACTOR);
        System.out.println("hard fill rate: " + HARD_FILL_RATE + " soft fill rate: " + SOFT_FILL_RATE);
    }

    private long curFillSize() {
        dbgUtility.dbg_print("once size: " + onceAccess.getBucketSize() + " multi size: " + multiAccess.getBucketSize());
        return onceAccess.getBucketSize() + multiAccess.getBucketSize();
    }

    public double getHitRate() {
        return this.hitCount / (double) (this.hitCount + this.missCount);
    }

    public double getMissRate() {
        return this.missCount / (double) (this.hitCount + this.missCount);
    }

    public long cacheSize() {
        return this.CACHE_SIZE;
    }
}
