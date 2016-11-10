import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;

import javax.swing.*;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;

/**
 * Created by ashesh on 9/18/2015.
 */
public class JOGLFrame extends JFrame {
  private View view;
  private TextRenderer textRenderer;
  private GLCanvas canvas;

  public JOGLFrame(String title) {
    //routine JFrame setting stuff
    super(title);
    setSize(400, 400); //this opens a 400x400 window
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //when X is pressed, close program

    //Our View class is the actual driver of the OpenGL stuff
    view = new View();

    GLProfile glp = GLProfile.getGL2GL3();
    GLCapabilities caps = new GLCapabilities(glp);
    canvas = new GLCanvas(caps);

    add(canvas);


    canvas.addGLEventListener(new GLEventListener() {
      @Override
      public void init(GLAutoDrawable glAutoDrawable) { //called the first time this canvas is created. Do your initialization here
        try {
          view.init(glAutoDrawable);
          textRenderer = new TextRenderer(new Font("SansSerif", Font.PLAIN, 18), true, false);
          glAutoDrawable.getGL().setSwapInterval(1);
        } catch (Exception e) {
          JOptionPane.showMessageDialog(JOGLFrame.this, e.getMessage(), "Error while loading", JOptionPane.ERROR_MESSAGE);
        }
      }

      @Override
      public void dispose(GLAutoDrawable glAutoDrawable) { //called when the canvas is destroyed.
        view.dispose(glAutoDrawable);
      }

      @Override
      public void display(GLAutoDrawable glAutoDrawable) { //called every time this window must be redraw
        view.draw(glAutoDrawable);
        //textRenderer.beginRendering(canvas.getWidth(), canvas.getHeight());
        // optionally set the color
        //textRenderer.setColor(1.0f, 1.0f, 0.0f, 1.0f);
        String text = "Frame rate: " + canvas.getAnimator().getLastFPS();
        //textRenderer.draw(text, 10, canvas.getHeight() - 50);
        //textRenderer.endRendering();
      }

      @Override
      public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) { //called every time this canvas is resized
        view.reshape(glAutoDrawable, x, y, width, height);
        repaint(); //refresh window
      }
    });

    //Add an animator to the canvas
    AnimatorBase animator = new FPSAnimator(canvas, 300);
    animator.setUpdateFPSFrames(100, null);
    animator.start();
  }


}
