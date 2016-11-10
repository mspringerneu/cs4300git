package cs4300.assignment1;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ashesh on 9/17/2016.
 */
public abstract class AbstractTowersOfHanoi implements TowersOfHanoi{
    protected List<Move> solution;

    public AbstractTowersOfHanoi() {
        solution = new LinkedList<Move>();
    }

    public void export(OutputStream out) {
        PrintStream outStream = new PrintStream(out);

        outStream.println(solution.size());

        for (Move m:solution) {
            outStream.println(m.getFrom() + " " + m.getTo());
        }
        outStream.close();
    }

    public List<Integer> getMoves() {
        List<Integer> exportedMoves = new ArrayList<>();
        for (Move m:solution) {
            exportedMoves.add(m.getFrom());
            exportedMoves.add(m.getTo());
        }
        return exportedMoves;
    }

    protected static class Move {
        private final int from;
        private final int to;

        public Move(int from,int to) {
            this.from = from;
            this.to = to;
        }

        public int getFrom() {
            return from;
        }

        public int getTo() {
            return to;
        }
    }


}
