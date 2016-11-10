import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import util.IVertexData;
import util.ObjectInstance;


import java.io.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by ashesh on 9/18/2015.
 *
 * The View class is the "controller" of all our OpenGL stuff. It cleanly
 * encapsulates all our OpenGL functionality from the rest of Java GUI, managed
 * by the JOGLFrame class.
 */
public class View {
    private enum LIGHT_COORDINATE{WORLD,VIEW};
    private int WINDOW_WIDTH, WINDOW_HEIGHT;
    private Matrix4f proj, modelView;
    private List<ObjectInstance> meshObjects;
    private List<util.TextureImage> textures;
    private List<util.Material> materials;
    private List<Matrix4f> transforms;
    private List<util.Light> lights;
    //0-meshObjects.size()-1 are object coordinates, then world and then view
    private List<Integer> lightCoordinateSystems;
    private Matrix4f trackballTransform, textureTransform;
    private float trackballRadius;
    private Vector2f mousePos;
    private boolean mipmapped;

    class LightLocation {
        int ambient, diffuse, specular, position;

        public LightLocation() {
            ambient = diffuse = specular = position = -1;
        }
    }


    util.ShaderProgram program;
    util.ShaderLocationsVault shaderLocations;
    private int modelviewLocation, projectionLocation, normalmatrixLocation, texturematrixLocation;
    private int materialAmbientLocation, materialDiffuseLocation, materialSpecularLocation, materialShininessLocation;
    private int textureLocation;
    private List<LightLocation> lightLocations;
    private int numLightsLocation;
    int angleOfRotation;


    public View() {
        proj = new Matrix4f();
        proj.identity();

        modelView = new Matrix4f();
        modelView.identity();

        meshObjects = new ArrayList<ObjectInstance>();
        transforms = new ArrayList<Matrix4f>();
        materials = new ArrayList<util.Material>();
        lights = new ArrayList<util.Light>();
        lightCoordinateSystems = new ArrayList<Integer>();
        lightLocations = new ArrayList<LightLocation>();
        textures = new ArrayList<util.TextureImage>();
        textureTransform = new Matrix4f();

        trackballTransform = new Matrix4f();
        angleOfRotation = 0;
        trackballRadius = 300;
        mipmapped = false;

    }

    public void toggleMipmapping() {
        mipmapped = !mipmapped;
    }

    private void initObjects(GL3 gl) throws FileNotFoundException, IOException {

        util.PolygonMesh<?> tmesh;

        InputStream in;

        in = getClass().getClassLoader().getResourceAsStream
                ("models/sphere.obj");

        tmesh = util.ObjImporter.importFile(new VertexAttribProducer(), in, true);
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

        mat.setAmbient(0.3f, 0.3f, 0.3f);
        mat.setDiffuse(0.7f, 0.7f, 0.7f);
        mat.setSpecular(0.7f, 0.7f, 0.7f);
        mat.setShininess(100);
        materials.add(mat);

        Matrix4f t;

        t = new Matrix4f().translate(0, 0, 0).scale(50, 50, 50);
        transforms.add(t);

        // textures

        util.TextureImage textureImage;

        textureImage = new util.TextureImage("textures/white.png", "png",
                "white");

        Texture tex = textureImage.getTexture();


        tex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
        tex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
        tex.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        tex.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);

        textures.add(textureImage);

    }

    private void initLights() {
        util.Light l = new util.Light();
        l.setAmbient(0.9f, 0.9f, 0.9f);
        l.setDiffuse(0.9f, 0.9f, 0.9f);
        l.setSpecular(0.9f, 0.9f, 0.9f);
        l.setPosition(00, 00, 100);
        lights.add(l);
        //read comments above where this list is declared
        lightCoordinateSystems.add(meshObjects.size()); //world

 /*   l = new util.Light();
    l.setAmbient(0.5f, 0.5f, 0.5f);
    l.setDiffuse(0.5f, 0.5f, 0.5f);
    l.setSpecular(0.5f, 0.5f, 0.5f);
    l.setPosition(00, 00, 0);
    lights.add(l);
    */

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


    public void init(GLAutoDrawable gla) throws Exception {
        GL3 gl = gla.getGL().getGL3();


        //compile and make our shader program. Look at the ShaderProgram class for details on how this is done
        program = new util.ShaderProgram();
        program.createProgram(gl, "shaders/phong-multiple.vert", "shaders/phong-multiple.frag");
        shaderLocations = program.getAllShaderVariables(gl);

        initObjects(gl);
        initLights();
        initShaderVariables();

    }




    public void draw(GLAutoDrawable gla) {
        angleOfRotation = (angleOfRotation + 1);
        //angleOfRotation = (angleOfRotation + 1) % 360;
        GL3 gl = gla.getGL().getGL3();
        FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);
        FloatBuffer fb4 = Buffers.newDirectFloatBuffer(4);

        Vector4f lightPos = new Vector4f(
                (float)(20*Math.sin(0.01*angleOfRotation)), 0, 100, 1.0f);

        //lights.get(0).setPosition(lightPos);


        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL.GL_DEPTH_TEST);

        program.enable(gl);

        /*
         *In order to change the shape of this triangle, we can either move the vertex positions above, or "transform" them
         * We use a modelview matrix to store the transformations to be applied to our triangle.
         * Right now this matrix is identity, which means "no transformations"
         */
        modelView = new Matrix4f()
                .lookAt(
                        new Vector3f((float)(80*Math.cos(0.02f*angleOfRotation))),
                        new Vector3f(0, 0, 0),
                        new Vector3f(0, 1, 0));

        //modelview currently represents world-to-view transformation
        //transform all lights so that they are in the view coordinate system too
        //before you send them to the shader.
        //that way everything is in one coordinate system (view) and the math will
        //be correct
        for (int i = 0; i < lights.size(); i++) {
            Vector4f pos = lights.get(i).getPosition();
            Matrix4f lightTransformation=null;

            if (lightCoordinateSystems.get(i)==meshObjects.size()) { //light in world
                lightTransformation = new Matrix4f(modelView);
            }
            else if (lightCoordinateSystems.get(i)==meshObjects.size()+1) { //light
                // in view
                lightTransformation = new Matrix4f();
            }
            else {//assumed to be object
                lightTransformation = new Matrix4f(modelView)
                        .mul(trackballTransform)
                        .mul(transforms.get(lightCoordinateSystems.get(i)));
            }
            pos = lightTransformation.transform(pos);
            gl.glUniform4fv(lightLocations.get(i).position, 1, pos.get(fb4));
        }

    /*
     *Supply the shader with all the matrices it expects.
    */
        gl.glUniformMatrix4fv(projectionLocation, 1, false, proj.get(fb16));
        //return;


        //all the light properties, except positions
        gl.glUniform1i(numLightsLocation, lights.size());
        for (int i = 0; i < lights.size(); i++) {
            gl.glUniform3fv(lightLocations.get(i).ambient, 1, lights.get(i).getAmbient().get(fb4));
            gl.glUniform3fv(lightLocations.get(i).diffuse, 1, lights.get(i).getDiffuse().get(fb4));
            gl.glUniform3fv(lightLocations.get(i).specular, 1, lights.get(i).getSpecular().get(fb4));
        }

        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glActiveTexture(GL.GL_TEXTURE0);


        gl.glUniform1i(textureLocation, 0);


        for (int i = 0; i < meshObjects.size(); i++) {
            Matrix4f transformation = new Matrix4f().mul(modelView).mul(trackballTransform).mul(transforms.get(i));
            Matrix4f normalmatrix = new Matrix4f(transformation);
            normalmatrix = normalmatrix.invert().transpose();
            gl.glUniformMatrix4fv(modelviewLocation, 1, false, transformation.get(fb16));
            gl.glUniformMatrix4fv(normalmatrixLocation, 1, false, normalmatrix.get(fb16));

            if (textures.get(i).getTexture().getMustFlipVertically()) //for
            // flipping the
            // image vertically
            {
                textureTransform = new Matrix4f().translate(0, 1, 0).scale(1, -1, 1);
            } else
                textureTransform = new Matrix4f();

            // textureTransform = new Matrix4f(textureTransform).rotate((float)Math.toRadians(45),0,0,1);
            gl.glUniformMatrix4fv(texturematrixLocation, 1, false, textureTransform.get(fb16));
            gl.glUniform3fv(materialAmbientLocation, 1, materials.get(i).getAmbient().get(fb4));
            gl.glUniform3fv(materialDiffuseLocation, 1, materials.get(i).getDiffuse().get(fb4));
            gl.glUniform3fv(materialSpecularLocation, 1, materials.get(i).getSpecular().get(fb4));
            gl.glUniform1f(materialShininessLocation, materials.get(i).getShininess());

            if (mipmapped)
                textures.get(i).getTexture().setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL
                        .GL_LINEAR_MIPMAP_LINEAR);
            else
                textures.get(i).getTexture().setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL
                        .GL_NEAREST);
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

        //proj = new Matrix4f().perspective((float)Math.toRadians(120.0f),(float)width/height,0.1f,10000.0f);
        proj = new Matrix4f().ortho(-50, 50, -50, 50, 0.1f, 10000.0f);

    }

    public void dispose(GLAutoDrawable gla) {
        GL3 gl = gla.getGL().getGL3();

    }


}
