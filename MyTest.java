import org.junit.*;

import static org.junit.Assert.*;

import java.io.*;

/**
 * Test
 */
public class MyTest {

    Configuration conf = new Configuration(100, false, null, 0.25f, 0.75f, 0.1f, 0.8f, 0.90f, false);
    Policy testPolicy;

    public void testFile(String input) {
        try {
            File file = new File(input);
            BufferedReader bf = new BufferedReader(new FileReader(file));
            String line;
            while ((line = bf.readLine()) != null) {
                String[] lines = line.split(",");
                if (lines.length >= 2) {
                    if (lines[1].equals("O")) {
                        testPolicy.cacheFileOpen(lines[0]);
                    } else if (lines[1].equals("R")) {
                        testPolicy.cacheFileRead(lines[0], Integer.parseInt(lines[2]));
                    } else {
                        System.err.println("Unknown type");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInit() {
        // assert statements
        testPolicy = new Policy(conf);
        assertTrue(testPolicy.overflow() == 0);
        assertTrue(testPolicy.checkCache());
        testPolicy.printConfig();
    }

    @Test
    public void test1() {
        // onceAccess evict
        testPolicy = new Policy(conf);
        String file1 = "/Users/yjin/Desktop/files/cache/traces/smallTest1";
        testFile(file1);
        testPolicy.printCache();
        for (FileInfo file : testPolicy.onceAccess.files) {
            assertTrue(file.getAccessCount() == 1);
        }
        System.out.println("hit rate: " + testPolicy.getHitRate());
        System.out.println("Test1 end\n\n");
    }

    @Test
    public void test2() {
        // currentFile evict
        testPolicy = new Policy(conf);
        String file2 = "/Users/yjin/Desktop/files/cache/traces/smallTest2";
        testFile(file2);
        testPolicy.printCache();
        System.out.println("hit rate: " + testPolicy.getHitRate());
        System.out.println("Test2 end\n\n");
    }

    @Test
    public void test3() {
        // both evict
        testPolicy = new Policy(conf);
        String file3 = "/Users/yjin/Desktop/files/cache/traces/smallTest3";
        testFile(file3);
        testPolicy.printCache();
        System.out.println("hit rate: " + testPolicy.getHitRate());
        System.out.println("Test3 end\n\n");
    }

    @Test
    public void test4() {
        // multiAccess evict
        testPolicy = new Policy(conf);
        String file4 = "/Users/yjin/Desktop/files/cache/traces/smallTest4";
        testFile(file4);
        testPolicy.printCache();
        System.out.println("hit rate: " + testPolicy.getHitRate());
        System.out.println("Test4 end\n\n");
    }

}
