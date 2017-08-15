import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FileBucket is a bucket of files
 * size: size of all files in this bucket
 * priority: onceAccess, multiAccess or inMemory
 */
public class FileBucket {

    // onceAccess, multiAccess or inMemory
    protected final String priority;
    // total size of all files in this bucket
    private long size;
    // max size this bucket can be
    private final long MAX_SIZE;
    // all files in this bucket, policy can be "LFU" or "LRU"
    protected Queue<FileInfo> files;
    // map <fileName, FileInfo>
    private Map<String, FileInfo> map;
    // log
    protected LOG log;

    public FileBucket(long maxSize, String priority, String policy) {
        this.priority = new String(priority);
        this.MAX_SIZE = maxSize;
        this.size = 0;
        this.map = new ConcurrentHashMap<>();
        if (policy.toLowerCase().equals("lfu")) {
            this.files = new PriorityQueue<FileInfo>(1, new FileComparatorLFU());
        } else if (policy.toLowerCase().equals("lru")) {
            this.files = new LinkedList<FileInfo>();
        } else {
            dbgUtility.dbg_print("evict policy not existed");
        }
    }

    protected synchronized FileInfo evitOneFile() {
        if (this.files == null) return null;
        if (this.files instanceof LinkedList) {
            FileInfo victim = (FileInfo) ((LinkedList) (this.files)).poll();
            return victim;
        } else if (this.files instanceof PriorityQueue) {
            return (FileInfo) ((PriorityQueue) (this.files)).poll();
        } else {
            return null;
        }
    }

    public long evict(long toFree) {
        long freed = 0;
        while (freed < toFree && this.size > 0) {
            FileInfo victimFile = evitOneFile();
            long evictSize = victimFile.getFileSize();
            String fileName = victimFile.getFilePath();
            System.out.println("From " + priority + " evict" + fileName);
            if (log.keepLog) {
                log.logPrint("From " + priority + " evict" + fileName);
            }
            freed += evictSize;
            map.remove(fileName);
            this.size -= evictSize;
        }
        dbgUtility.dbg_print("evict from bucket: " + freed);
        dbgUtility.dbg_requires(toFree <= freed, dbgUtility.curLine());
        return freed;
    }

    public long evict(long toFree, String currentFileName) {
        dbgUtility.dbg_print("evict " + priority + ": " + toFree);
        if (currentFileName == null) {
            return evict(toFree);
        }
        long freed = 0;
        FileInfo temp = null;
        while (freed < toFree && this.size > 0) {
            FileInfo victimFile = evitOneFile();
            dbgUtility.dbg_requires(victimFile != null, dbgUtility.curLine());
            long evictSize = victimFile.getFileSize();
            String fileName = victimFile.getFilePath();
            if (fileName.equals(currentFileName)) {
                temp = victimFile;
            } else {
                System.out.println("From " + priority + " evict " + fileName);
                if (log.keepLog) {
                    log.logPrint("From " + priority + " evict " + fileName);
                }
                map.remove(fileName);
                freed += evictSize;
                this.size -= evictSize;
            }
            if (currentFileName != null && this.contains(currentFileName)
                    && this.getFile(currentFileName).getFileSize() == this.size) {
                break;
            }
        }
        if (temp != null) {
            files.add(temp);
        }
        dbgUtility.dbg_print("Freed from " + priority + ": " + freed);
        return freed;
    }

    public boolean contains(String fileName) {
        return map.containsKey(fileName);
    }

    public boolean contains(FileInfo curFile) {
        return map.containsValue(curFile);
    }

    public FileInfo getFile(String name) {
        return map.get(name);
    }

    public void addFile(FileInfo file) {
        String key = file.getFilePath();
        map.put(key, file);
        files.add(file);
        size += file.getFileSize();
    }

    public void renewFile(FileInfo curFile, FileInfo.Range range) {
        dbgUtility.dbg_requires(this.contains(curFile), dbgUtility.curLine());
        long preSize = curFile.getFileSize();
        curFile.range.mergeRange(range);
        curFile.access(curFile.range.length, range);
        files.remove(curFile);
        size -= preSize;
        files.add(curFile);
        size += curFile.getFileSize();
    }

    public void renewFile(FileInfo curFile, long newSize) {
        dbgUtility.dbg_requires(this.contains(curFile), dbgUtility.curLine());
        long preSize = curFile.getFileSize();
        files.remove(curFile);
        size -= preSize;
        curFile.changeSize(newSize);
        files.add(curFile);
        size += curFile.getFileSize();
    }

    public void accessFile(FileInfo curFile) {
        dbgUtility.dbg_requires(this.contains(curFile), dbgUtility.curLine());
        files.remove(curFile);
        curFile.access();
        files.add(curFile);
    }

    public FileInfo remove(FileInfo file) {
        String path = file.getFilePath();
        if (!this.contains(path)) {
            return null;
        } else {
            map.remove(path);
            files.remove(file);
            size -= file.getFileSize();
            return file;
        }
    }

    public void printBucket() {
        System.out.println("Bucket: " + priority + "total size: " + size);
        for (FileInfo file : files) {
            System.out.println(file.getFilePath() + " " + file.getFileSize());
        }
    }

    public long getBucketSize() {
        return this.size;
    }

    protected long overflow() {
        return size > MAX_SIZE ? size - MAX_SIZE : 0;
    }

    class FileComparatorLFU implements Comparator<FileInfo> {
        public int compare(FileInfo o1, FileInfo o2) {
            int cnt1 = o1.getAccessCount();
            int cnt2 = o2.getAccessCount();
            if (cnt1 != cnt2) {
                return cnt1 > cnt2 ? 1 : -1;
            } else {
                return o1.getFileSize() > o2.getFileSize() ? -1 : 1;
            }
        }
    }
}
