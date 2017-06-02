import com.chinamobile.cmdi.securityteam.tools.StreamUtil;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Created by dugang on 16/9/20.
 */
public class Matrix implements Serializable{
    float data[];
    int m;
    int n;
    public Matrix(){
        m=0;
        n=0;
        data=null;
    }
    public Matrix(int m,int n){
        System.out.println(m+"\t"+n);

        this.m=m;
        this.n=n;
        data = new float[m*n];
    }

    public Matrix(Matrix other){
        m = other.m;
        n = other.n;
        data = new float[m * n];
        for (int i = 0; i < (m * n); i++) {
            data[i] = other.data[i];
        }
    }

    public void zero(){
        for (int i = 0; i < (m * n); i++) {
            data[i] = 0.0f;
        }

    }

    public  void uniform(float a) {
        Random r = new Random();
        for (int i = 0; i < (m* n); i++) {
            data[i] = r.nextFloat();
        }
    }

    public void addRow(Vector vec, int i, float a) {
        for (int j = 0; j < n; j++) {
            data[i * n + j] += a * vec.data[j];
        }
    }

    public float dotRow(final Vector vec, int i) {
        float d = 0.0f;
        for (int j = 0; j < n; j++) {
            d += data[i * n + j] * vec.data[j];
        }
        return d;
    }

    public void save(ObjectOutputStream oos) throws IOException {
        oos.writeInt(m);
        oos.writeInt(n);
        for(float w:data){
            oos.writeDouble(w);
        }
    }

    public void load(ObjectInputStream ois) throws IOException {
        int m = ois.readInt();
        int n = ois.readInt();
        this.data = new float[m*n];
        for(int i=0;i<m*n;i++){
            data[i] = ois.readFloat();
        }
    }

    public static void main(String[] args) {
        System.out.println("ok");
    }
}
