import com.chinamobile.cmdi.securityteam.tools.BufferedControl;
import com.chinamobile.cmdi.securityteam.tools.MyCounter;
import com.chinamobile.cmdi.securityteam.tools.StreamUtil;
import com.chinamobile.cmdi.securityteam.tools.Utils;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

import static fasttext.Params.dim;
import static fasttext.Params.model;
import static fasttext.Params.t;

/**
 * Created by Voyager on 2016/9/22.
 */
public class FastText {
    Dictionary dict_=new Dictionary();
    Matrix input_ = new Matrix();
    Matrix output_ = new Matrix();
    Model model_;
    Random rng = new Random();
    int tokenCount;
    long start = 0;
    final int CLOCKS_PER_SEC = 1000;

    public void getVector(Vector vec, final String word) {
        final List<Integer> ngrams = dict_.getNgrams(word);
        vec.zero();
        for (int it = ngrams.get(0); it != ngrams.get(ngrams.size()); ++it) {
            vec.addRow(input_, it);
        }
        if (ngrams.size() > 0) {
            vec.mul(1.0f / ngrams.size());
        }
    }

    void saveVectors() throws IOException{
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(Params.output + ".vec"));
        oos.writeObject(dict_.nwords());
        oos.writeObject(Params.dim);
        oos.writeObject(Params.dim);
        for (int i = 0; i < dict_.nwords(); i++) {
            String word = dict_.getWord(i);
            Vector vec = new Vector(Params.dim);
            getVector(vec, word);
            oos.writeObject(word);
            oos.writeObject(vec);
        }
        oos.close();
    }

    void saveModel() throws IOException{
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(Params.output + ".bin"));
        dict_.save(oos);
//        input_.save(oos);
//        output_.save(oos);
        oos.writeObject(input_);
        oos.writeObject(output_);
        oos.close();
    }

    void loadModel(String filename) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(Params.output + ".bin"));
        dict_.load(ois);
//        input_.load(ois);
//        output_.load(ois);
        input_ = (Matrix)ois.readObject();
        output_ = (Matrix)ois.readObject();
        ois.close();
        model_ = new Model(input_, output_, 0);
        if (Params.model == Params.MODEL_SUP) {
            model_.setTargetCounts(dict_.getCounts(Params.LABEL));
        } else {
            model_.setTargetCounts(dict_.getCounts(Params.WORD));
        }
    }

    long pre = System.currentTimeMillis();
    public void printInfo(float progress, float loss) {
        float t = (System.currentTimeMillis() - start) / CLOCKS_PER_SEC;//TODO

        float wst = tokenCount / t;
        float lr = (float) Params.lr * (1.0f - progress);
        int eta = (int) (t / progress * (1 - progress) / Params.thread);
        int etah = eta / 3600;
        int etam = (eta - etah * 3600) / 60;

        long l = System.currentTimeMillis();
        if(l-pre>1000) {
            DecimalFormat df = new DecimalFormat("0.0");
            System.out.println(                        Thread.currentThread().getName()+": Progress: " + df.format(100 * progress) + "%");
//            System.out.println();
//            System.out.print("  words/sec/thread: " + wst);
//            System.out.println();
//            System.out.print("  lr: " + lr);
//            System.out.println();
//            System.out.print("  loss: " + loss);
//            System.out.println();
//            System.out.print("  eta: " + etah + "h" + etam + "m ");
            pre=l;
        }
    }

    public void supervised(Model model, float lr, final List<Integer> line, final List<Integer> labels) {
        if (labels.size() == 0 || line.size() == 0){ return;}
        Random random = new Random(System.currentTimeMillis());
        int i = random.nextInt(labels.size());
        model.update(line, labels.get(i), lr);
    }

    public void cbow(Model model, float lr, final List<Integer> line) {
        List<Integer> bow = new ArrayList<>();
        Random random = new Random(System.currentTimeMillis());
        for (int w = 0; w < line.size(); w++) {
            int boundary = random.nextInt(Params.ws - 1) + 1;
            bow.clear();
            for (int c = -boundary; c <= boundary; c++) {
                if (c != 0 && w + c >= 0 && w + c < line.size()) {
                    final List<Integer> ngrams = dict_.getNgrams(line.get(w + c));
                    for (int i = 0; i < ngrams.size(); i++) {
                        bow.add(ngrams.get(i));
                    }
                }
            }
            model.update(bow, line.get(w), lr);
        }
    }

    public void skipgram(Model model, float lr,
                         final List<Integer> line) {
        Random random = new Random(System.currentTimeMillis());
        for (int w = 0; w < line.size(); w++) {
            int boundary = random.nextInt(Params.ws - 1) + 1;
            final List<Integer> ngrams = dict_.getNgrams(line.get(w));
            for (int c = -boundary; c <= boundary; c++) {
                if (c != 0 && w + c >= 0 && w + c < line.size()) {
                    model.update(ngrams, line.get(w + c), lr);
                }
            }
        }
    }

    public void test(final String filename, int k) {
        int nexamples = 0, nlabels = 0;
        float precision = 0.0f;
        try{
            BufferedReader  br =new BufferedReader(new FileReader(filename));
            String s;
            MyCounter<String> counter = new MyCounter<>();
            while ((s=br.readLine())!=null){
                ArrayList<Integer> line =new ArrayList<>();
                ArrayList<Integer> labels   =   new ArrayList<>();
                dict_.getLine(s,line,labels,this.rng);
                dict_.addNgrams(line,Params.wordNgrams);
                counter.count(dict_.getLabel(labels.get(0)));

                if(labels.size()>0 && line.size()>0){
                    List<Pair> predictions  =   new ArrayList<>();
                    model_.predict(line,k,predictions);
                    for (int i = 0; i < predictions.size(); i++) {
                        if(labels.contains(predictions.get(i).getSecond())){
                            counter.count(dict_.getLabel(labels.get(0))+"正确");
                            precision+=1d;
                        }
                    }
                    counter.count(dict_.getLabel(labels.get(0))+"查到");
                    nexamples++;
                    nlabels+=labels.size();
                }
            }
            counter.getCounter().keySet().stream().filter(label -> !label.endsWith("正确") && !label.endsWith("查到")).forEach(label ->
                    System.out.println(label.replaceAll(Params.label, "") + " 总计：" + counter.getCounts(label)
                    + " 查准率:" + (counter.getCounts(label + "正确") * 1.0 / counter.getCounts(label + "查到"))
                    + " 查全率:" + (counter.getCounts(label + "正确") * 1.0 / counter.getCounts(label))
            ));
            br.close();
        }catch (IOException e) {
            e.printStackTrace();
        }

        System.out.print("P@" + k +": " + precision / (k * nexamples));
        System.out.println();
        System.out.print("R@" + k +": " + precision / nlabels);
        System.out.println();
        System.out.print("Number of examples: " + nexamples);
    }

    public void predict(final String filename, int k, boolean print_prob) {

        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String s;
            while ((s=br.readLine())!=null){
//                System.out.println(s);
                ArrayList<Integer> line =   new ArrayList<>();
                ArrayList<Integer> labels   =   new ArrayList<>();
                dict_.getLine(s,line,labels,model_.rng);
                dict_.addNgrams(line,Params.wordNgrams);
                if(line.isEmpty()){
                    System.out.println("n/a");
                    continue;
                }
                ArrayList<Pair> predictions     =   new ArrayList<>();
                model_.predict(line,k,predictions);
                    for (int i = 0; i < predictions.size(); i++) {
                        if (predictions.get(i) != predictions.get(0)) {
                            System.out.print(" ");
                        }
                        System.out.print(dict_.getLabel(predictions.get(i).getSecond()));
                        if (print_prob) {
                            System.out.print(" " + Math.exp(predictions.get(i).getFirst()));
                        }
                    }
                    System.out.print(s);
                    System.out.println();
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void wordVectors() {
        String word = new String();
        Vector vec = new Vector(Params.dim);
        Scanner scanner = new Scanner(System.in);
        while (word == scanner.nextLine()) {
            getVector(vec, word);
            System.out.println(word + " " + vec);
        }
    }

    public void textVectors() {
        ArrayList<Integer> line =   new ArrayList<>();
        ArrayList<Integer> labels   =   new ArrayList<>();
        Vector vec  =   new Vector(Params.dim);
        Scanner scanner =   new Scanner(System.in);
        String s;
        while ((s=scanner.next()).length()>0){
            try {
                dict_.getLine(s,line,labels,model_.rng);
                dict_.addNgrams(line,Params.wordNgrams);
                vec.zero();
                for (int it:line) {
                    vec.addRow(input_,it);
                }
                if (!line.isEmpty()){
                    vec.mul(1f/line.size());
                }
                System.out.println(vec);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void printVectors() {
        if (Params.model == Params.MODEL_SUP) {
            textVectors();
        } else {
            wordVectors();
        }
    }
    int localTokenCount = 0;

    public void trainThread(int threadId,ArrayList<String> lines) {
        Model model = new Model(input_, output_, threadId);
        if (Params.model == Params.MODEL_SUP) {
            model.setTargetCounts(dict_.getCounts(Params.LABEL));
        } else {
            model.setTargetCounts(dict_.getCounts(Params.WORD));
        }
        int ntokens = dict_.ntokens();
        ArrayList<Integer> line = new ArrayList<Integer>();
        ArrayList<Integer> labels = new ArrayList<Integer>();
        lines.stream().skip(threadId * lines.size() / Params.thread).forEach(currentLine -> {

            while (tokenCount < Params.epoch * ntokens) {
                float progress = Float.valueOf(tokenCount) / (Params.epoch * ntokens);
                float lr = (float) Params.lr * (1.0f - progress);
                try {
                    localTokenCount += dict_.getLine(currentLine, line, labels, model.rng);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (Params.model == Params.MODEL_SUP) {
                    dict_.addNgrams(line, Params.wordNgrams);
                    supervised(model, lr, line, labels);
                } else if (Params.model == Params.MODEL_CBOW) {
                    cbow(model, lr, line);
                } else if (Params.model == Params.MODEL_SG) {
                    skipgram(model, lr, line);
                }
                if (localTokenCount > Params.lrUpdateRate) {
                    tokenCount += localTokenCount;
                    localTokenCount = 0;
                    if (threadId == 0 && Params.verbose > 1) {
                        printInfo(progress, model.getLoss());
                    }
                }
            }
            if (threadId == 0) {
                printInfo(1.0f, model.getLoss());
            }
        });

//        System.out.println("thread:" + threadId + ":completed!");

    }

    public void train(String filename) throws IOException {
        dict_=new Dictionary();
        try {
            dict_.readFromFile(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        input_ = new Matrix(dict_.nwords()+Params.bucket,Params.dim);

        if(Params.model==Params.MODEL_SUP){
             output_=new Matrix(dict_.nlabels(),Params.dim);
        }else{
             output_=new Matrix(dict_.nwords(),Params.dim);
        }
        input_.uniform(1f/Params.dim);
        output_.zero();
        start=System.currentTimeMillis();

        BufferedControl<String> bcc =new BufferedControl<String>("ff",200000) {
            @Override
            public void flush(ArrayList<String> arrayList) {
                localTokenCount=0;
                tokenCount  =   0;

                ExecutorService exec = Executors.newFixedThreadPool(Params.thread);
                CyclicBarrier barrier  =    new CyclicBarrier(Params.thread+1);
                for (int i = 0; i < Params.thread; i++) {
                    final int id =i;
                    exec.execute(() -> {
                        trainThread(id,arrayList);
                        try {
                            barrier.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (BrokenBarrierException e) {
                            e.printStackTrace();
                        }
                    });

                }
                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                exec.shutdownNow();
            }
        };
        StreamUtil.fileReader(filename,reader->reader.lines().forEach(bcc::append));
        bcc.close();
        System.out.println("ok");
        model_ = new Model(input_,output_,0);
        Utils.tick("save");
        saveModel();
        Utils.tick("save");

        if (Params.model != Params.MODEL_SUP) {
            saveVectors();//TODO
        }
    }

    public void test(int k,String modelname,String testname) throws IOException, ClassNotFoundException {
        FastText fasttext   =   new FastText();
        fasttext.loadModel(modelname);//TODO
        fasttext.test(testname,k);
        System.exit(0);
    }

    public void predict(int k, String modelname,String predictname) throws IOException, ClassNotFoundException {
        boolean print_prob = true;
        FastText fasttext   =   new FastText();
        fasttext.loadModel(modelname);
        fasttext.predict(predictname, k, print_prob);
        System.exit(0);
    }

    public void printVectors(int argc,String modelname) throws IOException, ClassNotFoundException {
        FastText fasttext   =   new FastText();
        fasttext.loadModel(modelname);

        fasttext.printVectors();
        System.exit(0);
    }

    public static void main(String args[]) throws IOException, ClassNotFoundException {
        Params.setSuperivsor();
        FastText    fasttest    =   new FastText();
        fasttest.train("/Users/dugang/develop/docker/program/fasttext/data/trainshort.txt");
//        fasttest.predict(1,"output.bin","/Users/dugang/develop/docker/program/fasttext/data/trainshort.txt");
//        fasttest.test(1,"output.bin","/Users/dugang/develop/docker/program/fasttext/data/train.txt");
        fasttest.test(1,"output.bin","/Users/dugang/develop/docker/program/fasttext/data/trainshort.txt");

//        if (command == "skipgram" || command == "cbow" || command == "supervised") {
//            Params.model    =   Params.MODEL_CBOW;
//            fasttest.train(filename);
//        } else if (command == "test") {
//            fasttest.test(k,modelname,testname);
//        } else if (command == "print-vectors") {
//            fasttest.printVectors();
//        } else if (command == "predict" || command == "predict-prob" ) {
//            fasttest.predict(k,true);
//        } else {
//
//        }
    }




}




