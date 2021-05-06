package graphdeal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Yuxuan Shi
 * @Date 11/11/2019
 * @Time 12:47 PM
 */

public class MinHeap<K extends CompareScore<K>> {

    //start from 1
    private ArrayList<K> heap;
    private Map<K, Integer> heapLoc;
    private int heapSize = 0;

    public MinHeap(){
        heap = new ArrayList<>();
        heap.add(null);
        //heapLoc = new HashMap<>();
        heapLoc = new HashMap<>();
    }

    //swap two heap element
    void swap(int i, int j) {
        K tmp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, tmp);
    }

    public void push(K ct) {
        if (!heapLoc.containsKey(ct)) {
            heap.add(ct);
            heapSize++;
            heapLoc.put(ct, heapSize);
            upModify(heapSize);
            return;
        }
        int loc = heapLoc.get(ct);
        if (loc < 1) return;
        //previous heap[loc] is smaller
        if (heap.get(loc).compareScore(ct) < 0) return;
        heap.set(loc, ct);
        upModify(loc);

    }

    public K poll() {
        if (heapSize == 0) {
            return null;
        }
        K ans = heap.get(1);
        heapLoc.remove(ans);
        //heapLoc.put(ans, 0);
        heapLoc.put(heap.get(heapSize), 1);
        swap(1, heapSize);
        heap.remove(heapSize);
        heapSize--;
        downModify(1);
        return ans;
    }


    public boolean isEmpty() {
        if (heapSize == 0) {
            return true;
        }
        return false;
    }

    void downModify(int loc) {
        int target = loc;
        int left = loc * 2;
        int right = loc * 2 + 1;
        if (left <= heapSize && heap.get(left).compareScore(heap.get(target)) < 0) {
            target = left;
        }
        if (right <= heapSize && heap.get(right).compareScore(heap.get(target)) < 0) {
            target = right;
        }
        if (target != loc) {
            heapLoc.put(heap.get(target), loc);
            heapLoc.put(heap.get(loc), target);
            swap(target, loc);
            downModify(target);
        }
    }

    void upModify(int loc) {
        if (loc > 1) {
            int target = loc / 2;
            if (heap.get(loc).compareScore(heap.get(target)) < 0) {
                heapLoc.put(heap.get(target), loc);
                heapLoc.put(heap.get(loc), target);
                swap(target, loc);
                upModify(target);
            }
        }
    }

    public void clear(){
        heap.clear();
        heapLoc.clear();
    }
}
