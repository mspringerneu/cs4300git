import cs4300.assignment1.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;



import javax.swing.*;

/**
 * A main class to test towers of hanoi
 */
public class TOH {

    public static void main(String []args) {
        cs4300.assignment1.TowersOfHanoi solver;

        solver = new cs4300.assignment1.NonRecTowersOfHanoi();
        int problemSize = -1;

        if (args.length > 0) {
            try {
                problemSize = (int)Integer.parseInt(args[0]);
            }
            catch (NumberFormatException e) {
                System.out.println(e);
            }
        }
        else {
            problemSize = 7;
        }
        final int disks = problemSize;
        String filename = "toh-"+problemSize+".txt";
        solver.solve(problemSize);
        OutputStream out = null;
        try {
            out = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            System.out.println("Could not write to file "+filename);
        }
        solver.export(out);
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(disks);
            }
        });
    }

    private static void createAndShowGUI(int problemSize) {
        JFrame frame = new JOGLFrame("Assignment 1: Towers Of Hanoi", problemSize);
        frame.setVisible(true);
    }
}
