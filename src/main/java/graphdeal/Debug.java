package graphdeal;

/**
 * @Author Yuxuan Shi
 * @Date 11/12/2019
 * @Time 12:50 PM
 */
public class Debug {
    public static Boolean DEBUGOneStar = false;
    public static int cnt = 0;

    public static void cleanCnt(){
        cnt = 0;
    }

    public static void addCnt(){
        cnt++;
    }

    public static void deleteCnt(){
        cnt--;
    }

    public static void printCnt(){
        System.out.println("cnt's value is " + cnt);
    }

    public static long second = 0;
    private static long start = -1;
    public static void cleanSecond() { second = 0;}
    public static void startSecond() { start = System.currentTimeMillis();}
    public static void endSecond() {
        second = second + System.currentTimeMillis() - start;
    }
    public static void printSecond(){
        System.out.println("time is " + second/1000D + "s");
    }
}
