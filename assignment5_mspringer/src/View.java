import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.GLBuffers;

import com.jogamp.opengl.util.texture.Texture;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.List;
import raytrace.Ray3D;
import raytrace.HitRecord;
import util.Material;
import util.ObjectInstance;


/**
 * Created by ashesh on 9/18/2015.
 *
 * The View class is the "controller" of all our OpenGL stuff. It cleanly
 * encapsulates all our OpenGL functionality from the rest of Java GUI, managed
 * by the JOGLFrame class.
 */
public class View {
    private int WINDOW_WIDTH, WINDOW_HEIGHT;
    private Stack<Matrix4f> modelViews;
    private Matrix4f projection, trackballTransform, modelView;
    private float trackballRadius;
    private Vector2f mousePos;


    private util.ShaderProgram program;
    private util.ShaderLocationsVault shaderLocations;
    private int projectionLocation;
    private sgraph.IScenegraph<VertexAttrib> scenegraph;
    private float fieldOfView = 120.0f;

    private List<ObjectInstance> meshObjects;
    private List<util.TextureImage> textures;
    private List<util.Material> materials;
    private List<Matrix4f> transforms;
    private List<util.Light> lights;
    private boolean raytrace;


    public View() {
        projection = new Matrix4f();
        modelViews = new Stack<Matrix4f>();
        trackballRadius = 300;
        trackballTransform = new Matrix4f();
        scenegraph = null;
    }

    public void initScenegraph(GLAutoDrawable gla, InputStream in) throws Exception {
        GL3 gl = gla.getGL().getGL3();

        if (scenegraph != null)
            scenegraph.dispose();

        program.enable(gl);

        scenegraph = sgraph.SceneXMLReader.importScenegraph(in, new VertexAttribProducer());

        sgraph.IScenegraphRenderer renderer = new sgraph.GL3ScenegraphRenderer();
        renderer.setContext(gla);
        Map<String, String> shaderVarsToVertexAttribs = new HashMap<String, String>();
        shaderVarsToVertexAttribs.put("vPosition", "position");
        shaderVarsToVertexAttribs.put("vNormal", "normal");
        shaderVarsToVertexAttribs.put("vTexCoord", "texcoord");
        renderer.initShaderProgram(program, shaderVarsToVertexAttribs);
        scenegraph.setRenderer(renderer);
        program.disable(gl);
    }

    private void raytrace(int width, int height, Stack<Matrix4f> modelView) {
        int i, j;

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Vector4f start = new Vector4f(0,0,0,1);

        for (i = 0; i < width; i++) {
            for (j = 0; j < height; j++) {

                Ray3D ray = new Ray3D(start, i, j, width, height, (float)Math.toRadians(fieldOfView));

                float Dx = (i - (width/2)) - start.x;
                float Dy = (j - (height/2)) - start.y;

                List<HitRecord> hits = scenegraph.raycast(ray, modelView);
                /*
                 create ray in view coordinates
                 start point: 0,0,0 always!
                 going through near plane pixel (i,j)
                 So 3D location of that pixel in view coordinates is
                 x = i-width/2
                 y = j-height/2
                 z = -0.5*height/tan(0.5*FOVY)
                */

                /*
                if (hits.size() > 0) {
                    float t = hits.get(0).getTEnter();
                    Vector3f intersect = new Vector3f(ray.getStart().add(ray.getDirection().mul(t)));
                    float reflection = hits.get(0).getMaterial().getReflection();

                    if (reflection > 0) {
                        Ray3D reflectRay = new Ray3D(intersect, new Vector3f(ray.getDirection().reflect(hits.get(0).getNormal())));
                    }
                }
                */


                //get color in (r,g,b)

                int r, g, b;
                Vector4f outColor = new Vector4f(0,0,0,0);
                if (hits.size() > 0) {
                    Material mat = hits.get(0).getMaterial();
                    outColor = mat.getAmbient();
                    System.out.println("Hit at Point (" + i + "," + j + ")!");
                }
                Color color = new Color(outColor.x, outColor.y, outColor.z);
                if (i % 10 == 0 && j % 10 == 0) {
                    // System.out.println("Color at Point (" + i + "," + j + ") is: " + color.toString());
                }
                output.setRGB(i, j, color.getRGB());
            }
        }

        System.out.println("Finished rayTracing!");

        /*
            REFLECTION

            If object is reflective
            I - incoming ray
            P - Point of Intersection
            N - Normal at P
            R - Reflective Ray (reflection of I about N)
            x -

            Cp - Color at point P:
                a - coefficient of absorption
                r - coefficient of reflection
                a + r = 1
                Cp = a * Color from shading + r * Color from reflection
         */

        /*
            REFRACTION

            Snell's Law of Refraction:
            C = speed of light
            Refractive index Mu: Cvacuum / Cmaterial ( > 1)
            I - Incoming ray
            P - Point of intersection
            N - Normal at P
            T - Refracted ray
            X - Ray perpendicular to N
            ThetaI - Angle between I and N
            ThetaT - angle between T and -N
            MuI - Refractive Index of I
            MuT - Refactive Index of T
            sin(ThetaI)/sin(ThetaT) = MuT / MuI

            T = sin(ThetaT) * X - cos(ThetaT) * N
            X = I + cos(ThetaI) * N
            X = I - (N dot I) * N / sin(ThetaI)
            T = MuI/MuT(I - (N dot I) * N) - cos(ThetaT) * N

            cos(ThetaI) = -(N dot I)
            sin(ThetaI) = sqrt(1 - (N dot I)^2)
            sin(ThetaT) = MuI/MuT * sqrt(1 - (N dot I)^2)
            sin(ThetaT) = (MuI/MuT)^2 * (1 - (N dot I)^2)
            cos(ThetaT) = sqrt(1 - (MuI/MuT)^2 * (1 - (N dot I)^2)
         */

        OutputStream outStream = null;

        try {
            outStream = new FileOutputStream("output/raytrace.png");
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Could not write raytraced image!");
        }

        try {
            ImageIO.write(output, "png", outStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not write raytraced image!");
        }
    }


    public void init(GLAutoDrawable gla) throws Exception {
        GL3 gl = gla.getGL().getGL3();


        //compile and make our shader program. Look at the ShaderProgram class for details on how this is done
        program = new util.ShaderProgram();

        program.createProgram(gl, "shaders/phong-multiple.vert",
                "shaders/phong-multiple.frag");

        shaderLocations = program.getAllShaderVariables(gl);

        //get input variables that need to be given to the shader program
        projectionLocation = shaderLocations.getLocation("projection");

        raytrace = true;
    }

    /*
    private void initObjects(GL3 gl) throws FileNotFoundException, IOException {

        util.PolygonMesh<?> tmesh;

        InputStream in;

        in = getClass().getClassLoader().getResourceAsStream("models/box.obj");

        tmesh = util.ObjImporter.importFile(new VertexAttribProducer(),
                in, true);

        util.ObjectInstance obj;

        Map<String, String> shaderToVertexAttribute = new HashMap<String, String>();

        shaderToVertexAttribute.put("vPosition", "position");
        shaderToVertexAttribute.put("vNormal", "normal");
        shaderToVertexAttribute.put("vTexCoord", "texcoord");


        obj = new util.ObjectInstance(
                gl,
                program,
                shaderLocations,
                shaderToVertexAttribute,
                tmesh, new String(""));
        meshObjects.add(obj);
        util.Material mat;

        mat = new util.Material();

        mat.setAmbient(0.5f, 0.5f, 0.5f);
        mat.setDiffuse(0.6f, 0.6f, 0.6f);
        mat.setSpecular(0.6f, 0.6f, 0.6f);
        mat.setShininess(100);
        materials.add(mat);

        Matrix4f t;

        t = new Matrix4f();
        transforms.add(t);

        // textures

        util.TextureImage textureImage;

        textureImage = new util.TextureImage("textures/die.png",
                "png",
                "white");

        Texture tex = textureImage.getTexture();


        tex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
        tex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
        tex.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        tex.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

        textures.add(textureImage);
    }

    private void initLights() {
        util.Light l = new util.Light();
        l.setAmbient(0.5f, 0.5f, 0.5f);
        l.setDiffuse(0.5f, 0.5f, 0.5f);
        l.setSpecular(0.5f, 0.5f, 0.5f);
        l.setPosition(-100, 100, 100);
        lights.add(l);

        l = new util.Light();
        l.setAmbient(0.5f, 0.5f, 0.5f);
        l.setDiffuse(0.5f, 0.5f, 0.5f);
        l.setSpecular(0.5f, 0.5f, 0.5f);
        l.setPosition(100, 100, 100);
        lights.add(l);

    }
    */


    public void draw(GLAutoDrawable gla) {
        while (!modelViews.empty()) {
            modelViews.pop();
        }

        modelView = new Matrix4f().lookAt(new Vector3f(0, 0, -15f), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        modelViews.push(new Matrix4f().mul(modelView));

        if (raytrace) {
            raytrace(WINDOW_WIDTH, WINDOW_HEIGHT, modelViews);
            raytrace = false;
        } else {
            scenegraph.draw(modelViews);
        }
    }

    public void drawOpenGL(GLAutoDrawable gla) {
        GL3 gl = gla.getGL().getGL3();
        FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);
        FloatBuffer fb4 = Buffers.newDirectFloatBuffer(4);


        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL.GL_DEPTH_TEST);

        program.enable(gl);
        for (int i = 0; i < lights.size(); i++) {

            Vector4f pos = lights.get(i).getPosition();
            Matrix4f lightTransformation;

            lightTransformation = new Matrix4f(modelView);
            pos = lightTransformation.transform(pos);
            String varName = "light[" + i + "].position";

            gl.glUniform4fv(shaderLocations.getLocation(varName), 1, pos.get
                    (fb4));
        }

    /*
     *Supply the shader with all the matrices it expects.
    */
        gl.glUniformMatrix4fv(
                shaderLocations.getLocation("projection"),
                1,
                false, projection.get(fb16));


        //all the light properties, except positions
        gl.glUniform1i(shaderLocations.getLocation("numLights"),
                lights.size());
        for (int i = 0; i < lights.size(); i++) {
            String name = "light[" + i + "].";
            gl.glUniform3fv(shaderLocations.getLocation(name + "ambient"),
                    1, lights.get(i).getAmbient().get(fb4));
            gl.glUniform3fv(shaderLocations.getLocation(name + "diffuse"),
                    1, lights.get(i).getDiffuse().get(fb4));
            gl.glUniform3fv(shaderLocations.getLocation(name + "specular"),
                    1, lights.get(i).getSpecular().get(fb4));
        }

        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glActiveTexture(GL.GL_TEXTURE0);


        gl.glUniform1i(shaderLocations.getLocation("image"), 0);


        for (int i = 0; i < meshObjects.size(); i++) {
            Matrix4f transformation = new Matrix4f().mul(modelView).mul(trackballTransform).mul(transforms.get(i));
            Matrix4f normalmatrix = new Matrix4f(transformation);
            normalmatrix = normalmatrix.invert().transpose();
            gl.glUniformMatrix4fv(shaderLocations.getLocation("modelview"), 1, false, transformation.get(fb16));
            gl.glUniformMatrix4fv(shaderLocations.getLocation("normalmatrix"), 1, false, normalmatrix.get(fb16));

            gl.glUniform3fv(shaderLocations.getLocation("material.ambient"), 1, materials.get(i).getAmbient().get(fb4));
            gl.glUniform3fv(shaderLocations.getLocation("material.diffuse"), 1, materials.get(i).getDiffuse().get(fb4));
            gl.glUniform3fv(shaderLocations.getLocation("material.specular"), 1, materials.get(i).getSpecular().get(fb4));
            gl.glUniform1f(shaderLocations.getLocation("material.shininess"), materials.get(i).getShininess());

            textures.get(i).getTexture().bind(gl);
            meshObjects.get(i).draw(gla);
        }
        gl.glFlush();

        program.disable(gl);


    }

    public void mousePressed(int x, int y) {
        mousePos = new Vector2f(x, y);
    }

    public void mouseReleased(int x, int y) {
        System.out.println("Released");
    }

    public void mouseDragged(int x, int y) {
        Vector2f newM = new Vector2f(x, y);

        Vector2f delta = new Vector2f(newM.x - mousePos.x, newM.y - mousePos.y);
        mousePos = new Vector2f(newM);

        trackballTransform = new Matrix4f().rotate(delta.x / trackballRadius, 0, 1, 0)
                .rotate(delta.y / trackballRadius, 1, 0, 0)
                .mul(trackballTransform);
    }

    public void reshape(GLAutoDrawable gla, int x, int y, int width, int height) {
        GL gl = gla.getGL();
        WINDOW_WIDTH = width;
        WINDOW_HEIGHT = height;
        gl.glViewport(0, 0, width, height);

        projection = new Matrix4f().perspective((float) Math.toRadians(fieldOfView),
                (float) width / height, 0.1f, 10000.0f);
        // proj = new Matrix4f().ortho(-400,400,-400,400,0.1f,10000.0f);

    }

    public void dispose(GLAutoDrawable gla) {
        GL3 gl = gla.getGL().getGL3();

    }


}
