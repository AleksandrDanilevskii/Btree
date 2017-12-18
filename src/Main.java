import static java.lang.System.*;
public class Main {

    static public void main(String args[]) {
        Btree t = new Btree();

        int n = 100;
        int n2 = 1000;
        //заполняем его
        for (int i = 0; i < n; i++) {
            long rand = Math.round(n2 * Math.random());
            //out.println(rand);
            t.add(rand, i);
            //t.print();
            //out.println();
        }
        t.print();

        for (int i = 0; i < n; i++) {
            long key = Math.round(n2 * Math.random());
            Long v = t.find(key);
            if (v == null) {
                out.printf("\nno nothing for %d", key);
            } else {
                out.printf("\n%d -> %d", key, v);
                t.delete(key);
            }
        }
        t.print();

    }
}
