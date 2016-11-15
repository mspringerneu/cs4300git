import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.GLBuffers;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import sgraph.GroupNode;
import sgraph.INode;
import sgraph.IScenegraph;
import sgraph.IScenegraphRenderer;
import util.Light;
import util.Material;
import util.ObjectInstance;
import util.PolygonMesh;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;


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
    private List<ObjectInstance> meshObjects;
    private List<util.TextureImage> textures;
    private List<util.Material> materials;
    private List<Matrix4f> transforms;
    private List<util.Light> lights;
    //0-meshObjects.size()-1 are object coordinates, then world and then view
    private List<Integer> lightCoordinateSystems;
    private float trackballRadius;

    // main scenegraph
    private sgraph.IScenegraph<VertexAttrib> scenegraph;

    // secondary scenegraph for roller coaster tracks
    private sgraph.IScenegraph<VertexAttrib> slatScenegraph;

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
    private float numSlats = 360f;

    private Vector2f mousePos;
    private boolean mipmapped;

    class LightLocation {
        int ambient, diffuse, specular, position, spotDirection, spotAngle;

        public LightLocation() {
            ambient = diffuse = specular = position = spotDirection = spotAngle = -1;
        }
    }


    util.ShaderProgram program;
    util.ShaderLocationsVault shaderLocations;
    private int modelviewLocation, projectionLocation, normalmatrixLocation, texturematrixLocation;
    private int materialAmbientLocation, materialDiffuseLocation, materialSpecularLocation, materialShininessLocation;
    private int textureLocation;
    private List<LightLocation> lightLocations;
    private int numLightsLocation;


    public View()
    {
        projection = new Matrix4f();
        modelView = new Stack<Matrix4f>();
        trackballRadius = 300;
        trackballTransform = new Matrix4f();
        scenegraph = null;
    }

    public void initScenegraph(GLAutoDrawable gla,InputStream in1, InputStream in2) throws Exception
    {
        GL3 gl = gla.getGL().getGL3();

        if (scenegraph!=null)
            scenegraph.dispose();

        program.enable(gl);

        scenegraph = sgraph.SceneXMLReader.importScenegraph(in1,new VertexAttribProducer());

        sgraph.IScenegraphRenderer renderer = new sgraph.GL3ScenegraphRenderer();
        renderer.setContext(gla);
        Map<String,String> shaderVarsToVertexAttribs = new HashMap<String,String>();
        shaderVarsToVertexAttribs.put("vPosition","position");
        shaderVarsToVertexAttribs.put("vNormal","normal");
        shaderVarsToVertexAttribs.put("vTexCoord","texcoord");
        renderer.initShaderProgram(program,shaderVarsToVertexAttribs);
        scenegraph.setRenderer(renderer);
        scenegraph.giveTexturesMap();

        slatScenegraph = sgraph.SceneXMLReader.importScenegraph(in2,new VertexAttribProducer());

        sgraph.IScenegraphRenderer slatRenderer = new sgraph.GL3ScenegraphRenderer();
        slatRenderer.setContext(gla);
        Map<String,String> slatShaderVarsToVertexAttribs = new HashMap<String,String>();
        slatShaderVarsToVertexAttribs.put("vPosition","position");
        slatShaderVarsToVertexAttribs.put("vNormal","normal");
        slatShaderVarsToVertexAttribs.put("vTexCoord","texcoord");
        slatRenderer.initShaderProgram(program,slatShaderVarsToVertexAttribs);
        slatScenegraph.setRenderer(slatRenderer);
        slatScenegraph.giveTexturesMap();
        program.disable(gl);
    }

    private void initShaderVariables() {
        //get input variables that need to be given to the shader program
        projectionLocation = shaderLocations.getLocation("projection");
        modelviewLocation = shaderLocations.getLocation("modelview");
        normalmatrixLocation = shaderLocations.getLocation("normalmatrix");
        texturematrixLocation = shaderLocations.getLocation("texturematrix");
        materialAmbientLocation = shaderLocations.getLocation("material.ambient");
        materialDiffuseLocation = shaderLocations.getLocation("material.diffuse");
        materialSpecularLocation = shaderLocations.getLocation("material.specular");
        materialShininessLocation = shaderLocations.getLocation("material.shininess");

        textureLocation = shaderLocations.getLocation("image");

        numLightsLocation = shaderLocations.getLocation("numLights");
        for (int i = 0; i < lights.size(); i++) {
            LightLocation ll = new LightLocation();
            String name;

            name = "light[" + i + "]";
            ll.ambient = shaderLocations.getLocation(name + "" + ".ambient");
            ll.diffuse = shaderLocations.getLocation(name + ".diffuse");
            ll.specular = shaderLocations.getLocation(name + ".specular");
            ll.position = shaderLocations.getLocation(name + ".position");
            lightLocations.add(ll);
        }
    }

    public void initLights() {
        numLightsLocation = shaderLocations.getLocation("numLights");
        if (numLightsLocation<0)
            throw new IllegalArgumentException("No shader variable for \" numLights \"");
        //modelview currently represents world-to-view transformation
        //transform all lights so that they are in the view coordinate system too
        //before you send them to the shader.
        //that way everything is in one coordinate system (view) and the math will
        //be correct
        for (int i = 0; i < lights.size(); i++) {
            LightLocation ll = new LightLocation();
            String name;

            name = "light[" + i + "]";
            ll.ambient = shaderLocations.getLocation(name + "" + ".ambient");
            if (ll.ambient<0)
                throw new IllegalArgumentException("No shader variable for " + name + ".ambient \"");

            ll.diffuse = shaderLocations.getLocation(name + ".diffuse");
            if (ll.diffuse<0)
                throw new IllegalArgumentException("No shader variable for " + name + ".diffuse \"");

            ll.specular = shaderLocations.getLocation(name + ".specular");
            if (ll.specular<0)
                throw new IllegalArgumentException("No shader variable for " + name + ".specular \"");

            ll.position = shaderLocations.getLocation(name + ".position");
            if (ll.position<0)
                throw new IllegalArgumentException("No shader variable for " + name + ".position \"");

            ll.spotDirection = shaderLocations.getLocation(name + ".position");
            if (ll.spotDirection<0)
                throw new IllegalArgumentException("No shader variable for " + name + ".spotDirection \"");

            ll.spotAngle = shaderLocations.getLocation(name + ".position");
            if (ll.spotAngle<0)
                throw new IllegalArgumentException("No shader variable for " + name + ".spotAngle \"");

            lightLocations.add(ll);
        }
    }


    public void init(GLAutoDrawable gla) throws Exception
    {
        GL3 gl = gla.getGL().getGL3();


        //compile and make our shader program. Look at the ShaderProgram class for details on how this is done
        program = new util.ShaderProgram();

        program.createProgram(gl,"shaders/phong-multiple.vert","shaders/phong-multiple.frag");

        shaderLocations = program.getAllShaderVariables(gl);
        lightLocations = new ArrayList<LightLocation>();

        //get input variables that need to be given to the shader program
        projectionLocation = shaderLocations.getLocation("projection");
    }



    public void draw(GLAutoDrawable gla)
    {
        GL3 gl = gla.getGL().getGL3();

        gl.glClearColor(0,0,0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(gl.GL_DEPTH_TEST);

        FloatBuffer fb4 = Buffers.newDirectFloatBuffer(4);
        FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);

        program.enable(gl);

        while (!modelView.empty())
            modelView.pop();

        /*
         *In order to change the shape of this triangle, we can either move the vertex positions above, or "transform" them
         * We use a modelview matrix to store the transformations to be applied to our triangle.
         * Right now this matrix is identity, which means "no transformations"
         */
        modelView.push(new Matrix4f());
        modelView.peek().lookAt(new Vector3f(0,150,-1000),new Vector3f(0,0,0),new Vector3f(0,1,0))
                        .mul(trackballTransform);

        lights = scenegraph.getLights(modelView.peek());

        initShaderVariables();

        gl.glUniform1f(numLightsLocation, lights.size());
        //modelview currently represents world-to-view transformation
        //transform all lights so that they are in the view coordinate system too
        //before you send them to the shader.
        //that way everything is in one coordinate system (view) and the math will
        //be correct
        for (int i = 0; i < lights.size(); i++) {

            gl.glUniform3fv(lightLocations.get(i).ambient,1,lights.get(i).getAmbient().get(fb4));
            gl.glUniform3fv(lightLocations.get(i).diffuse,1,lights.get(i).getDiffuse().get(fb4));
            gl.glUniform3fv(lightLocations.get(i).specular,1,lights.get(i).getSpecular().get(fb4));
            gl.glUniform4fv(lightLocations.get(i).position,1, lights.get(i).getPosition().get(fb4));
            gl.glUniform4fv(lightLocations.get(i).spotDirection, 1,lights.get(i).getSpotDirection().get(fb4));
            gl.glUniform1f(lightLocations.get(i).spotAngle, lights.get(i).getSpotCutoff());
        }

    /*
     *Supply the shader with all the matrices it expects.
    */


        float slatIncrement = (float)360/numSlats;

        for (int i = 0; i < 360; i+= slatIncrement) {
            float atan = (float)Math.atan((double)((cartLength/2) / trackRadius));
            float angle = (float)Math.toRadians(i);


            Matrix4f orbit = new Matrix4f().mul(modelView.peek())
                    // move car up/down based on sine wave
                    .translate(0, (sineWaveMagnitude * (float)Math.sin(sineWaveFrequency * (double)angle)) - 20f, 0)
                    // progress car along circular orbit about y
                    .rotate(angle, 0, 1, 0)
                    // move car from origin along x axis by RADIUS units
                    .translate(trackRadius, 0, 0);
            modelView.push(orbit);
            slatScenegraph.draw(modelView);
            modelView.pop();
        }

        /*
        for (int i = 0; i < numberOfCars; i++) {
            float atan = (float)Math.atan((double)((cartLength/2) / trackRadius));
            float rot = (2 * i) - 1;
            float angle = (float)Math.toRadians(angleOfRotation1) - (atan * rot);


            Matrix4f orbit = new Matrix4f().mul(modelView.peek())
                    // move car up/down based on sine wave
                    .translate(0, sineWaveMagnitude * (float)Math.sin(sineWaveFrequency * (double)angle), 0)
                    // progress car along circular orbit about y
                    .rotate(angle, 0, 1, 0)
                    // rotate car about x depending on increasing/decreasing height
                    .rotate((float)Math.toRadians(cosWaveMagnitude * Math.cos(sineWaveFrequency * (double) angle)), 1, 0, 0)
                    // move car from origin along x axis by RADIUS units
                    .translate(trackRadius, 0, 0)
                    // rotate car 180 about Y so it faces the correct direction
                    .rotate((float)Math.toRadians(180), 0, 1, 0);
            modelView.push(orbit);
            angleOfRotation1 += carSpeed;
            sineWave += sineWaveFrequency;
            scenegraph.draw(modelView);
            modelView.pop();
        }
         */
        scenegraph.animate(angleOfRotation1);
        angleOfRotation1 += (carSpeed * 5);
        sineWave += sineWaveFrequency;
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

        gl.glUniformMatrix4fv(projectionLocation,1,false,projection.get(fb16));
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
