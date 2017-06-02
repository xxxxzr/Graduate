import java.util.*;

/**
 * Created by dugang on 16/9/21.
 */
public class Model {
    Matrix wi_;
    Matrix wo_;
    int isz_;
    int osz_;
    int hsz_;
    //    int negpos = 0;
    float loss_ = 0.0f;
    int nexamples_ = 1;
    Vector hidden_;
    Vector output_;
    Vector grad_;
    Random rng = new Random();

    List<Integer> negatives = new ArrayList<>();
    int negpos;
    List<List<Integer>> paths = new ArrayList<>();
    List<List<Boolean>> codes = new ArrayList<>();
    List<Node> tree = new ArrayList<>();

    public Model(Matrix wi,
                 Matrix wo,
                 int seed) {
        wi_ = wi;
        wo_ = wo;
        isz_ = wi_.m;
        osz_ = wo_.m;
        hsz_ = Params.dim;
        negpos = 0;
        loss_ = 0.0f;
        nexamples_ = 1;
        this.hidden_ = new Vector(Params.dim);
        this.output_ = new Vector(wo.m);
        this.grad_ = new Vector(Params.dim);
        this.rng = new Random(seed);

    }

    private float toDouble(boolean label) {
        if (label) {
            return 1.0f;
        } else {
            return 0.0f;
        }
    }

    public float binaryLogistic(int target, boolean label, float lr) {
        float score = Util.sigmoid(wo_.dotRow(hidden_, target));
        float alpha = lr * (toDouble(label) - score);
        grad_.addRow(wo_, target, alpha);
        wo_.addRow(hidden_, target, alpha);
        if (label) {
            return -Util.log(score);
        } else {
            return -Util.log(1.0f - score);
        }
    }

    float negativeSampling(int target, float lr) {
        float loss = 0.0f;
        grad_.zero();
        for (int n = 0; n <= Params.neg; n++) {
            if (n == 0) {
                loss += binaryLogistic(target, true, lr);
            } else {
                loss += binaryLogistic(getNegative(target), false, lr);
            }
        }
        return loss;
    }

    float hierarchicalSoftmax(int target, float lr) {
        float loss = 0.0f;
        grad_.zero();
        List<Boolean> binaryCode = codes.get(target);
        List<Integer> pathToRoot = paths.get(target);

        for (int i = 0; i < pathToRoot.size(); i++) {
            loss += binaryLogistic(pathToRoot.get(i), binaryCode.get(i), lr);
        }
        return loss;
    }

    void computeOutputSoftmax() {
        output_.mul(wo_, hidden_);
        float max = output_.get(0), z = 0.0f;
        for (int i = 0; i < osz_; i++) {
            max = Math.max(output_.get(i), max);
        }
        for (int i = 0; i < osz_; i++) {
            output_.set(i, (float) Math.exp(output_.get(i) - max));
            z += output_.get(i);
        }
        for (int i = 0; i < osz_; i++) {
            output_.set(i, output_.get(i) / z);
        }
    }

    public float softmax(int target, float lr) {
        grad_.zero();
        computeOutputSoftmax();
        for (int i = 0; i < osz_; i++) {
            float label = (i == target) ? 1.0f : 0.0f;
            float alpha = lr * (label - output_.get(i));
            grad_.addRow(wo_, i, alpha);
            wo_.addRow(hidden_, i, alpha);
        }
        return -Util.log(output_.get(target));
    }

    public void computeHidden(final List<Integer> input) {
        hidden_.zero();
        for (Integer it : input) {
            hidden_.addRow(wi_, it);
        }
        hidden_.mul(1.0f / input.size());
    }


    Comparator<Pair> comparePairs = (pair1, pair2) -> {
        return Double.valueOf(pair1.getFirst() - pair2.getFirst()).intValue();
    };

    void predict(List<Integer> input, int k,
                 List<Pair> heap) {
        assert (k > 0);
//        heap.reserve(k + 1);
        computeHidden(input);
        if (Params.loss == Params.LOSS_HS) {
            dfs(k, 2 * osz_ - 2, 0.0f, heap);
        } else {
            findKBest(k, heap);
        }
        HeapMethod.sort_heap(heap, 0, heap.size(), comparePairs);
    }

    void findKBest(int k, List<Pair> heap) {
        computeOutputSoftmax();
        for (int i = 0; i < osz_; i++) {
            if (heap.size() == k && Util.log(output_.get(i)) < heap.get(0).getFirst()) {
                continue;
            }
            heap.add(new Pair(Util.log(output_.get(i)), i));
            HeapMethod.push_heap(heap, 0, heap.size(), comparePairs);
            if (heap.size() > k) {
                HeapMethod.pop_heap(heap, 0, heap.size(), comparePairs);
                heap.remove(heap.size() - 1);
            }
        }
    }

    void dfs(int k, int node, float score,
             List<Pair> heap) {
        if (heap.size() == k && score < heap.get(0).getFirst()) {
            return;
        }

        if (tree.get(node).left == -1 && tree.get(node).right == -1) {
            heap.add(new Pair(score, node));
            HeapMethod.push_heap(heap, 0, heap.size(), comparePairs);
            if (heap.size() > k) {
                HeapMethod.pop_heap(heap, 0, heap.size(), comparePairs);
                heap.remove(heap.size() - 1);
            }
            return;
        }

        float f = Util.sigmoid(wo_.dotRow(hidden_, node - osz_));
        dfs(k, tree.get(node).left, score + Util.log(1.0f - f), heap);
        dfs(k, tree.get(node).right, score + Util.log(f), heap);
    }

    void update(List<Integer> input, int target, float lr) {
        assert (target >= 0);
        assert (target < osz_);
        if (input.size() == 0) return;
        hidden_.zero();
        for (Integer it : input) {
            hidden_.addRow(wi_, it);
        }
        hidden_.mul(1.0f / input.size());

        if (Params.loss == Params.LOSS_NS) {
            loss_ += negativeSampling(target, lr);
        } else if (Params.loss == Params.LOSS_HS) {
            loss_ += hierarchicalSoftmax(target, lr);
        } else {
            loss_ += softmax(target, lr);
        }
        nexamples_ += 1;

        if (Params.model == Params.MODEL_SUP) {
            grad_.mul(1.0f / input.size());
        }
        for (Integer it : input) {
            wi_.addRow(grad_, it, 1.0f);

        }
    }

    public void setTargetCounts(List<Integer> counts) {
        assert (counts.size() == osz_);
        if (Params.loss == Params.LOSS_NS) {
            initTableNegatives(counts);
        }
        if (Params.loss == Params.LOSS_HS) {
            buildTree(counts);
        }
    }

    void initTableNegatives(final List<Integer> counts) {
        float z = 0.0f;
        for (int i = 0; i < counts.size(); i++) {
            z += Math.pow(counts.get(i), 0.5);
        }
        for (int i = 0; i < counts.size(); i++) {
            float c = (float)Math.pow(counts.get(i), 0.5);
            for (int j = 0; j < c * Params.NEGATIVE_TABLE_SIZE / z; j++) {
                negatives.add(i);
            }
        }
        Collections.shuffle(negatives, rng);
    }

    int getNegative(int target) {
        int negative;
        do {
            negative = negatives.get(negpos);
            negpos = (negpos + 1) % negatives.size();
        } while (target == negative);
        return negative;
    }

    void buildTree(List<Integer> counts) {
        for (int i = 0; i < 2 * osz_ - 1; i++) {
            Node node = new Node();
            node.parent = -1;
            node.left = -1;
            node.right = -1;
            node.count = Double.valueOf(1e15).longValue();
            node.binary = false;
            tree.add(node);
        }
        for (int i = 0; i < osz_; i++) {
            tree.get(i).count = counts.get(i);
        }
        int leaf = osz_ - 1;
        int node = osz_;
        for (int i = osz_; i < 2 * osz_ - 1; i++) {
            int[] mini = new int[2];
            for (int j = 0; j < 2; j++) {
                if (leaf >= 0 && tree.get(leaf).count < tree.get(node).count) {
                    mini[j] = leaf--;
                } else {
                    mini[j] = node++;
                }
            }
            tree.get(i).left = mini[0];
            tree.get(i).right = mini[1];
            tree.get(i).count = tree.get(mini[0]).count + tree.get(mini[1]).count;
            tree.get(mini[0]).parent = i;
            tree.get(mini[1]).parent = i;
            tree.get(mini[1]).binary = true;
        }
        for (int i = 0; i < osz_; i++) {
            List<Integer> path = new ArrayList<>();
            List<Boolean> code = new ArrayList<>();
            int j = i;
            while (tree.get(j).parent != -1) {
                path.add(tree.get(j).parent - osz_);
                code.add(tree.get(j).binary);
                j = tree.get(j).parent;
            }
            paths.add(path);
            codes.add(code);
        }
    }

    float getLoss() {
        return loss_ / nexamples_;
    }
}

    class Node {
        public int parent;
        public int left;
        public int right;
        public long count;
        public boolean binary;
    }