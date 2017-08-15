import java.util.Date;

/**
 * Information of file:
 * path: path of file
 * size: size of file kept in file system
 * range: range of file, not used
 * last access time: time of last time acces
 * total access count: count of total access number
 */
public class FileInfo {

    // max size of one file
    protected final long MAX_SIZE = Integer.MAX_VALUE;
    // size of current file
    protected long size;
    // last access time
    protected Date lastAccessTime;
    // total access number
    protected int accessCount;
    // path
    protected final String filePath;
    // range, not usd
    protected Range range;

    public FileInfo(String path) {
        this.filePath = path;
        this.size = 0;
        this.lastAccessTime = new Date();
        this.accessCount = 1;
    }

    public FileInfo(String path, long size) {
        this.filePath = path;
        this.size = size;
        this.lastAccessTime = new Date();
        this.accessCount = 1;
    }

    public FileInfo(String path, long size, Range range) {
        this.filePath = path;
        this.size = size;
        this.range = range;
        this.lastAccessTime = new Date();
        this.accessCount = 1;
    }

    public void access() {
        this.accessCount++;
        this.lastAccessTime = new Date();
    }

    public void changeSize(long newSize) {
        this.size = newSize;
    }

    public void access(long newSize, Range newRange) {
        this.accessCount++;
        this.size = newSize;
        this.range = newRange;
        this.lastAccessTime = new Date();
    }

    public long getFileSize() {
        return this.size;
    }

    public int getAccessCount() {
        return this.accessCount;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public Date getLastAccessTime() {
        return this.lastAccessTime;
    }

    public Range getFileRange() {
        return this.range;
    }

    class Range {
        protected int offset;
        protected int length;

        Range(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }

        public void mergeRange(Range that) {
            int thisEnd = this.offset + this.length;
            int thatEnd = that.offset + that.length;
            this.offset = Math.min(this.offset, that.offset);
            int end = Math.max(thisEnd, thatEnd);
            this.length = end - this.offset;
        }
    }
}
