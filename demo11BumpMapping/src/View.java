import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

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
  private int WINDOW_WIDTH, WINDOW_HEIGHT;
  private Matrix4f proj, modelView;
  private List<ObjectInstance> meshObjects;
  private List<util.TextureImage> textures, normalmaps;
  private List<util.Material> materials;
  private List<Matrix4f> transforms;
  private List<util.Light> lights;
  private Matrix4f trackballTransform, textureTransform;
  private float trackballRadius;
  private Vector2f mousePos;
  private boolean bumpMapping;
  private util.ShaderLocationsVault shaderLocations;

  class LightLocation {
    int ambient, diffuse, specular, position;

    public LightLocation() {
      ambient = diffuse = specular = position = -1;
    }
  }


  util.ShaderProgram program;
  private int modelviewLocation, projectionLocation, normalmatrixLocation, texturematrixLocation;
  private int materialAmbientLocation, materialDiffuseLocation, materialSpecularLocation, materialShininessLocation;
  private int textureLocation, normalMapLocation, bumpMappingEnabledLocation;
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
    lightLocations = new ArrayList<LightLocation>();
    textures = new ArrayList<util.TextureImage>();
    normalmaps = new ArrayList<util.TextureImage>();
    textureTransform = new Matrix4f();

    trackballTransform = new Matrix4f();
    angleOfRotation = 0;
    trackballRadius = 300;

    bumpMapping = false;

  }

  /**
   * Computes the tangent vector for each vertex in a polygon mesh This will
   * help in setting up a normal space coordinate system for bump mapping
   */
  private <T extends util.IVertexData> void computeTangents(
          util.PolygonMesh<T> tmesh) {
    int i, j;
    List<Vector4f> tangents = new ArrayList<Vector4f>();
    float[] data;

    List<T> vertexData = tmesh.getVertexAttributes();
    List<Integer> primitives = tmesh.getPrimitives();
    int primitiveSize = tmesh.getPrimitiveSize();

    tangents.clear();
    for (i = 0; i < vertexData.size(); i++) {
      tangents.add(new Vector4f(0, 0, 0, 0));
    }

    //go through all the triangles
    for (i = 0; i < primitives.size(); i += primitiveSize) {
      int i0, i1, i2;

      i0 = primitives.get(i);
      i1 = primitives.get(i + 1);
      i2 = primitives.get(i + 2);

      data = vertexData.get(i0).getData("position");

      Vector3f v0 = new Vector3f(
              data[0],
              data[1],
              data[2]);

      data = vertexData.get(i1).getData("position");
      Vector3f v1 = new Vector3f(
              data[0],
              data[1],
              data[2]);

      data = vertexData.get(i2).getData("position");
      Vector3f v2 = new Vector3f(
              data[0],
              data[1],
              data[2]);

      // Shortcuts for UVs
      data = vertexData.get(i0).getData("texcoord");
      Vector2f uv0 = new Vector2f(
              data[0],
              data[1]);

      data = vertexData.get(i1).getData("texcoord");
      Vector2f uv1 = new Vector2f(
              data[0],
              data[1]);

      data = vertexData.get(i2).getData("texcoord");
      Vector2f uv2 = new Vector2f(
              data[0],
              data[1]);

      // Edges of the triangle : position delta
      Vector3f deltaPos1 = v1.sub(v0);
      Vector3f deltaPos2 = v2.sub(v0);

      // UV delta
      Vector2f deltaUV1 = uv1.sub(uv0);
      Vector2f deltaUV2 = uv2.sub(uv0);

      float r = 1.0f / (deltaUV1.x * deltaUV2.y - deltaUV1.y * deltaUV2.x);
      Vector3f tangent = deltaPos1.mul(deltaUV2.y).sub(deltaPos2.mul(deltaUV1.y)).mul(r);

      for (j = 0; j < 3; j++) {

        tangents.set(primitives.get(i + j), new Vector4f(tangents.get(primitives.get(i + j))).add(new Vector4f(tangent, 0.0f)));
      }
    }

    for (i = 0; i < tangents.size(); i++) {
      Vector3f t = new Vector3f(tangents.get(i).x, tangents.get(i).y, tangents.get(i).z);
      t = t.normalize();
      data = vertexData.get(i).getData("normal");
      Vector3f n = new Vector3f(
              data[0],
              data[1],
              data[2]);

      Vector3f b = new Vector3f(n).cross(t);
      t = new Vector3f(b).cross(n);

      t = t.normalize();

      tangents.set(i, new Vector4f(t, 0.0f));
    }

    data = new float[4];
    for (i = 0; i < vertexData.size(); i++) {
      data[0] = tangents.get(i).x;
      data[1] = tangents.get(i).y;
      data[2] = tangents.get(i).z;
      data[3] = tangents.get(i).w;

      vertexData.get(i).setData("tangent", data);
    }
    tmesh.setVertexData(vertexData);
  }

  private void initObjects(GL3 gl) throws FileNotFoundException, IOException {
    util.PolygonMesh<?> tmesh;

    InputStream in;

    in = getClass().getClassLoader().getResourceAsStream(
            "models/sphere.obj");

    tmesh = util.ObjImporter.importFile(new VertexAttribProducer(), in, true);
    computeTangents(tmesh);
    util.ObjectInstance obj;

    Map<String, String> shaderToVertexAttribute = new HashMap<String, String>();

    shaderToVertexAttribute.put("vPosition", "position");
    shaderToVertexAttribute.put("vNormal", "normal");
    shaderToVertexAttribute.put("vTexCoord", "texcoord");
    shaderToVertexAttribute.put("vTangent", "tangent");


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
    textureImage = new util.TextureImage("textures/earthmap.jpg", "jpg",
            "earth");

    Texture tex = textureImage.getTexture();


    tex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
    tex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

    tex.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);

    textures.add(textureImage);


    //normal maps
    textureImage = new util.TextureImage("textures/earthmap-normalmap.jpg",
            "jpg",
            "earth-normalmap");

    Texture norm = textureImage.getTexture();


    norm.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
    norm.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
    norm.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);

    normalmaps.add(textureImage);

  }

  private void initLights() {
    util.Light l = new util.Light();
    l.setAmbient(0.8f, 0.8f, 0.8f);
    l.setDiffuse(0.8f, 0.8f, 0.8f);
    l.setSpecular(0.8f, 0.8f, 0.8f);
    l.setPosition(300, 300, 300);
    lights.add(l);
  }

  private void initShaderVariables(GL3 gl, util.ShaderProgram program) {
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
    normalMapLocation = shaderLocations.getLocation("normalmap");
    bumpMappingEnabledLocation = shaderLocations.getLocation("bumpMapping");
    numLightsLocation = shaderLocations.getLocation("numLights");
    for (int i = 0; i < lights.size(); i++) {
      LightLocation ll = new LightLocation();
      String name;

      name = "light[" + i + "]";
      ll.ambient = shaderLocations.getLocation(name + ".ambient");
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
    program.createProgram(gl, "shaders/normalmapping.vert", "shaders/normalmapping.frag");
    shaderLocations = program.getAllShaderVariables(gl);

    initObjects(gl);

    initLights();
    initShaderVariables(gl, program);

  }


  private List<Vector4f> getLightPositions(Matrix4f transformation) {
    List<Vector4f> lPositions = new ArrayList<Vector4f>();
    for (int i = 0; i < lights.size(); i++) {
      Vector4f v = lights.get(i).getPosition();
      v = transformation.transform(v);
      lPositions.add(v);
    }
    return lPositions;

  }

  public void draw(GLAutoDrawable gla) {
    angleOfRotation = (angleOfRotation + 1) % 360;
    GL3 gl = gla.getGL().getGL3();
    FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);
    FloatBuffer fb4 = Buffers.newDirectFloatBuffer(4);


    gl.glClearColor(0, 0, 0, 1);
    gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
    gl.glEnable(GL.GL_DEPTH_TEST);

    program.enable(gl);

        /*
         *In order to change the shape of this triangle, we can either move the vertex positions above, or "transform" them
         * We use a modelview matrix to store the transformations to be applied to our triangle.
         * Right now this matrix is identity, which means "no transformations"
         */
    modelView = new Matrix4f().lookAt(new Vector3f(0, 0, 60.0f), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));

    //modelview currently represents world-to-view transformation
    //transform all lights here if you specified them in world coordinates
    for (int i = 0; i < lights.size(); i++) {
      Vector4f pos = lights.get(i).getPosition();
      Matrix4f lightTransformation;
      lightTransformation = new Matrix4f(modelView);
      pos = lightTransformation.transform(pos);
      gl.glUniform4fv(lightLocations.get(i).position, 1, pos.get(fb4));
    }

    /*
     *Supply the shader with all the matrices it expects.
    */
    gl.glUniformMatrix4fv(projectionLocation, 1, false, proj.get(fb16));
    //return;
    if (bumpMapping)
      gl.glUniform1i(bumpMappingEnabledLocation, 1);
    else
      gl.glUniform1i(bumpMappingEnabledLocation, 0);

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


    gl.glActiveTexture(GL.GL_TEXTURE1);
    gl.glUniform1i(normalMapLocation, 1);


    for (int i = 0; i < meshObjects.size(); i++) {
      Matrix4f transformation = new Matrix4f().mul(modelView).mul(trackballTransform).mul(transforms.get(i));
      Matrix4f normalmatrix = new Matrix4f(transformation);
      normalmatrix = normalmatrix.invert().transpose();
      gl.glUniformMatrix4fv(modelviewLocation, 1, false, transformation.get(fb16));
      gl.glUniformMatrix4fv(normalmatrixLocation, 1, false, normalmatrix.get(fb16));


      if (textures.get(i).getTexture().getMustFlipVertically()) //for
      // flipping the image
      // vertically
      {
        textureTransform = new Matrix4f().translate(0, 1, 0).scale(1, -1, 1);
      }

      gl.glUniformMatrix4fv(texturematrixLocation, 1, false, textureTransform.get(fb16));
      gl.glUniform3fv(materialAmbientLocation, 1, materials.get(i).getAmbient().get(fb4));
      gl.glUniform3fv(materialDiffuseLocation, 1, materials.get(i).getDiffuse().get(fb4));
      gl.glUniform3fv(materialSpecularLocation, 1, materials.get(i).getSpecular().get(fb4));
      gl.glUniform1f(materialShininessLocation, materials.get(i).getShininess());

      gl.glActiveTexture(GL.GL_TEXTURE0);
      textures.get(i).getTexture().bind(gl);

      gl.glActiveTexture(GL.GL_TEXTURE1);
      normalmaps.get(i).getTexture().bind(gl);

      meshObjects.get(i).draw(gla);
    }
    gl.glFlush();
    program.disable(gl);


  }

  public void toggleBumpMapping() {
    bumpMapping = !bumpMapping;
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
