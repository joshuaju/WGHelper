package expense;

/**
 * Created by Joshua Jungen on 05.03.2016.
 */
public enum PaidBy {
    Mario(1),
    Joshua(2);

    private int id;

    PaidBy(int id){
        this.id = id;
    }

    public int getId(){
        return id;
    }
}
