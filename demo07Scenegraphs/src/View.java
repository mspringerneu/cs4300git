import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.GLBuffers;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;


/**
 * Created by ashesh on 9/18/2015.
 *
 * The View class is the "controller" of all our OpenGL stuff. It cleanly encapsulates all our OpenGL functionality from the rest of Java GUI, managed
 * by the JOGLFrame class.
 */
public class View
{
    private int WINDOW_WIDTH,WINDOW_HEIGHT;
    private Stack<Matrix4f> modelView;
    private Matrix4f projection,trackballTransform;
    private float trackballRadius;
    private Vector2f mousePos;


    private util.ShaderProgram program;
    private util.ShaderLocationsVault shaderLocations;
    private int projectionLocation;
    private sgraph.IScenegraph<VertexAttrib> scenegraph;
    private float angleOfRotation1 = 0;
    private float angleOfRotation2 = 180;
    private float sineWave = 0f;
    private float sineWaveFrequency = 10f;
    private float sineWaveMagnitude = 100f;
    private float cosWaveMagnitude = 45;
    private float carSpeed = 0.1f;
    private int numberOfCars = 5;
    private float cartLength = 120;
    private float trackRadius = 800f;


    public View()
    {
        projection = new Matrix4f();
        modelView = new Stack<Matrix4f>();
        trackballRadius = 300;
        trackballTransform = new Matrix4f();
        scenegraph = null;
    }

    public void initScenegraph(GLAutoDrawable gla,InputStream in) throws Exception
    {
        GL3 gl = gla.getGL().getGL3();

        if (scenegraph!=null)
            scenegraph.dispose();

        program.enable(gl);

        scenegraph = sgraph.SceneXMLReader.importScenegraph(in,new VertexAttribProducer());

        sgraph.IScenegraphRenderer renderer = new sgraph.GL3ScenegraphRenderer();
        renderer.setContext(gla);
        Map<String,String> shaderVarsToVertexAttribs = new HashMap<String,String>();
        shaderVarsToVertexAttribs.put("vPosition","position");
        shaderVarsToVertexAttribs.put("vNormal","normal");
        shaderVarsToVertexAttribs.put("vTexCoord","texcoord");
        renderer.initShaderProgram(program,shaderVarsToVertexAttribs);
        scenegraph.setRenderer(renderer);
        program.disable(gl);
    }

    public void init(GLAutoDrawable gla) throws Exception
    {
        GL3 gl = gla.getGL().getGL3();




        //compile and make our shader program. Look at the ShaderProgram class for details on how this is done
        program = new util.ShaderProgram();

        program.createProgram(gl,"shaders/triangles.vert","shaders/triangles.frag");

        shaderLocations = program.getAllShaderVariables(gl);

        //get input variables that need to be given to the shader program
        projectionLocation = shaderLocations.getLocation("projection");
    }



    public void draw(GLAutoDrawable gla)
    {
        GL3 gl = gla.getGL().getGL3();

        gl.glClearColor(0,0,0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);

        program.enable(gl);

        while (!modelView.empty())
            modelView.pop();

        /*
         *In order to change the shape of this triangle, we can either move the vertex positions above, or "transform" them
         * We use a modelview matrix to store the transformations to be applied to our triangle.
         * Right now this matrix is identity, which means "no transformations"
         */
        modelView.push(new Matrix4f());
        modelView.peek().lookAt(new Vector3f(0,0,-100),new Vector3f(0,0,0),new Vector3f(0,1,0))
                        .mul(trackballTransform);

        scenegraph.draw(modelView);


        /*
        Matrix4f orbit1 = new Matrix4f().mul(modelView.peek()).rotate((float)Math.toRadians(angleOfRotation1), 0, 1, 0)
                .translate(500, (float)Math.sin(Math.toRadians(angleOfRotation1)), 0)
                .rotate((float)Math.toRadians(180), 0, 1, 0);

        Matrix4f orbit2 = new Matrix4f().mul(modelView.peek()).rotate((float)Math.toRadians(angleOfRotation2), 0, 1, 0)
                .translate(500, (float)Math.sin(Math.toRadians(angleOfRotation2)), 0)
                .rotate((float)Math.toRadians(180), 0, 1, 0);

        modelView.push(orbit1);
        modelView.push(orbit2);
        angleOfRotation1++;
        angleOfRotation2++;
        */

    /*
     *Supply the shader with all the matrices it expects.
    */
        FloatBuffer fb = Buffers.newDirectFloatBuffer(16);
        gl.glUniformMatrix4fv(projectionLocation,1,false,projection.get(fb));
        //return;


        //gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_LINE); //OUTLINES

        /*
        scenegraph.draw(modelView);
        modelView.pop();
        scenegraph.draw(modelView);
        */
    /*
     *OpenGL batch-processes all its OpenGL commands.
          *  *The next command asks OpenGL to "empty" its batch of issued commands, i.e. draw
     *
     *This a non-blocking function. That is, it will signal OpenGL to draw, but won't wait for it to
     *finish drawing.
     *
     *If you would like OpenGL to start drawing and wait until it is done, call glFinish() instead.
     */
        gl.glFlush();

        program.disable(gl);



    }

    public void mousePressed(int x,int y)
    {
        mousePos = new Vector2f(x,y);
    }

    public void mouseReleased(int x,int y)
    {
        System.out.println("Released");
    }

    public void mouseDragged(int x,int y)
    {
        Vector2f newM = new Vector2f(x,y);

        Vector2f delta = new Vector2f(newM.x-mousePos.x,newM.y-mousePos.y);
        mousePos = new Vector2f(newM);

        trackballTransform = new Matrix4f().rotate(delta.x/trackballRadius,0,1,0)
                                           .rotate(delta.y/trackballRadius,1,0,0)
                                           .mul(trackballTransform);
    }

    public void reshape(GLAutoDrawable gla,int x,int y,int width,int height)
    {
        GL gl = gla.getGL();
        WINDOW_WIDTH = width;
        WINDOW_HEIGHT = height;
        gl.glViewport(0, 0, width, height);

        projection = new Matrix4f().perspective((float)Math.toRadians(120.0f),(float)width/height,0.1f,10000.0f);
       // proj = new Matrix4f().ortho(-400,400,-400,400,0.1f,10000.0f);

    }

    public void dispose(GLAutoDrawable gla)
    {
        GL3 gl = gla.getGL().getGL3();

    }



}
