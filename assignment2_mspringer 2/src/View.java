import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;

import util.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;


/**
 * Created by ashesh on 9/18/2015.
 *
 * The View class is the "controller" of all our OpenGL stuff. It cleanly
 * encapsulates all our OpenGL functionality from the rest of Java GUI, managed
 * by the JOGLFrame class.
 */
public class View {
    private int WINDOW_WIDTH,WINDOW_HEIGHT;
    private Matrix4f proj,modelView, planetTransform, orbitTransform, moonTransform, moonOrbitTransform, boxTransform, trackballTransform, lookAt;
    private Stack<Matrix4f> modelViews;
    private util.ObjectInstance planetMeshObject;
    private util.ObjectInstance orbitMeshObject;
    private util.ObjectInstance boxMeshObject;
    private util.Material material;
    private ArrayList<util.ObjectInstance> meshObjects;
    private ArrayList<util.Material> materials;
    private ArrayList<Planet> solarSystem;
    private int SLICES = 50;
    private float BOUNDING_BOX_SCALAR = 1850;


    private util.ShaderProgram program;
    float angleOfRotation;
    private ShaderLocationsVault shaderLocations;




    public View() {
        proj = new Matrix4f();
        proj.identity();

        modelView = new Matrix4f();
        modelView.identity();

        planetTransform = new Matrix4f();
        planetTransform.identity();

        orbitTransform = new Matrix4f();
        orbitTransform.identity();

        moonTransform = new Matrix4f();
        moonTransform.identity();

        moonOrbitTransform = new Matrix4f();
        moonOrbitTransform.identity();

        boxTransform = new Matrix4f();
        boxTransform.identity();

        trackballTransform = new Matrix4f();
        trackballTransform.identity();

        lookAt = new Matrix4f().lookAt(new Vector3f(0, 0, -3000), new Vector3f(0,
                0, 0), new Vector3f(0, 1, 0));

        modelViews = new Stack<Matrix4f>();

        meshObjects = new ArrayList<ObjectInstance>();

        materials = new ArrayList<Material>();

        angleOfRotation = 0;
    }

    private void initObjects(GL3 gl) throws FileNotFoundException
    {
        /////////////////////////////////////////////////
        //
        //       Initialize Planet Mesh Object (sphere)
        //
        /////////////////////////////////////////////////

        util.PolygonMesh planetMesh;

        InputStream in1, in2;

        in1 = new FileInputStream("models/sphere.obj");

        planetMesh = util.ObjImporter.importFile(new VertexAttribProducer(),in1,true);

        Map<String, String> planetShaderToVertexAttribute = new HashMap<String, String>();

        //currently there is only one per-vertex attribute: position
        planetShaderToVertexAttribute.put("vPosition", "position");


        planetMeshObject = new util.ObjectInstance(gl,
                program,
                shaderLocations,
                planetShaderToVertexAttribute,
                planetMesh,new
                String(""));

        Vector4f planetMin = planetMesh.getMinimumBounds();
        Vector4f planetMax = planetMesh.getMaximumBounds();

        /////////////////////////////////////////////////
        //
        //       Initialize Bounding Box Mesh Object (cube)
        //
        /////////////////////////////////////////////////

        //  Specified vertices

        List<Vector4f> positions = new ArrayList<Vector4f>();

        // bottom square
        positions.add(new Vector4f(-0.5f, -0.5f, -0.5f, 1f));
        positions.add(new Vector4f(0.5f, -0.5f, -0.5f, 1f));
        positions.add(new Vector4f(0.5f, 0.5f, -0.5f, 1f));
        positions.add(new Vector4f(-0.5f, 0.5f, -0.5f, 1f));

        // top square
        positions.add(new Vector4f(-0.5f, -0.5f, 0.5f, 1f));
        positions.add(new Vector4f(0.5f, -0.5f, 0.5f, 1f));
        positions.add(new Vector4f(0.5f, 0.5f, 0.5f, 1f));
        positions.add(new Vector4f(-0.5f, 0.5f, 0.5f, 1f));

        // we add a second attribute to each vertex: color
        // note that the shader variable has been changed to "in" as compared
        // to HellJOGL because color is now a per-vertex attribute

        List<Vector4f> colors = new ArrayList<Vector4f>();
        colors.add(new Vector4f(1f, 1f, 1f, 1f)); //white

        //set up vertex attributes (in this case we have only position and color)
        List<IVertexData> vertexData = new ArrayList<IVertexData>();
        VertexAttribWithColorProducer producer = new VertexAttribWithColorProducer();
        for (int j = 0; j < positions.size(); j++) {
            IVertexData v = producer.produce();
            v.setData("position", new float[]{positions.get(j).x,
                    positions.get(j).y,
                    positions.get(j).z,
                    positions.get(j).w});
            // all orbit vertices should be white
            v.setData("color", new float[]{colors.get(0).x,
                    colors.get(0).y,
                    colors.get(0).z,
                    colors.get(0).w});
            vertexData.add(v);
        }

        List<Integer> indices = new ArrayList<Integer>();
        // bottom square
        indices.add(0);
        indices.add(1);
        indices.add(1);
        indices.add(2);
        indices.add(2);
        indices.add(3);
        indices.add(3);
        indices.add(0);

        // top square
        indices.add(4);
        indices.add(5);
        indices.add(5);
        indices.add(6);
        indices.add(6);
        indices.add(7);
        indices.add(7);
        indices.add(4);

        // connecting lines
        indices.add(0);
        indices.add(4);
        indices.add(1);
        indices.add(5);
        indices.add(2);
        indices.add(6);
        indices.add(3);
        indices.add(7);
        //now we create a polygon mesh object
        util.PolygonMesh boxMesh;

        boxMesh = new PolygonMesh();


        boxMesh.setVertexData(vertexData);
        boxMesh.setPrimitives(indices);

        boxMesh.setPrimitiveType(GL.GL_LINES);
        boxMesh.setPrimitiveSize(2);

        /*
        now we create an ObjectInstance for it
        The ObjectInstance encapsulates a lot of the
         OpenGL-specific code to draw this object
         */

        /* so in the mesh, we have some attributes for each vertex. In the shader
        we have variables for each vertex attribute. We have to provide a mapping
        between attribute name in the mesh and corresponding shader variable name.
        This will allow us to use PolygonMesh with any shader program, without
        assuming that the attribute names in the mesh and the names of shader variables
        will be the same.

        We create such a shader variable -> vertex attribute mapping now
         */
        Map<String, String> boxShaderToVertexAttribute = new HashMap<String, String>();

        //currently there are two per-vertex attributes: position and color
        boxShaderToVertexAttribute.put("vPosition", "position");
        boxShaderToVertexAttribute.put("vColor", "color");
        boxMeshObject = new ObjectInstance(gl, program, shaderLocations, boxShaderToVertexAttribute, boxMesh, "lines");

        // .obj file
        /*
        util.PolygonMesh boxMesh;

        in2 = new FileInputStream("models/box.obj");

        boxMesh = util.ObjImporter.importFile(new VertexAttribProducer(),in2,true);

        Map<String, String> boxShaderToVertexAttribute = new HashMap<String, String>();

        //currently there is only one per-vertex attribute: position
        boxShaderToVertexAttribute.put("vPosition", "position");


        boxMeshObject = new util.ObjectInstance(gl,
                program,
                shaderLocations,
                planetShaderToVertexAttribute,
                boxMesh,new
                String(""));

        Vector4f boxMin = planetMesh.getMinimumBounds();
        Vector4f boxMax = planetMesh.getMaximumBounds();
        */

        /////////////////////////////////////////////////
        //
        //       Initialize Orbit Mesh Object (circle)
        //
        /////////////////////////////////////////////////

        List<Vector4f> orbitPositions = new ArrayList<Vector4f>();

        for (float k = 0; k < 360; k += (360f / SLICES)) {
            orbitPositions.add(new Vector4f(1f * (float) Math.cos(Math.toRadians(k)), 1f * (float) Math.sin(Math.toRadians(k)), 0f, 1.0f));
        }

        // we add a second attribute to each vertex: color
        // note that the shader variable has been changed to "in" as compared
        // to HellJOGL because color is now a per-vertex attribute

        List<Vector4f> orbitColors = new ArrayList<Vector4f>();
        orbitColors.add(new Vector4f(1f, 1f, 1f, 1f)); //white

        //set up vertex attributes (in this case we have only position and color)
        List<IVertexData> orbitVertexData = new ArrayList<IVertexData>();
        VertexAttribWithColorProducer orbitProducer = new VertexAttribWithColorProducer();
        List<Integer> orbitIndices = new ArrayList<Integer>();
        for (int j = 0; j < orbitPositions.size(); j++) {
            IVertexData v = orbitProducer.produce();
            v.setData("position", new float[]{orbitPositions.get(j).x,
                    orbitPositions.get(j).y,
                    orbitPositions.get(j).z,
                    orbitPositions.get(j).w});
            // all orbit vertices should be white
            v.setData("color", new float[]{orbitColors.get(0).x,
                    orbitColors.get(0).y,
                    orbitColors.get(0).z,
                    orbitColors.get(0).w});
            orbitVertexData.add(v);
            orbitIndices.add(j);
        }

        //now we create a polygon mesh object
        util.PolygonMesh orbitMesh;

        orbitMesh = new PolygonMesh();


        orbitMesh.setVertexData(orbitVertexData);
        orbitMesh.setPrimitives(orbitIndices);

        orbitMesh.setPrimitiveType(GL.GL_LINE_LOOP);
        orbitMesh.setPrimitiveSize(2);

        /*
        now we create an ObjectInstance for it
        The ObjectInstance encapsulates a lot of the
         OpenGL-specific code to draw this object
         */

        /* so in the mesh, we have some attributes for each vertex. In the shader
        we have variables for each vertex attribute. We have to provide a mapping
        between attribute name in the mesh and corresponding shader variable name.
        This will allow us to use PolygonMesh with any shader program, without
        assuming that the attribute names in the mesh and the names of shader variables
        will be the same.

        We create such a shader variable -> vertex attribute mapping now
         */
        Map<String, String> orbitShaderToVertexAttribute = new HashMap<String, String>();

        //currently there are two per-vertex attributes: position and color
        orbitShaderToVertexAttribute.put("vPosition", "position");
        orbitShaderToVertexAttribute.put("vColor", "color");
        orbitMeshObject = new ObjectInstance(gl, program, shaderLocations, orbitShaderToVertexAttribute, orbitMesh, "line loop");

    }

    public void init(GLAutoDrawable gla) throws Exception {
        //initialize planets
        solarSystem = new ArrayList<>();
        solarSystem.add(new Planet("Sun"));
        solarSystem.add(new Planet("Mercury"));
        solarSystem.add(new Planet("Venus"));
        solarSystem.add(new Planet("Earth"));
        solarSystem.add(new Planet("Jupiter"));

        GL3 gl = (GL3) gla.getGL().getGL3();


        //compile and make our shader program. Look at the ShaderProgram class for details on how this is done
        program = new ShaderProgram();
        program.createProgram(gl, "shaders/default.vert", "shaders/default.frag");

        shaderLocations = program.getAllShaderVariables(gl);

        initObjects(gl);


    }


    public void draw(GLAutoDrawable gla) {
        GL3 gl = gla.getGL().getGL3();
        FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);
        FloatBuffer fb4 = Buffers.newDirectFloatBuffer(4);

        //set the background color to be black
        gl.glClearColor(0, 0, 0, 1);
        //clear the background
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL.GL_DEPTH_TEST);
        //enable the shader program
        program.enable(gl);
        /*
        modelView = new Matrix4f().lookAt(new Vector3f(0, 2500, 0), new Vector3f(0,
                0, 0), new Vector3f(0, 0, 1));
                */
        modelView = new Matrix4f().lookAt(new Vector3f(0, 0, -2500), new Vector3f(0,
                0, 0), new Vector3f(0, 1, 0)).mul(trackballTransform);

        modelViews.push(modelView);
        angleOfRotation = angleOfRotation + 0.1f;

        for (int i = 0; i < solarSystem.size(); i++) {

            /////////////////////////////////////////////////
            //
            //       Draw planet spheres
            //
            /////////////////////////////////////////////////

            planetTransform = new Matrix4f().mul(modelViews.peek());
            Planet p = solarSystem.get(i);
            //material = materials.get(i);
            //modelView = new Matrix4f().mul(modelViews.peek());

            Matrix4f scale = new Matrix4f().scale(p.getRadius(), p.getRadius(), p.getRadius());
            Matrix4f translate = new Matrix4f().translate(p.getOrbitRadius(), 0, 0);
            Matrix4f advancePosition = new Matrix4f().rotate(p.getSpeed() * (float) Math.toRadians(angleOfRotation), 0, 0, 1);
            Matrix4f rotatePhi = new Matrix4f().rotate(p.getPhi(), 0, 1, 0);
            Matrix4f rotateTheta = new Matrix4f().rotate(p.getTheta(), 0, 0, 1);

            //transform = modelView.mul(rotateTheta).mul(rotatePhi).mul(advancePosition).mul(translate).mul(scale);


            planetTransform = new Matrix4f().mul(modelViews.peek())
                    .mul(new Matrix4f().rotate(p.getTheta(), 0, 0, 1))
                    .mul(new Matrix4f().rotate(p.getPhi(), 0, 1, 0))
                    .mul(new Matrix4f().rotate(p.getSpeed() * (float) Math.toRadians(angleOfRotation), 0, 0, 1))
                    .mul(new Matrix4f().translate(p.getOrbitRadius(), 0, 0))
                    .mul(new Matrix4f().scale(p.getRadius(), p.getRadius(), p.getRadius()));

            orbitTransform = new Matrix4f().mul(modelViews.peek())
                    .mul(new Matrix4f().rotate(p.getTheta(), 0, 0, 1))
                    .mul(new Matrix4f().rotate(p.getPhi(), 0, 1, 0))
                    .mul(new Matrix4f().scale(p.getOrbitRadius(), p.getOrbitRadius(), p.getOrbitRadius()));

            modelView = new Matrix4f().mul(planetTransform);


            util.Material mat = new util.Material();

            //System.out.println("Planet " + p.name + " has color: " + p.getColor());
            mat.setAmbient(p.getColor());
            mat.setDiffuse(1, 1, 1);
            mat.setSpecular(1, 1, 1);

            material = mat;


            //pass the projection matrix to the shader
            gl.glUniformMatrix4fv(
                    shaderLocations.getLocation("projection"),
                    1, false, proj.get(fb16));

            //pass the modelview matrix to the shader
            gl.glUniformMatrix4fv(
                    shaderLocations.getLocation("modelview"),
                    1, false, modelView.get(fb16));

            //send the color of the planet
            gl.glUniform4fv(
                    shaderLocations.getLocation("vColor")
                    , 1, material.getAmbient().get(fb4));

            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL3.GL_LINE); //OUTLINES

            //draw the object
            planetMeshObject.draw(gla);

            /////////////////////////////////////////////////
            //
            //       Draw orbit circles
            //
            /////////////////////////////////////////////////

            modelView = new Matrix4f().mul(orbitTransform);

            mat.setAmbient(new Vector4f(1f,1f,1f,1f));

            //pass the modelview matrix to the shader
            gl.glUniformMatrix4fv(
                    shaderLocations.getLocation("modelview"),
                    1, false, modelView.get(fb16));

            //send the color of the planet
            gl.glUniform4fv(
                    shaderLocations.getLocation("vColor")
                    , 1, material.getAmbient().get(fb4));

            orbitMeshObject.draw(gla);

            //System.out.println("Planet " + p.name + " has " + p.moons.size() + " moons.");
            //System.out.println(p.hasMoons());
            if (p.hasMoons()) {
                for (int j = 0; j < p.moons.size(); j++) {

                    /////////////////////////////////////////////////
                    //
                    //       Draw moon spheres
                    //
                    /////////////////////////////////////////////////

                    Planet m = solarSystem.get(i).moons.get(j);
                    //modelView = new Matrix4f().mul(modelViews.peek());

                    Matrix4f moonScale = new Matrix4f().scale(m.getRadius(), m.getRadius(), m.getRadius());
                    Matrix4f moonTranslate = new Matrix4f().translate(m.getOrbitRadius(), 0, 0);
                    Matrix4f moonAdvancePosition = new Matrix4f().rotate(m.getSpeed() * (float)Math.toRadians(angleOfRotation),0,0,1);
                    Matrix4f moonRotatePhi = new Matrix4f().rotate(m.getPhi(),0,1,0);
                    Matrix4f moonRotateTheta = new Matrix4f().rotate(m.getTheta(),0,0,1);

                    //moonTransform = modelView.mul(moonRotateTheta).mul(moonRotatePhi).mul(moonAdvancePosition).mul(moonTranslate).mul(moonScale);

                    moonTransform = new Matrix4f().mul(modelViews.peek())

                            .mul(new Matrix4f().rotate(p.getTheta(), 0, 0, 1))
                            .mul(new Matrix4f().rotate(p.getPhi(), 0, 1, 0))
                            .mul(new Matrix4f().rotate(p.getSpeed() * (float) Math.toRadians(angleOfRotation), 0, 0, 1))
                            .mul(new Matrix4f().translate(p.getOrbitRadius(), 0, 0))

                            .mul(new Matrix4f().rotate(m.getTheta(), 0, 0, 1))
                            .mul(new Matrix4f().rotate(m.getPhi(), 0, 1, 0))
                            .mul(new Matrix4f().rotate(m.getSpeed() * (float) Math.toRadians(angleOfRotation), 0, 0, 1))
                            .mul(new Matrix4f().translate(m.getOrbitRadius(), 0, 0))
                            .mul(new Matrix4f().scale(m.getRadius(), m.getRadius(), m.getRadius()));

                    moonOrbitTransform = new Matrix4f().mul(modelViews.peek())
                            .mul(new Matrix4f().rotate(p.getTheta(), 0, 0, 1))
                            .mul(new Matrix4f().rotate(p.getPhi(), 0, 1, 0))
                            .mul(new Matrix4f().rotate(p.getSpeed() * (float) Math.toRadians(angleOfRotation), 0, 0, 1))
                            .mul(new Matrix4f().translate(p.getOrbitRadius(), 0, 0))
                            .mul(new Matrix4f().rotate(m.getTheta(), 0, 0, 1))
                            .mul(new Matrix4f().rotate(m.getPhi(), 0, 1, 0))
                            .mul(new Matrix4f().scale(m.getOrbitRadius(), m.getOrbitRadius(), m.getOrbitRadius()));

                    modelView = new Matrix4f().mul(moonTransform);

                    //System.out.println("Planet " + p.name + " has color: " + p.getColor());
                    mat.setAmbient(m.getColor());
                    mat.setDiffuse(1, 1, 1);
                    mat.setSpecular(1, 1, 1);

                    material = mat;


                    //pass the projection matrix to the shader
                    gl.glUniformMatrix4fv(
                            shaderLocations.getLocation("projection"),
                            1, false, proj.get(fb16));

                    //pass the modelview matrix to the shader
                    gl.glUniformMatrix4fv(
                            shaderLocations.getLocation("modelview"),
                            1, false, modelView.get(fb16));

                    //send the color of the planet
                    gl.glUniform4fv(
                            shaderLocations.getLocation("vColor")
                            , 1, material.getAmbient().get(fb4));

                    gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL3.GL_LINE); //OUTLINES

                    //draw the object
                    planetMeshObject.draw(gla);

                    /////////////////////////////////////////////////
                    //
                    //       Draw moon orbit circles
                    //
                    /////////////////////////////////////////////////

                    modelView = new Matrix4f().mul(moonOrbitTransform);

                    mat.setAmbient(new Vector4f(1f,1f,1f,1f));

                    //pass the modelview matrix to the shader
                    gl.glUniformMatrix4fv(
                            shaderLocations.getLocation("modelview"),
                            1, false, modelView.get(fb16));

                    //send the color of the planet
                    gl.glUniform4fv(
                            shaderLocations.getLocation("vColor")
                            , 1, material.getAmbient().get(fb4));

                    orbitMeshObject.draw(gla);
                }
            }
        }

        /////////////////////////////////////////////////
        //
        //       Draw bounding box cube
        //
        /////////////////////////////////////////////////

        boxTransform = new Matrix4f().mul(modelViews.peek())
                .mul(new Matrix4f().scale(BOUNDING_BOX_SCALAR, BOUNDING_BOX_SCALAR, BOUNDING_BOX_SCALAR));

        modelView = new Matrix4f().mul(boxTransform);

        util.Material mat = new util.Material();

        //System.out.println("Planet " + p.name + " has color: " + p.getColor());
        mat.setAmbient(new Vector4f(1f,1f,1f,1f)); // white
        mat.setDiffuse(1, 1, 1);
        mat.setSpecular(1, 1, 1);

        material = mat;


        //pass the modelview matrix to the shader
        gl.glUniformMatrix4fv(
                shaderLocations.getLocation("modelview"),
                1, false, modelView.get(fb16));

        //send the color of the planet
        gl.glUniform4fv(
                shaderLocations.getLocation("vColor")
                , 1, material.getAmbient().get(fb4));

        boxMeshObject.draw(gla);

        gl.glFlush();

        //disable the program
        program.disable(gl);

        while (!modelViews.empty()) {
            modelViews.pop();
        }
    }

    //this method is called from the JOGLFrame class, everytime the window resizes
    public void reshape(GLAutoDrawable gla, int x, int y, int width, int height) {
        GL gl = gla.getGL();
        WINDOW_WIDTH = width;
        WINDOW_HEIGHT = height;
        gl.glViewport(0, 0, width, height);

        proj = new Matrix4f().perspective((float)Math.toRadians(80.0f),
                (float) width/height,
                0.1f,
                10000.0f);
        // proj = new Matrix4f().ortho(-400,400,-400,400,0.1f,10000.0f);

    }

    public void dispose(GLAutoDrawable gla) {

        planetMeshObject.cleanup(gla);
        orbitMeshObject.cleanup(gla);
    }

    public void trackballRotation(int startX, int startY, int endX, int endY) {
        float rotY = (float)Math.toRadians(endX-startX);
        float rotX = (float)Math.toRadians(endY-startY);

        //if (!modelViews.empty()) {
            /*
            Matrix4f rotationMatrix = new Matrix4f().rotationXYZ((float)Math.toRadians(rotX), (float)Math.toRadians(rotY), 0f);
            modelViews.peek().mul(rotationMatrix);
            */

            //modelViews.peek().mul(new Matrix4f().rotationXYZ(rotX/10, 0f, rotY/10));

            trackballTransform = new Matrix4f()
                    .rotate(rotX/30f, 1f, 0f, 0f)
                    .rotate(rotY/30f, 0f, 1f, 0f)
                    .mul(trackballTransform);
        //}
    }
}
