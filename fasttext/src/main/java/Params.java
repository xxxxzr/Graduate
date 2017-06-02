/**
 * @author ZhanRan.
 *         Date: 2017/6/1 22:39
 */
public class Params {
    public static int maxn=6;
    public static int minn=3;
    public static int bucket=2000000;
    public static double  lr = 0.05;
    public static  int dim = 100;
    public static  int ws = 5;
    public static  int epoch = 5;
    public static  int minCount = 1;
    public static  int neg = 5; //负采样次数
    public static  int wordNgrams = 1;
    public static  final int MAX_VOCAB_SIZE = 30000000;
    public static  final int MAX_LINE_SIZE = 1024;
    public static int thread = 12;
    public static int lrUpdateRate = 100;
    public static double t = 1e-4;
    public static String label = "__label__";
    public static int verbose = 2;
    public static final int MODEL_CBOW=1;
    public static final int MODEL_SG=2;
    public static final int MODEL_SUP=3;
    public static final int LOSS_HS=1;//hierarchical Softmax
    public static final int LOSS_NS=2; //Negative Sampling
    public static final int LOSS_SOFTMAX=3;
    public static short LABEL=1;
    public static short WORD=0;
    public static int model=MODEL_SUP;
    public static int loss=LOSS_SOFTMAX;
    public static final int  NEGATIVE_TABLE_SIZE = 10000000;
    public static String input="input";
    public static String output="output";

    public static void setSuperivsor(){
        model = MODEL_SUP;
        loss = LOSS_SOFTMAX;
        minCount = 1;
        minn = 0;
        maxn = 0;
        lr = 0.1;
        bucket=0;
    }

}