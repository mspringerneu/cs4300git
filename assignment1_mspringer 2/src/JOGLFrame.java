import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import javax.swing.*;

/**
 * Created by ashesh on 9/18/2015.
 */
public class JOGLFrame extends JFrame {
    private View view;
    private int disks;

    public JOGLFrame(String title, int disks) {
        //routine JFrame setting stuff
        super(title);
        this.disks = disks;
        setSize(400, 400); //this opens a 400x400 window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //when X is pressed, close program

        //Our View class is the actual driver of the OpenGL stuff
        view = new View(disks);

        GLProfile glp = GLProfile.getGL2GL3();
        GLCapabilities caps = new GLCapabilities(glp);
        GLCanvas canvas = new GLCanvas(caps);

        add(canvas);

        canvas.addGLEventListener(new GLEventListener() {
            @Override
            public void init(GLAutoDrawable glAutoDrawable) { //called the first time this canvas is created. Do your initialization here
                try {
                    view.init(glAutoDrawable);
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
            }

            @Override
            public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) { //called every time this canvas is resized
                view.reshape(glAutoDrawable, x, y, width, height);
                repaint(); //refresh window
            }
        });
    }


}
