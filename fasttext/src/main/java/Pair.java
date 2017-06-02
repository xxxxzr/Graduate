import java.io.Serializable;

/**
 * Created by dugang on 16/9/21.
 */
public class Pair implements Serializable{
    private double first;
    private int second;


    Pair(double first,int second){
        this.first=first;
        this.second=second;
    }

    public String toString(){
        return "first:"+first+" second:"+second;
    }
    public double getFirst() {
        return first;
    }

    public void setFirst(double first) {
        this.first = first;
    }

    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        this.second = second;
    }
}
