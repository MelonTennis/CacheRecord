/**
 * contains class method for debug
 */
public class dbgUtility {

    public static void dbg_requires(boolean input, int line) {
        if (!input) {
            System.out.printf("err %d\n", line);
        }
    }

    public static void dbg_print(String str) {
        System.out.println(str);
    }

    public static int curLine() {
        return Thread.currentThread().getStackTrace()[2].getLineNumber();
    }
}
