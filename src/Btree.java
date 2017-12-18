import java.util.ArrayList;
import java.util.Arrays;
import static java.lang.System.*;

public class Btree {
    static public int N = 20; // t = 10; any node should contain from t to 2t keys (t+1 to 2t+1 refs)

    private Store<Long> values = new Store<>();
    private Store<Node> nodes = new Store<>(); //узлы

    int root = nodes.add(new Node( true ));

    static class Node {
        long[] keys = new long[N]; //кдючи
        int[] refs = new int[N + 1]; //указатели

        int keysCount = 0; //счетчик ключей
        boolean isLeaf;

        public Node( boolean isLeaf ) {
            this.isLeaf = isLeaf;
            refs[keysCount] = -1; // last ref is ref to sibling node// последний ref ссылается на узел-брата
        }

        public int find(long key) {
            // returns index of key which is greater or equal to the parameter or 'keysCount'
            //возвращает индекс ключа, который больше или равен параметру или keysCount
            int idx = Arrays.binarySearch(keys, 0, keysCount, key);
            if (idx < 0) {
                idx = -idx - 1;
            }
            return idx;
        }

        public void extend(int idx) {   //увеличение
            arraycopy(keys, idx, keys, idx + 1, keysCount - idx);
            arraycopy(refs, idx, refs, idx + 1, (keysCount - idx) + 1);
            keysCount++;
        }
        public void insertValRef(int idx, long key, int valRef) {
            extend(idx);
            keys[idx] = key;
            refs[idx] = valRef;
        }
        private void insertNodeRef(int idx, long key, int nodeRef) {
            extend( idx );
            keys[idx] = key;
            refs[idx + 1] = nodeRef;
        }
    }

    public Long add(long key, long val) {
        ArrayList<Integer> path = new ArrayList<>();

        //remember the path//запоминаем муть
        for (int nodeIdx = root;;) {
            path.add(nodeIdx);
            Node node = nodes.get(nodeIdx);
            if (node.isLeaf)
                break;
            nodeIdx = node.refs[node.find(key)];
        }


        final int valref = values.add(val);
        Node leaf = nodes.get(path.get(path.size() - 1));
        int idx = Arrays.binarySearch(leaf.keys, 0, leaf.keysCount, key);
        if (idx >= 0) { //we found the same key
            long old = values.get(leaf.refs[idx]);
            leaf.refs[idx] = valref;
            return old;
        }
        idx = -idx - 1;
        if (leaf.keysCount < leaf.keys.length){ //there is a space in the leaf//в листе есть место
            leaf.insertValRef(idx, key, valref);
            return null;
        }
        // need to split the leaf//нужно разбить лист
        Node newLeaf = new Node( true );
        arraycopy(leaf.keys, leaf.keys.length/2, newLeaf.keys, 0, leaf.keys.length/2);
        arraycopy(leaf.refs, leaf.keys.length/2, newLeaf.refs, 0, leaf.keys.length/2 + 1);
        newLeaf.keysCount = leaf.keysCount/2;
        leaf.keysCount -= newLeaf.keysCount;
        int newLeafIdx = nodes.add(newLeaf);
        leaf.refs[leaf.keysCount] = newLeafIdx; //b+ tree
        if (idx <= leaf.keysCount) {
            leaf.insertValRef(idx, key, valref);
        } else {
            newLeaf.insertValRef(idx - leaf.keysCount, key, valref);
        }

        // вставляем ключ и новый блок в parent
        key = leaf.keys[leaf.keysCount - 1];
        int noderef = newLeafIdx;
        int pathIdx = path.size()-1;

        while( pathIdx > 0 ) {
            Node node = nodes.get(path.get(--pathIdx));
            idx = node.find(key);
            if (node.keysCount < node.keys.length) {
                node.insertNodeRef(idx, key, noderef);
                return null;
            }
            // если idx не указывает в самый конец запомним последние ключ и ссылку и вставим нужные значения (затерев последние)
            long lastKey = key;
            int lastRef = noderef;
            if (idx < node.keysCount) {
                lastKey = node.keys[node.keysCount - 1];
                lastRef = node.refs[node.keysCount];
                node.keysCount--;
                node.insertNodeRef(idx, key, noderef);
            }
            // разрезать узел надвое, вставить ключ значение и перейти к его паренту (если есть)
            Node newNode = new Node( false );
            arraycopy(node.keys, node.keys.length / 2 + 1, newNode.keys, 0, node.keys.length / 2 - 1);
            arraycopy(node.refs, node.keys.length / 2 + 1, newNode.refs, 0, node.keys.length / 2);
            newNode.keysCount = node.keysCount / 2 - 1;
            node.keysCount -= newNode.keysCount;
            newNode.insertNodeRef(newNode.keysCount, lastKey, lastRef);
            key = node.keys[--node.keysCount];
            noderef = nodes.add(newNode);
        }
        // надо создавать новый корень
        Node newRoot = new Node( false );
        newRoot.refs[ 0 ] = root;
        newRoot.insertNodeRef( 0, key, noderef );
        root = nodes.add( newRoot );
        return null;
    }

    public Long find(long key) {
        Node node = nodes.get(root);
        for (;;) {
            int idx = Arrays.binarySearch(node.keys, 0, node.keysCount, key);
            if (node.isLeaf) {
                if (idx >= 0) {
                    return (values.get(node.refs[idx]));
                }
                return (null);
            }
            if (idx < 0) {
                idx = -idx - 1;
            }
            node = nodes.get(node.refs[idx]);
        }
    }

    public void delete(long key) {
        //запоминаем пусть
        ArrayList<Integer> path = new ArrayList<>();
        for (int nodeIdx = root; ; ) {
            path.add(nodeIdx);
            Node node = nodes.get(nodeIdx);
            if (node.isLeaf)
                break;
            nodeIdx = node.refs[node.find(key)];
        }

        int idxInPath = path.size() - 1;
        Node node = nodes.get(path.get(idxInPath));
        int idx = node.find(key);
        if (node.keys[idx] != key) {
            // ключ не найден, ничего не делать
            return;
        }
        for (; ; ) {
            // удалить запись
            values.delete(node.refs[idx]);
            arraycopy(node.keys, idx + 1, node.keys, idx, node.keysCount - (idx + 1));
            arraycopy(node.refs, idx + 1, node.refs, idx, node.keysCount - idx);
            node.keysCount--;
            // проверьте, действительно ли количество записей
            if (node.keysCount * 2 >= N || idxInPath == 0) {
                // узел действителен, никаких действий не требуется
                return;
            }
            // необходимо присоединить или сбалансировать узлы
            Node parent = nodes.get(path.get(idxInPath - 1));
            int pidx = parent.find(key);
            if (pidx == parent.keysCount) pidx--;
            // родитель имеет обоих детей: pidx и pidx + 1
            // проверить, можем ли мы присоединиться к детям
            int refsCount = nodes.get(parent.refs[pidx]).keysCount + (node.isLeaf ? 0 : 1);
            if (refsCount + (nodes.get(parent.refs[pidx + 1]).keysCount + 1) > N + 1) {
                // невозможно объединить узлы
                balanceNodes(parent, pidx);
                return;
            }
            // присоединиться к узлам
            joinNodes(parent, pidx);
            // подготовить удаление записи от родителя
            idxInPath--;
            parent.refs[pidx + 1] = parent.refs[pidx];
            node = parent;
            idx = pidx;
        }
    }

    private void balanceNodes(Node parent, int pidx) {
        assert (pidx < parent.keysCount);
        Node ch1 = nodes.get(parent.refs[pidx]), ch2 = nodes.get(parent.refs[pidx + 1]);
        if (ch1.isLeaf) {
            // собирать все записи
            long[] keys = new long[ch1.keysCount + ch2.keysCount];
            int[] refs = new int[keys.length + 1];
            arraycopy(ch1.keys, 0, keys, 0, ch1.keysCount);
            arraycopy(ch2.keys, 0, keys, ch1.keysCount, ch2.keysCount);
            arraycopy(ch1.refs, 0, refs, 0, ch1.keysCount);
            arraycopy(ch2.refs, 0, refs, ch1.keysCount, ch2.keysCount + 1);
            // распространять записи на узлы
            ch1.keysCount = keys.length / 2;
            ch2.keysCount = keys.length - ch1.keysCount;
            arraycopy(keys, 0, ch1.keys, 0, ch1.keysCount);
            arraycopy(refs, 0, ch1.refs, 0, ch1.keysCount);
            ch1.refs[ch1.keysCount] = parent.refs[pidx + 1];
            arraycopy(keys, ch1.keysCount, ch2.keys, 0, ch2.keysCount);
            arraycopy(refs, ch1.keysCount, ch2.refs, 0, ch2.keysCount + 1);
            // изменить ключ в родительском
            parent.keys[pidx] = ch1.keys[ch1.keysCount - 1];
        } else {
            // собирать все записи
            long[] keys = new long[ch1.keysCount + 1 + ch2.keysCount];
            int[] refs = new int[keys.length + 1];
            arraycopy(ch1.keys, 0, keys, 0, ch1.keysCount);
            keys[ch1.keysCount] = parent.keys[pidx];
            arraycopy(ch2.keys, 0, keys, ch1.keysCount + 1, ch2.keysCount);
            arraycopy(ch1.refs, 0, refs, 0, ch1.keysCount + 1);
            arraycopy(ch2.refs, 0, refs, ch1.keysCount + 1, ch2.keysCount + 1);
            // распространять записи на узлы
            ch1.keysCount = keys.length / 2;
            ch2.keysCount = keys.length - ch1.keysCount;
            arraycopy(keys, 0, ch1.keys, 0, ch1.keysCount);
            arraycopy(refs, 0, ch1.refs, 0, ch1.keysCount + 1);
            parent.keys[pidx] = keys[ch1.keysCount]; // change key in parent
            arraycopy(keys, ch1.keysCount + 1, ch2.keys, 0, ch2.keysCount);
            arraycopy(refs, ch1.keysCount + 1, ch2.refs, 0, ch2.keysCount + 1);
        }
    }

    private void joinNodes(Node parent, int pidx) {
        assert (pidx < parent.keysCount);
        Node ch1 = nodes.get(parent.refs[pidx]), ch2 = nodes.get(parent.refs[pidx + 1]);
        arraycopy(ch2.refs, 0, ch1.refs, ch1.keysCount + (ch1.isLeaf ? 0 : 1), ch2.keysCount + 1);
        if (!ch1.isLeaf)
            ch1.keys[ch1.keysCount++] = parent.keys[pidx];
        arraycopy(ch2.keys, 0, ch1.keys, ch1.keysCount, ch2.keysCount);
        ch1.keysCount += ch2.keysCount;
        nodes.delete(parent.refs[pidx + 1]);
    }

    public void print() {
        int ref = root;
        int countLeaves = 0;
        Node node;
        while( !(node = nodes.get( ref )).isLeaf )
            ref = node.refs[ 0 ];
        for(;;){
            countLeaves++;
            out.printf("\n[%d]:", node.keysCount);
            for( int i = 0; i < node.keysCount; i++) out.printf( " %d(%d)", node.keys[ i ], values.get(node.refs[ i ]) );
            if( node.refs[ node.keysCount ] == -1 )
                break;
            node = nodes.get( node.refs[ node.keysCount ] );

        }
        out.printf("\nnumber of leaves is %d", countLeaves);
    }
}