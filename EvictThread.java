/**
 * A common thread
 */
public class EvictThread implements Runnable {
    private Thread thread;
    private String threadName;
    private Policy cache;

    public EvictThread(String name){
        thread =  new Thread(this, name);
        threadName = name;
        System.out.println("Create thread: " + threadName);
    }

    public EvictThread(Policy cache){
        this.cache = cache;
        thread =  new Thread(this);
    }

    public void run(){
        synchronized (this){
            try{
                this.wait(10000);
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
            }
            if(cache != null){
                cache.evict(cache.overflow());
            }
        }
    }

    public void start(){
        thread.start();
    }

    public void interrupt(){
        thread.interrupt();
    }

    public final void setName(String name) {
        thread.setName(name);
    }

}
