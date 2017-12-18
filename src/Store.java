import java.util.ArrayList;

public class Store<T> {

    private ArrayList<T> l = new ArrayList<>();
    private int freeElementHead = -1;

    public int add(T value) {
        int idx = freeElementHead;
        if( idx == -1 ){
            idx = l.size();
            l.add( null );
        } else {
            freeElementHead = (Integer)l.get(freeElementHead);
        }
        l.set( idx, value );
        return( idx );
    }

    public T get(int idx) {
        return l.get( idx );
    }

    public void delete( int idx ){
        l.set( idx, (T)Integer.valueOf(freeElementHead) );
        freeElementHead = idx;
    }
}
