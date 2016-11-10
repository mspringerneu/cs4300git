import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.awt.TextRenderer;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

/**
 * Created by ashesh on 9/18/2015.
 */
public class JOGLFrame extends JFrame {
  private View view;
  private int mouseX, mouseY;
  private boolean mousePressed;
  private final GLCanvas canvas;
  private TextRenderer textRenderer;

  public JOGLFrame(String title) {
    //routine JFrame setting stuff
    super(title);
    setSize(600, 600); //this opens a 400x400 window
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //when X is pressed, close program

    //Our View class is the actual driver of the OpenGL stuff
    view = new View();

    GLProfile glp = GLProfile.getDefault();
    GLCapabilities caps = new GLCapabilities(glp);
    canvas = new GLCanvas(caps);

    add(canvas);

    EventListener listener = new EventListener();

    canvas.addGLEventListener(listener);

    canvas.addMouseListener(listener);
    canvas.addMouseMotionListener(listener);
    canvas.addMouseWheelListener(listener);
    canvas.addKeyListener(listener);
    canvas.requestFocus();
  }

  class EventListener extends MouseAdapter implements GLEventListener, KeyListener {
    @Override
    public void init(GLAutoDrawable glAutoDrawable) { //called the first time this canvas is created. Do your initialization here
      try {
        view.init(glAutoDrawable);
        textRenderer = new TextRenderer(new Font("SansSerif", Font.PLAIN, 18), true, false);
      } catch (Exception e) {
        JOptionPane.showMessageDialog(JOGLFrame.this, e.getMessage(), "Error while loading", JOptionPane.ERROR_MESSAGE);
      }
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) { //called when the canvas is destroyed.
      view.dispose(glAutoDrawable);
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) { //called every time this window must be redrawn

      view.draw(glAutoDrawable);
      textRenderer.beginRendering(canvas.getWidth(), canvas.getHeight());
      // optionally set the color
      textRenderer.setColor(1.0f, 1.0f, 0.0f, 1.0f);
      textRenderer.draw(view.getFrameInfoString(), 10, canvas.getHeight() - 50);
      textRenderer.draw(view.getIterationInfoString(), 10, canvas.getHeight() - 80);
      textRenderer.endRendering();
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) { //called every time this canvas is resized
      view.reshape(glAutoDrawable, x, y, width, height);
      repaint(); //refresh window
    }


    @Override
    public void mousePressed(MouseEvent e) {
      mouseX = e.getX();
      mouseY = canvas.getHeight() - e.getY();

    }

    @Override
    public void mouseDragged(MouseEvent e) {
      view.translate(e.getX() - mouseX, canvas.getHeight() - e.getY() - mouseY);
      mouseX = e.getX();
      mouseY = canvas.getHeight() - e.getY();
      canvas.repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
      if (e.getWheelRotation() > 0)
        view.zoomIn();
      else
        view.zoomOut();
      canvas.repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
      switch (e.getKeyCode()) {
        case KeyEvent.VK_PLUS:
        case KeyEvent.VK_EQUALS:
          view.increaseMaxIterations();
          canvas.repaint();
          break;
        case KeyEvent.VK_MINUS:
        case KeyEvent.VK_UNDERSCORE:
          view.decreaseMaxIterations();
          canvas.repaint();
          break;
        case KeyEvent.VK_UP:
          view.zoomIn();
          canvas.repaint();
          break;
        case KeyEvent.VK_DOWN:
          view.zoomOut();
          canvas.repaint();
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
  }


}
