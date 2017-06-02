/**
 * Created by dugang on 16/9/20.
 */
public class Util {
    static float[] t_sigmoid;
    static float[] t_log;

    final static int SIGMOID_TABLE_SIZE =2048;
    final static int MAX_SIGMOID=16;
    final static int LOG_TABLE_SIZE=1024;
    static{
        initTables();
    }
    public static float log(float x) {
        if (x > 1.0) {
            return 0.0f;
        }
        int i = Double.valueOf(x * LOG_TABLE_SIZE).intValue();
        return t_log[i];
    }


    public static float sigmoid(float x) {
            if (x < -MAX_SIGMOID) {
                return 0.0f;
            } else if (x > MAX_SIGMOID) {
                return 1.0f;
            } else {
                int i = Double.valueOf((x + MAX_SIGMOID) * SIGMOID_TABLE_SIZE / MAX_SIGMOID / 2).intValue();
                return t_sigmoid[i];
            }
        }

    public static void  initTables() {
        initSigmoid();
        initLog();
    }

    public static void  initSigmoid() {
        if (t_sigmoid != null){
            return;
        }
        t_sigmoid = new float[SIGMOID_TABLE_SIZE + 1];
        for (int i = 0; i < SIGMOID_TABLE_SIZE + 1; i++) {
            float x = i * 2 * MAX_SIGMOID*1.0f/ SIGMOID_TABLE_SIZE - MAX_SIGMOID;
            t_sigmoid[i] = 1.0f/ (1.0f + (float) Math.exp(-x));
        }
    }

    public static void initLog() {
        if (t_log != null) {
            return;
        }
        t_log = new float[LOG_TABLE_SIZE + 1];
        for (int i = 0; i < LOG_TABLE_SIZE + 1; i++) {
            float x = (Float.valueOf(i) + 1e-5f) / LOG_TABLE_SIZE;
            t_log[i] = (float) Math.log(x);
        }
    }

    public static void main(String[] arg) {
        System.out.println(Util.log(0.19f)+"\t"+Math.log(0.19f));
    }
}
