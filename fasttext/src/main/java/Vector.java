import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * Created by dugang on 16/9/20.
 */
public class Vector implements Serializable {
    int m;
    float[] data;
    public Vector(int m){
        this.m = m;
        data = new float[m];
    }

    public void zero(){
        for (int i = 0; i < m; i++) {
            data[i] = 0.0f;
        }
    }
    public void mul(float a) {
        for (int i = 0; i < m; i++) {
            data[i] *= a;
        }
    }

    //将矩阵A的第i行加到向量
    public void addRow(final Matrix A, int i) {
        for (int j = 0; j < A.n; j++) {
            data[j] += A.data[i * A.n + j];
        }
    }
    //将矩阵A的第i行乘a加到向量
    public void addRow(final Matrix A, int i, float a) {
        for (int j = 0; j < A.n; j++) {
            data[j] += a * A.data[i * A.n + j];
        }
    }

    //矩阵A乘以向量vec
    public void mul(Matrix A, final Vector vec) {
        for (int i = 0; i < m; i++) {
            data[i] = 0.0f;
            for (int j = 0; j < A.n; j++) {
                data[i] += A.data[i * A.n + j] * vec.data[j];
            }
        }
    }

    //取向量最大值
    public int argmax() {
        float max = data[0];
        int argmax = 0;
        for (int i = 1; i < m; i++) {
            if (data[i] > max) {
                max = data[i];
                argmax = i;
            }
        }
        return argmax;
    }

    public float get(int i){
        return data[i];
    }

    public void set(int i,float value){
        data[i] = value;
    }


    public String toString(){
        return Arrays.toString(data);
    }
}
