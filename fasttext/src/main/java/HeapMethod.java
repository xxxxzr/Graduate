import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by dugang on 16/9/21.
 */
public class HeapMethod {
    public static <T extends Object> void sort_heap(List<T> data, int begin, int end, Comparator<T> comparator) {
        for(int i=end;i>begin;i--){
            pop_heap(data,begin,i,comparator);
        }
    }

    public static <T extends Object> void push_heap(List<T> data, int begin, int end, Comparator<T> comparator) {
        siftUp(data,begin,end,end-1,comparator);

    }

    public static <T extends Object> void pop_heap(List<T> data, int begin, int end, Comparator<T> comparator) {
        T tmp = data.get(begin);
        data.set(begin,data.get(end-1));
        data.set(end-1,tmp);
        siftDown(data,begin,end-1,0,comparator);
    }

    public static <T extends Object> void make_heap(List<T> data,int begin,int end,Comparator<T> comparator){
        for(int i=begin+(end-begin)/2-1;i>=begin;i--){
            siftDown(data,begin,end,i,comparator);
        }
    }

    public static <T extends Object> void siftDown(List<T> data, int begin,int end,int parent,Comparator<T> comparator) {
        int left = left(begin,parent);
        int right = right(begin,parent);
        int largest = parent;
        if (left < end && comparator.compare(data.get(left), data.get(parent)) > 0) {
            largest = left;
        }
        if (right < end && comparator.compare(data.get(right), data.get(largest)) > 0) {
            largest = right;
        }
        if (largest == parent || largest>=end){
            return;
        }else{
            T tmp = data.get(largest);
            data.set(largest,data.get(parent)) ;
            data.set(parent,tmp);
            siftDown(data,begin,end,largest,comparator);
        }
    }

    public static <T extends Object> void siftUp(List<T> data,int begin,int end,int child,Comparator<T> comparator){
        int parent = parent(begin, child);
        int largest= parent;
        if(comparator.compare(data.get(child),data.get(parent))>0){
            largest = child;
        }
        if(largest==parent){
            return;
        }else{
            T tmp = data.get(largest);
            data.set(largest,data.get(parent));
            data.set(parent,tmp);
            siftUp(data,begin,end,parent,comparator);
        }
    }



    public static int left(int begin,int i){
        return begin+2*(i-begin+1)-1;
    }
    public static int right(int begin,int i){
        return begin+2*(i-begin+1);
    }

    public static int parent(int begin,int i){
        return begin+(i-begin-1)/2;
    }

    public static void main(String[] args) {
        List<Integer> collect = Stream.of(new Integer[]{33, 6, 2, 13, 8, 40, 5}).collect(Collectors.toList());
        System.out.println(collect);
        make_heap(collect,0,collect.size(),(i,j)->i-j);
        System.out.println(collect);

        collect.add(100);
        System.out.println(collect);

        push_heap(collect,0,collect.size(),(i,j)->i-j);
        System.out.println(collect);


    }

}
