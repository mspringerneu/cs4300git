package cs4300.assignment1;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements a recursive solution to the Towers of Hanoi problem
 */
public class RecTowersOfHanoi extends AbstractTowersOfHanoi {

    /**
     * Compute a solution to the n-disk Towers of Hanoi problem
     * @param numDisks the number of disks, i.e. the size of the problem
     */
    public void solve(int numDisks) {
        solution.clear();
        solve(numDisks,1,3,2);
    }



    private void solve(int numDisks,int from,int to,int inter) {
        if (numDisks>0) {
            for (int i = 0; i < numDisks; i++) {

            }
            solve(numDisks-1,from,inter,to);
            solution.add(new Move(from,to));
            solve(numDisks-1,inter,to,from);
        }
    }




}
