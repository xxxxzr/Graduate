import com.chinamobile.cmdi.securityteam.tools.MyCounter;
import com.chinamobile.cmdi.securityteam.tools.StreamUtil;
import com.chinamobile.cmdi.securityteam.tools.Utils;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Created by dugang on 16/9/20.
 */
public class Dictionary implements Serializable{

    public static final String EOS = "</s>";
    public static final String BOW = "<";
    public static final String EOW = ">";


    int[] word2int_;
    ArrayList<entry> words_ = new ArrayList<>();
    ArrayList<Double> pdiscard_ = new ArrayList<>();
    int size_;
    int nwords_;
    int nlabels_;
    int ntokens_;

    public Dictionary() {
//        args_ = args;
        size_ = 0;
        nwords_ = 0;
        nlabels_ = 0;
        ntokens_ = 0;
        word2int_ = new int[Params.MAX_VOCAB_SIZE];
        for (int i = 0; i < Params.MAX_VOCAB_SIZE; i++) {
            word2int_[i] = -1;
        }
    }

    //根据词，找其编号
    public int find(String w) {
        int h = hash(w) % Params.MAX_VOCAB_SIZE;
        while (word2int_[h] != -1 && !words_.get(word2int_[h]).getWord().equals(w)) {
            h = (h + 1) % Params.MAX_VOCAB_SIZE;
        }
        return h;
    }

    //将词加入词典
    void add(String w) {
        int h = find(w);
        ntokens_++;
        if (word2int_[h] == -1) {
            entry e = new entry();
            e.setWord(w);
            e.setCount(1);
            e.setType(w.contains(Params.label) ? Params.LABEL : Params.WORD);
            words_.add(e);
            word2int_[h] = size_++;
        } else {
            words_.get(word2int_[h]).increaseCount();
        }

    }

    int nwords() {
        return nwords_;
    }

    int nlabels() {
        return nlabels_;
    }

    int ntokens() {
        return ntokens_;
    }

    //获取指定编号的ngram
    ArrayList<Integer> getNgrams(int i) {
        assert (i >= 0);
        assert (i < nwords_);
        return words_.get(i).getSubwords();
    }

    ArrayList<Integer> getNgrams(String word) {
        ArrayList<Integer> ngrams = new ArrayList<>();
        int i = getId(word);
        if (i >= 0) {
            ngrams = words_.get(i).getSubwords();
        } else {
            computeNgrams(BOW + word + EOW, ngrams);
        }
        return ngrams;
    }

    public boolean discard(int id, double rand) {
        assert (id >= 0);
        assert (id < nwords_);
        if (Params.model == Params.MODEL_SUP){
            return false;
        }
        return rand > pdiscard_.get(id);
    }

    public int getId(String w) {
        int h = find(w);
        return word2int_[h];
    }

    public short getType(int id) {
        assert (id >= 0);
        assert (id < size_);
        return words_.get(id).getType();
    }

    String getWord(int id) {
        assert (id >= 0);
        assert (id < size_);
        return words_.get(id).getWord();
    }

    int hash(String str) {
        int h = 216613626;
        for (int i = 0; i < str.length(); i++) {
            h = h ^ (int) str.charAt(i);
            h = h * 16777619;
        }
        return h>>>1;
    }

    public void computeNgrams(String word, ArrayList<Integer> ngrams) {
        for (int i = 0; i < word.length(); i++) {
            StringBuffer ngramBuf = new StringBuffer();
            if ((word.charAt(i) & 0xC0) == 0x80){
                continue;
            }
            for (int j = i, n = 1; j < word.length() && n <= Params.maxn; n++) {
                ngramBuf.append(word.charAt(j));
                j++;
                while (j < word.length() && (word.charAt(i) & 0xC0) == 0x80) {
                    ngramBuf.append(word.charAt(j));
                    j++;
                }
                if (n >= Params.minn) {
                    String ngram = ngramBuf.toString();
                    int h = hash(ngram) % Params.bucket;
//                    System.out.println(ngram+"\t"+h);

                    ngrams.add(nwords_ + h);
                }
            }
        }
    }


    public void initNgrams() {
        for (int i = 0; i < size_; i++) {
            if(words_.get(i).getType()==Params.WORD) {
                String word = BOW + words_.get(i).getWord() + EOW;
                words_.get(i).getSubwords().add(i);
                computeNgrams(word, words_.get(i).getSubwords());
            }
        }
    }
    public int getLine(String line,ArrayList<Integer> words, ArrayList<Integer> labels,Random r) throws IOException {
        int ntokens = 0;
        words.clear();
        labels.clear();
        String[] split = line.split("\\s");
        for(String word:split){
            int wid = getId(word);
            if (wid < 0){
                continue;
            }
            short type = getType(wid);
            ntokens++;
            if (type == Params.WORD && !discard(wid, r.nextDouble())) {
                words.add(wid);
            }
            if (type == Params.LABEL) {
                labels.add(wid - nwords_);
            }
            if (words.size() > Params.MAX_LINE_SIZE && Params.model != Params.MODEL_SUP) break;
        }

        return ntokens;
    }

    void readFromFile(String file) throws IOException {
        MyCounter<String> counter = new MyCounter<>();
        counter.setCount("threshold",1);
        StreamUtil.fileReader(file,reader-> reader.lines().forEach(line->{
            String[] split = line.split("\\s");
            for(String word:split){
                add(word);
                if (ntokens_ % 1000000 == 0) {
                    System.out.println("Read" + ntokens_ / 1000000 + "M words");
                }
                if (size_ > 0.75 * Params.MAX_VOCAB_SIZE) {
                    counter.count("threshold");
                    threshold(counter.getCounts("threshold"));
                }
            }
        }));

        System.out.println("Read" + ntokens_ / 1000000 + "M words");
        threshold(Params.minCount);
        initTableDiscard();
        initNgrams();
        System.out.println("Number of words:  " + nwords_);
        System.out.println("Number of labels: " + nlabels_);
        if (size_ == 0) {
            System.out.println("Empty vocabulary. Try a smaller -minCount value.");
            System.exit(1);
        }

    }

    public void threshold(int t) {
        //标签排在最前面，频次大的排在前面
        Collections.sort(words_, (e1, e2) -> e1.getType() != e2.getType() ? e1.getType() - e2.getType() : e2.getCount() - e1.getCount());
        //删除频次小于t的词
        System.out.println(words_.size());
        List<entry> collect = words_.stream().filter(e -> e.getType() == Params.LABEL || e.getCount() >= t).collect(Collectors.toList());
        System.out.println(collect.size());
        words_.clear();
        words_.addAll(collect);
        size_ = 0;
        nwords_ = 0;
        nlabels_ = 0;
        for (int i = 0; i < Params.MAX_VOCAB_SIZE; i++) {
            word2int_[i] = -1;
        }
        for (entry it : words_) {
            int h = find(it.getWord());
            word2int_[h] = size_;
            size_++;
            if (it.getType() == Params.WORD) {
                nwords_++;
            }
            if (it.getType() == Params.LABEL){
                nlabels_++;
            }
        }
    }

    void initTableDiscard() {
        pdiscard_ = new ArrayList<>(size_);
        for (int i = 0; i < size_; i++) {
            double f = 1.0 * words_.get(i).getCount() / ntokens_;
            pdiscard_.add(Math.sqrt(Params.t / f) + Params.t / f);
        }
    }

    public List<Integer> getCounts(short type) {
        List<Integer> counts = new ArrayList<>();
        for (entry w : words_) {
            if (w.getType() == type) {
                counts.add(w.getCount());
            }
        }
        return counts;
    }

    public void addNgrams(ArrayList<Integer> line, int n) {
        int line_size = line.size();
        for (int i = 0; i < line_size; i++) {
            int h = line.get(i);
            for (int j = i + 1; j < line_size && j < i + n; j++) {
                h = h * 116049371 + line.get(j);
                line.add(nwords_ + (h % Params.bucket));
            }
        }
    }



    public String getLabel(int lid) {
        assert (lid >= 0);
        assert (lid < nlabels_);
        return words_.get(lid + nwords_).getWord();
    }

    public void save(ObjectOutputStream oos) throws IOException {
                oos.writeObject(size_);
                oos.writeObject(nwords_);
                oos.writeObject(nlabels_);
                oos.writeObject(ntokens_);
                oos.writeObject(words_);
    }

    public void load(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        words_.clear();
        for (int i = 0; i < Params.MAX_VOCAB_SIZE; i++) {
            word2int_[i] = -1;
        }

        size_ = (int) ois.readObject();
        nwords_ = (int) ois.readObject();
        nlabels_ = (int) ois.readObject();
        ntokens_ = (int) ois.readObject();
        words_ = (ArrayList<entry>) ois.readObject();
        for (int i = 0; i < size_; i++) {
            word2int_[find(words_.get(i).getWord())] = i;
        }
        initTableDiscard();
        initNgrams();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Dictionary dict = new Dictionary();
        dict.readFromFile("/Users/dugang/develop/docker/program/fasttext/data/trainNotCutword.txt");
        for(int i=0;i<10;i++) {
            System.out.println(dict.getWord(i));;
        }

    }
}

class entry implements Serializable{
    private String word;
    private int count;
    private short type;
    private ArrayList<Integer> subwords = new ArrayList<>();

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public ArrayList<Integer> getSubwords() {
        return subwords;
    }

    public void setSubwords(ArrayList<Integer> subwords) {
        this.subwords = subwords;
    }

    public void increaseCount() {
        count++;
    }

    public String toString(){
        return word+"\t"+count+"\t"+type+"\t"+subwords;
    }
};
