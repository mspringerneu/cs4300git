/**
 * Created by mspringer on 9/26/16.
 */
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;

import util.*;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import cs4300.assignment1.*;


/**
 * Created by ashesh on 9/18/2015.
 *
 * The View class is the "controller" of all our OpenGL stuff. It cleanly
 * encapsulates all our OpenGL functionality from the rest of Java GUI, managed
 * by the JOGLFrame class.
 */
public class View {
    private int WINDOW_WIDTH, WINDOW_HEIGHT;
    private int disks;
    private List<Integer> moves;
    private int currentMove;
    private Matrix4f proj;
    private Matrix4f modelview;
    private ArrayList<util.ObjectInstance> meshObjects;
    private ArrayList<util.ObjectInstance> diskObjects;
    private ArrayList<util.ObjectInstance> towerObjects;
    private ArrayList<Matrix4f> diskTransforms;
    private ArrayList<Matrix4f> towerTransforms;
    private ArrayList<util.Material> meshMaterials;
    private ArrayList<Matrix4f> meshTransforms;
    private ShaderLocationsVault shaderLocations;
    private Matrix4f towerTransform;
    private Matrix4f diskTransform;
    private List<Tower> towers = new ArrayList<Tower>();
    private float BASE_DIMENSION = 10f;
    private float TOWER_SEPARATION = 20f;
    private float TOWER_BASE_WIDTH = 200f;
    private float TOWER_BASE_HEIGHT = 200f;
    private float TOWER_POLE_WIDTH = 200f;
    private float TOWER_POLE_HEIGHT = 200f;
    private float DISK_HEIGHT = (TOWER_POLE_HEIGHT / disks) - 10f;
    private float DISK_MAX_WIDTH = TOWER_BASE_WIDTH - 5f;
    private float DISK_MIN_WIDTH = TOWER_POLE_WIDTH - 5f;
    private float DISK_INCREMENT = (DISK_MAX_WIDTH - DISK_MIN_WIDTH) / disks;
    private float BOTTOM_DISK_SCALE = 10f;


    ShaderProgram program;


    public View(int disks) {
        this.disks = disks;
        moves = new ArrayList<>();
        currentMove = 0;
        proj = new Matrix4f();
        proj.identity();
        modelview = new Matrix4f();
        modelview.identity();
        towerObjects = new ArrayList<util.ObjectInstance>();
        meshObjects = new ArrayList<util.ObjectInstance>();
        meshMaterials = new ArrayList<Material>();
        meshTransforms = new ArrayList<Matrix4f>();
        shaderLocations = null;
        WINDOW_WIDTH = WINDOW_HEIGHT = 0;

        diskTransform = new Matrix4f().scale(BOTTOM_DISK_SCALE,1,1);
    }

    public void generateObjects() {

    }

    public void init(GLAutoDrawable gla) throws Exception {
        cs4300.assignment1.TowersOfHanoi solver;
        solver = new cs4300.assignment1.NonRecTowersOfHanoi();
        solver.solve(disks);
        moves = solver.getMoves();

        GL3 gl = (GL3) gla.getGL().getGL3();


        //compile and make our shader program. Look at the ShaderProgram class for details on how this is done
        program = new ShaderProgram();
        program.createProgram(gl, "shaders/default.vert", "shaders/default.frag");

        shaderLocations = program.getAllShaderVariables(gl);

        for (int i =0; i < 3; i++) {
            Tower t = new Tower(i);
            towers.add(new Tower(i));
            ObjectInstance obj = new ObjectInstance(gl, program, shaderLocations, t.shaderToVertexAttribute.get(0), t.mesh.get(0), "triangles");
            towerObjects.add(obj);
            obj = new ObjectInstance(gl, program, shaderLocations, t.shaderToVertexAttribute.get(1), t.mesh.get(1), "triangles");
            towerObjects.add(obj);
            Matrix4f towerBaseTransform = new Matrix4f().translate((TOWER_BASE_WIDTH + TOWER_SEPARATION) * i, 0, 0, 1)
                    .scale(TOWER_BASE_WIDTH, TOWER_BASE_HEIGHT, 1, 1);
            towerTransforms.add(towerBaseTransform);
            Matrix4f towerPoleTransform = new Matrix4f().translate((TOWER_BASE_WIDTH + TOWER_SEPARATION) * i, TOWER_POLE_HEIGHT / 2 + TOWER_BASE_HEIGHT / 2, 0, 1)
                    .scale(TOWER_POLE_HEIGHT, TOWER_POLE_HEIGHT, 1, 1);
            towerTransforms.add(towerPoleTransform);
        }
        for (int i =0; i < disks; i++) {
            Disk d = new Disk(i, towers.get(0).getId());
            ObjectInstance obj = new ObjectInstance(gl, program, shaderLocations, d.shaderToVertexAttribute, d.mesh, "triangles");
            diskObjects.add(obj);
            diskTransforms.add(new Matrix4f());
            towers.get(0).addNewDisk(d);
        }


        /*
          Now we create a triangle mesh from these
          vertices.

          The mesh has vertex positions, colors and indices for now.

         */

        /*
        Create the vertices of the two triangles to be
        drawn. Since we are drawing in 2D, z-coordinate
        of all points will be 0. The fourth number
        for each vertex is 1. This is the
        homogeneous coordinate, and "1" means this
        is a location and not a direction
         */




        // tower positions
        for (int i = 0; i < towers.size(); i++) {
            for (int j = 0; j < 2; j++) {
                List<Vector4f> positions = new ArrayList<Vector4f>();
                if (j == 0) {
                    /** BASE **/
                    // bottom left
                    positions.add(new Vector4f(-BASE_DIMENSION, -BASE_DIMENSION, 0, 1.0f));
                    // bottom right
                    positions.add(new Vector4f(BASE_DIMENSION, -BASE_DIMENSION, 0, 1.0f));
                    // top right
                    positions.add(new Vector4f(BASE_DIMENSION, BASE_DIMENSION, 0, 1.0f));
                    // top left
                    positions.add(new Vector4f(-BASE_DIMENSION, BASE_DIMENSION, 0, 1.0f));
                }
                List<Vector4f> colors = new ArrayList<Vector4f>();
                colors.add(new Vector4f(1, 0, 0, 1)); //red

                List<IVertexData> vertexData = new ArrayList<IVertexData>();
                VertexAttribWithColorProducer producer = new VertexAttribWithColorProducer();
                for (int i = 0; i < positions.size(); i++) {
                    IVertexData v = producer.produce();
                    v.setData("position", new float[]{positions.get(i).x,
                            positions.get(i).y,
                            positions.get(i).z,
                            positions.get(i).w});
                    v.setData("color", new float[]{colors.get(0).x,
                            colors.get(0).y,
                            colors.get(0).z,
                            colors.get(0).w});
                    vertexData.add(v);
                }

            /*
            We now generate a series of indices.
            These indices will be for the above list
            of vertices. For example we want to use
            the above list to draw triangles.
            The first triangle will be created from
            vertex numbers 0, 1 and 2 in the list above
            (indices begin at 0 like arrays). The second
            triangle will be created from vertex numbers
            0, 2 and 3. Therefore we will create a list
            of indices {0,1,2,0,2,3}.

            What is the advantage of having a second
            list? Vertices are often shared between
            triangles, and having a separate list of
            indices avoids duplication of vertex data
             */
                List<Integer> indices = new ArrayList<Integer>();
                indices.add(0);
                indices.add(1);
                indices.add(2);

                indices.add(0);
                indices.add(2);
                indices.add(3);

                //now we create a polygon mesh object
                PolygonMesh mesh;

                mesh = new PolygonMesh();


                mesh.setVertexData(vertexData);
                mesh.setPrimitives(indices);

            /*
            It turns out, there are several ways of
            reading the list of indices and interpreting
            them as triangles.

            The first, simplest (and the one we have
            assumed above) is to just read the list of
            indices 3 at a time, and use them as triangles.
            In OpenGL, this is the GL_TRIANGLES mode.

            If we wanted to draw lines by reading the indices
            two at a time, we would specify GL_LINES.

            In any case, this "mode" and the actual list of
            indices are related. That is, decide which mode
            you want to use, and accordingly build the list
            of indices.
             */

                mesh.setPrimitiveType(GL.GL_TRIANGLES);
                mesh.setPrimitiveSize(3);

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
                Map<String, String> shaderToVertexAttribute = new HashMap<String, String>();

                //currently there are two per-vertex attributes: position and color
                shaderToVertexAttribute.put("vPosition", "position");
                shaderToVertexAttribute.put("vColor", "color");
                ObjectInstance obj = new ObjectInstance(gl, program, shaderLocations, shaderToVertexAttribute, mesh, "triangles");
                meshObjects.add(obj);

                if (j == 0) {
                    Matrix4f towerBaseTransform = new Matrix4f().translate((TOWER_BASE_WIDTH + TOWER_SEPARATION) * i, 0, 0, 1)
                            .scale(TOWER_BASE_WIDTH, TOWER_BASE_HEIGHT, 1, 1);
                    meshTransforms.add(towerBaseTransform);
                } else if (j == 1) {
                    Matrix4f towerPoleTransform = new Matrix4f().translate((TOWER_BASE_WIDTH + TOWER_SEPARATION) * i, TOWER_POLE_HEIGHT / 2 + TOWER_BASE_HEIGHT / 2, 0, 1)
                            .scale(TOWER_POLE_HEIGHT, TOWER_POLE_HEIGHT, 1, 1);
                    meshTransforms.add(towerPoleTransform);
                }
            }
        }

        List<Vector4f> colors = new ArrayList<Vector4f>();
        colors.add(new Vector4f(1, 0, 0, 1)); //red
        colors.add(new Vector4f(0, 1, 0, 1)); //green
        colors.add(new Vector4f(0, 0, 1, 1)); //blue
        colors.add(new Vector4f(0, 0, 0, 1)); //black

        for (int i = 0; i < towers.size(); i++) {
            for (int j = 0; j < towers.get(i).disks.size(); j++) {
                List<Vector4f> positions = new ArrayList<Vector4f>();
                /** BASE **/
                // bottom left
                positions.add(new Vector4f(-BASE_DIMENSION, -BASE_DIMENSION, 0, 1.0f));
                // bottom right
                positions.add(new Vector4f(BASE_DIMENSION, -BASE_DIMENSION, 0, 1.0f));
                // top right
                positions.add(new Vector4f(BASE_DIMENSION, BASE_DIMENSION, 0, 1.0f));
                // top left
                positions.add(new Vector4f(-BASE_DIMENSION, BASE_DIMENSION, 0, 1.0f));


                List<IVertexData> vertexData = new ArrayList<IVertexData>();
                VertexAttribWithColorProducer producer = new VertexAttribWithColorProducer();
                for (int i = 0; i < positions.size(); i++) {
                    IVertexData v = producer.produce();
                    v.setData("position", new float[]{positions.get(i).x,
                            positions.get(i).y,
                            positions.get(i).z,
                            positions.get(i).w});
                    v.setData("color", new float[]{colors.get(j % colors.size()).x,
                            colors.get(j % colors.size()).y,
                            colors.get(j % colors.size()).z,
                            colors.get(j % colors.size()).w});
                    vertexData.add(v);
                }

                /*
                We now generate a series of indices.
                These indices will be for the above list
                of vertices. For example we want to use
                the above list to draw triangles.
                The first triangle will be created from
                vertex numbers 0, 1 and 2 in the list above
                (indices begin at 0 like arrays). The second
                triangle will be created from vertex numbers
                0, 2 and 3. Therefore we will create a list
                of indices {0,1,2,0,2,3}.

                What is the advantage of having a second
                list? Vertices are often shared between
                triangles, and having a separate list of
                indices avoids duplication of vertex data
                 */
                List<Integer> indices = new ArrayList<Integer>();
                indices.add(0);
                indices.add(1);
                indices.add(2);

                indices.add(0);
                indices.add(2);
                indices.add(3);

                //now we create a polygon mesh object
                PolygonMesh mesh;

                mesh = new PolygonMesh();


                mesh.setVertexData(vertexData);
                mesh.setPrimitives(indices);

                /*
                It turns out, there are several ways of
                reading the list of indices and interpreting
                them as triangles.

                The first, simplest (and the one we have
                assumed above) is to just read the list of
                indices 3 at a time, and use them as triangles.
                In OpenGL, this is the GL_TRIANGLES mode.

                If we wanted to draw lines by reading the indices
                two at a time, we would specify GL_LINES.

                In any case, this "mode" and the actual list of
                indices are related. That is, decide which mode
                you want to use, and accordingly build the list
                of indices.
                 */

                mesh.setPrimitiveType(GL.GL_TRIANGLES);
                mesh.setPrimitiveSize(3);

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
                Map<String, String> shaderToVertexAttribute = new HashMap<String, String>();

                //currently there are two per-vertex attributes: position and color
                shaderToVertexAttribute.put("vPosition", "position");
                shaderToVertexAttribute.put("vColor", "color");
                ObjectInstance obj = new ObjectInstance(gl, program, shaderLocations, shaderToVertexAttribute, mesh, "triangles");
                meshObjects.add(obj);

                Matrix4f diskTransform = new Matrix4f().translate((TOWER_BASE_WIDTH + TOWER_SEPARATION) * , (TOWER_BASE_HEIGHT/2 + DISK_HEIGHT/2 + (DISK_HEIGHT * j)),0, 1)
                        .scale(DISK_MAX_WIDTH - (DISK_INCREMENT * j), DISK_HEIGHT, 1,1);

                meshTransforms.add(towerBaseTransform);
                meshTransforms.add(towerPoleTransform);
            }
        }
        // bottom left
        positions.add(new Vector4f(-DISK_HEIGHT/2f, -DISK_HEIGHT/2f, 0, 1.0f));
        // bottom right
        positions.add(new Vector4f(DISK_HEIGHT/2f, -DISK_HEIGHT/2f, 0, 1.0f));
        // top right
        positions.add(new Vector4f(DISK_HEIGHT/2f, DISK_HEIGHT/2f, 0, 1.0f));
        // top left
        positions.add(new Vector4f(-DISK_HEIGHT/2f, DISK_HEIGHT/2f, 0, 1.0f));

        //we add a second attribute to each vertex: color
        //note that the shader variable has been changed to "in" as compared
        // to HellJOGL because color is now a per-vertex attribute

        List<Vector4f> colors = new ArrayList<Vector4f>();
        colors.add(new Vector4f(1, 0, 0, 1)); //red
        colors.add(new Vector4f(0, 1, 0, 1)); //green
        colors.add(new Vector4f(0, 0, 1, 1)); //blue
        colors.add(new Vector4f(0, 0, 0, 1)); //black

        for (int i = 0; i < towers.get(1).disks.size(); i++) {
            towers.get(1).disks.get(i).setColor(colors.get(i % colors.size()));
        }


        //set up vertex attributes (in this case we have only position and color)
        List<IVertexData> vertexData = new ArrayList<IVertexData>();
        VertexAttribWithColorProducer producer = new VertexAttribWithColorProducer();
        for (int i = 0; i < positions.size(); i++) {
            IVertexData v = producer.produce();
            v.setData("position", new float[]{positions.get(i).x,
                    positions.get(i).y,
                    positions.get(i).z,
                    positions.get(i).w});
            v.setData("color", new float[]{colors.get(i).x,
                    colors.get(i).y,
                    colors.get(i).z,
                    colors.get(i).w});
            vertexData.add(v);
        }

        /*
        We now generate a series of indices.
        These indices will be for the above list
        of vertices. For example we want to use
        the above list to draw triangles.
        The first triangle will be created from
        vertex numbers 0, 1 and 2 in the list above
        (indices begin at 0 like arrays). The second
        triangle will be created from vertex numbers
        0, 2 and 3. Therefore we will create a list
        of indices {0,1,2,0,2,3}.

        What is the advantage of having a second
        list? Vertices are often shared between
        triangles, and having a separate list of
        indices avoids duplication of vertex data
         */
        List<Integer> indices = new ArrayList<Integer>();
        indices.add(0);
        indices.add(1);
        indices.add(2);

        indices.add(0);
        indices.add(2);
        indices.add(3);

        //now we create a polygon mesh object
        PolygonMesh mesh;

        mesh = new PolygonMesh();


        mesh.setVertexData(vertexData);
        mesh.setPrimitives(indices);

        /*
        It turns out, there are several ways of
        reading the list of indices and interpreting
        them as triangles.

        The first, simplest (and the one we have
        assumed above) is to just read the list of
        indices 3 at a time, and use them as triangles.
        In OpenGL, this is the GL_TRIANGLES mode.

        If we wanted to draw lines by reading the indices
        two at a time, we would specify GL_LINES.

        In any case, this "mode" and the actual list of
        indices are related. That is, decide which mode
        you want to use, and accordingly build the list
        of indices.
         */

        mesh.setPrimitiveType(GL.GL_TRIANGLES);
        mesh.setPrimitiveSize(3);

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
        Map<String, String> shaderToVertexAttribute = new HashMap<String, String>();

        //currently there are two per-vertex attributes: position and color
        shaderToVertexAttribute.put("vPosition", "position");
        shaderToVertexAttribute.put("vColor", "color");
        obj = new ObjectInstance(gl, program, shaderLocations, shaderToVertexAttribute, mesh, "triangles");


    }


    public void draw(GLAutoDrawable gla) {
        int from = moves.get(currentMove * 2);
        int to = moves.get((currentMove * 2) + 1);

        // get top disk from "from" tower
        Disk d = towers.get(from - 1).getTopDisk();
        // pop top disk from "from" tower
        towers.get(from - 1).popTopDisk();
        // set the disk's position to the "to" tower
        d.setPosition(to);
        // push the disk onto the "to" tower's stack
        towers.get(to - 1).addNewDisk(d);

        // increment the move counter
        currentMove++;





        GL3 gl = gla.getGL().getGL3();
        FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);

        //set the background color to be white
        gl.glClearColor(1, 1, 1, 1);
        //clear the background
        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        //enable the shader program
        program.enable(gl);


        // draw the towers
        for (int i = 0; i < towers.size(); i++) {


            for (int j = 0; j < 2; j++) {
                modelview = new Matrix4f();
                towerTransform = new Matrix4f();
                diskTransform = new Matrix4f();
                modelview = modelview *
                        (towerTransform *diskTransform);
                modelview = modelview.rotate(45, 0, 0, 1);


                gl.glUniformMatrix4fv(
                        shaderLocations.getLocation("modelview"),
                        1, false, modelview.get(fb16));

                gl.glUniformMatrix4fv(
                        shaderLocations.getLocation("projection"),
                        1, false, proj.get(fb16));


                obj.draw(gla);
            }
        }


         /*
         *In order to change the shape of this triangle, we can either move the vertex positions above, or "transform" them
         * We use a modelview matrix to store the transformations to be applied to our triangle.
         * Right now this matrix is identity, which means "no transformations"
         */
         for (int i = 0; i < towers.size(); i++) {
             for (int j = 0; j < towers.get(i).disks.size(); j++) {
                 modelview = new Matrix4f();
                 towerTransform = new Matrix4f();
                 diskTransform = new Matrix4f();
                 modelview = modelview *
                         (towerTransform *
                         diskTransform);
                 modelview = modelview.rotate(45, 0, 0, 1);


                 gl.glUniformMatrix4fv(
                         shaderLocations.getLocation("modelview"),
                         1, false, modelview.get(fb16));

                 gl.glUniformMatrix4fv(
                         shaderLocations.getLocation("projection"),
                         1, false, proj.get(fb16));


                 obj.draw(gla);
             }
         }


        gl.glFlush();
        //disable the program
        program.disable(gl);
    }

    public void reshape(GLAutoDrawable gla, int x, int y, int width, int height) {
        GL gl = gla.getGL();
        WINDOW_WIDTH = width;
        WINDOW_HEIGHT = height;
        gl.glViewport(0, 0, width, height);

        proj = new Matrix4f().ortho2D(-150, 150, -150, 150);

    }

    public void dispose(GLAutoDrawable gla) {
        obj.cleanup(gla);
    }
}

