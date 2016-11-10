package cs4300.assignment1;

/**
 * Created by mspringer on 9/26/16.
 */
public class TowersOfHanoiDisk {
    public int id;
    private int[] moves;
    private int currentmove;

    public TowersOfHanoiDisk(int tower) {
        this.id= id;
        this.moves = new int[(int)Math.pow(2.0, (double)id)];
        this.currentmove = 0;
        if (id % 2 == 0) {
            int offset = 2;
            for (int i = 0; i < moves.length; i ++) {
                moves[i] = offset % 3;
                offset--;
            }
        }
        else {
            int offset = 1;
            for (int i = 0; i < moves.length; i ++) {
                moves[i] = offset % 3;
                offset++;
            }
        }
    }

    public int getNextMove() {
        int nextmove;
        if (this.currentmove < moves.length) {
            nextmove = moves[currentmove];
            currentmove++;
            return nextmove;
        }
        else {
            throw new ArrayIndexOutOfBoundsException("Disk " + id + " has already made all of its moves");
        }
    }
}
