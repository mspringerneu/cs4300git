package cs4300.assignment1;

import java.util.Scanner;
import java.util.Stack;

/**
 * This class implements a nonrecursive solution to the Towers of Hanoi problem
 */
public class NonRecTowersOfHanoi extends AbstractTowersOfHanoi {

  public NonRecTowersOfHanoi() {
    super();
  }

  @Override
  public void solve(int numDisks) {
    Stack<String> moveStack = new Stack<String>();
    String message;
    Scanner temp;
    int disks, from, to, inter;
    String command;


    message = "p "+numDisks+ " 1 3 2";
    moveStack.push(message);
    while (!moveStack.isEmpty()) {
      message = moveStack.peek();
      moveStack.pop();

      if (message.charAt(0) == 'p') {
        temp = new Scanner(message);
        command = temp.next();
        disks = temp.nextInt();
        from = temp.nextInt();
        to = temp.nextInt();
        inter = temp.nextInt();

        if (disks > 0) {
          message = "p " + (disks - 1) + " " + inter + " " + to + " " + from;
          moveStack.push(message);

          message = "c " + from + " " + to;
          moveStack.push(message);

          message = "p " + (disks - 1) + " " + from + " " + inter + " " + to;
          moveStack.push(message);

        }
      } else if (message.charAt(0) == 'c') {
        temp = new Scanner(message);
        command = temp.next();
        from = temp.nextInt();
        to = temp.nextInt();
        solution.add(new Move(from, to));
      }
    }
  }
}
