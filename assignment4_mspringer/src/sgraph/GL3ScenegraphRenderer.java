package sgraph;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import org.joml.Matrix4f;
import util.IVertexData;
import util.TextureImage;
import com.jogamp.opengl.util.texture.Texture;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.*;

/**
 * This is a scene graph renderer implementation that works specifically with the JOGL library
 * It mandates OpenGL 3 and above.
 * @author Amit Shesh
 */
public class GL3ScenegraphRenderer implements IScenegraphRenderer {
    /**
     * The JOGL specific rendering context
     */
    private GLAutoDrawable glContext;
    /**
     * A table of shader locations and variable names
     */
    protected util.ShaderLocationsVault shaderLocations;
    /**
     * A table of shader variables -> vertex attribute names in each mesh
     */
    protected Map<String,String> shaderVarsToVertexAttribs;

    /**
     * A map to store all the texture names and paths
     */
    private Map<String, String> texturePaths;

    /**
     * A table of renderers for individual meshes
     */
    private Map<String,util.ObjectInstance> meshRenderers;

    /**
     * A variable tracking whether shader locations have been set. This must be done before
     * drawing!
     */
    private boolean shaderLocationsSet;

    public GL3ScenegraphRenderer()
    {
        meshRenderers = new HashMap<String,util.ObjectInstance>();
        shaderLocations = new util.ShaderLocationsVault();
        shaderLocationsSet = false;
    }

    /**
     * Specifically checks if the passed rendering context is the correct JOGL-specific
     * rendering context {@link GLAutoDrawable}
     * @param obj the rendering context (should be {@link GLAutoDrawable})
     * @throws IllegalArgumentException if given rendering context is not {@link GLAutoDrawable}
     */
    @Override
    public void setContext(Object obj) throws IllegalArgumentException
    {
        if (obj instanceof GLAutoDrawable)
        {
            glContext = (GLAutoDrawable)obj;
        }
        else
            throw new IllegalArgumentException("Context not of type GLAutoDrawable");
    }

    /**
     * Add a mesh to be drawn later.
     * The rendering context should be set before calling this function, as this function needs it
     * This function creates a new
     * {@link util.ObjectInstance} object for this mesh
     * @param name the name by which this mesh is referred to by the scene graph
     * @param mesh the {@link util.PolygonMesh} object that represents this mesh
     * @throws Exception
     */
    @Override
    public <K extends IVertexData> void addMesh(String name, util.PolygonMesh<K> mesh) throws Exception
    {
        if (!shaderLocationsSet)
            throw new Exception("Attempting to add mesh before setting shader variables. Call initShaderProgram first");
        if (glContext==null)
            throw new Exception("Attempting to add mesh before setting GL context. Call setContext and pass it a GLAutoDrawable first.");

        //verify that the mesh has all the vertex attributes as specified in the map
        if (mesh.getVertexCount()<=0)
            return;
        K vertexData = mesh.getVertexAttributes().get(0);
      GL3 gl = glContext.getGL().getGL3();

      for (Map.Entry<String,String> e:shaderVarsToVertexAttribs.entrySet()) {
            if (!vertexData.hasData(e.getValue()))
                throw new IllegalArgumentException("Mesh does not have vertex attribute "+e.getValue());
        }
      util.ObjectInstance obj = new util.ObjectInstance(gl,
              shaderLocations,shaderVarsToVertexAttribs,mesh,name);

      meshRenderers.put(name,obj);
    }

    @Override
    public void getTexturePaths(Map<String, String> map) {
        this.texturePaths = map;
    }

    @Override
    public util.TextureImage getTextureImage(String name) {

        TextureImage image = null;
        String path = texturePaths.get(name);
        String imageFormat = path.substring(path.indexOf('.')+1);
        try {
            image = new TextureImage(path,imageFormat,name);
        } catch (IOException e) {
            throw new IllegalArgumentException("Texture "+path+" cannot be read!");
        }
        return image;
    }

    /**
     * Begin rendering of the scene graph from the root
     * @param root
     * @param modelView
     */
    @Override
    public void draw(INode root, Stack<Matrix4f> modelView)
    {
        root.draw(this,modelView);
    }

    @Override
    public void dispose()
    {
        for (util.ObjectInstance s:meshRenderers.values())
            s.cleanup(glContext);
    }
    /**
     * Draws a specific mesh.
     * If the mesh has been added to this renderer, it delegates to its correspond mesh renderer
     * This function first passes the material to the shader. Currently it uses the shader variable
     * "vColor" and passes it the ambient part of the material. When lighting is enabled, this method must
     * be overriden to set the ambient, diffuse, specular, shininess etc. values to the shader
     * @param name
     * @param material
     * @param transformation
     */
    @Override
    public void drawMesh(String name, util.Material material,String textureName,final Matrix4f transformation) {
        if (meshRenderers.containsKey(name))
        {
            GL3 gl = glContext.getGL().getGL3();
            //get the color

            FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);
            FloatBuffer fb4 = Buffers.newDirectFloatBuffer(4);

            //get material input variables that need to be given to the shader program

            int materialAmbientLocation, materialDiffuseLocation, materialSpecularLocation, materialShininessLocation;

            materialAmbientLocation = shaderLocations.getLocation("material.ambient");
            if (materialAmbientLocation<0)
                throw new IllegalArgumentException("No shader variable for \" material.ambient \"");

            gl.glUniform3fv(materialAmbientLocation,1,material.getAmbient().get(fb4));
            materialDiffuseLocation = shaderLocations.getLocation("material.diffuse");

            if (materialDiffuseLocation<0)
                throw new IllegalArgumentException("No shader variable for \" material.diffuse \"");

            gl.glUniform3fv(materialDiffuseLocation,1,material.getDiffuse().get(fb4));

            materialSpecularLocation = shaderLocations.getLocation("material.specular");
            if (materialSpecularLocation<0)
                throw new IllegalArgumentException("No shader variable for \" material.specular \"");

            gl.glUniform3fv(materialSpecularLocation,1,material.getSpecular().get(fb4));

            materialShininessLocation = shaderLocations.getLocation("material.shininess");
            if (materialShininessLocation<0)
                throw new IllegalArgumentException("No shader variable for \" material.shininess \"");
            gl.glUniform1f(materialShininessLocation, material.getShininess());

            //get texture input variables that need to be given to the shader program
            gl.glEnable(GL.GL_TEXTURE_2D);
            gl.glActiveTexture(GL.GL_TEXTURE0);

            util.TextureImage texImage = getTextureImage(textureName);
            Texture tex = texImage.getTexture();
            int textureLocation, texturematrixLocation;
            Matrix4f textureTransform;

            tex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
            tex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
            tex.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
            tex.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);

            if (tex.getMustFlipVertically()) // for flipping the image vertically
            {
                textureTransform = new Matrix4f().translate(0, 1, 0).scale(1, -1, 1);
            } else
                textureTransform = new Matrix4f();

            textureLocation = shaderLocations.getLocation("image");
            if (textureLocation<0)
                throw new IllegalArgumentException("No shader variable for \" image \"");
            gl.glUniform1i(textureLocation, 0);




            int projectionLocation, modelviewLocation, normalmatrixLocation;
            fb16 = Buffers.newDirectFloatBuffer(16);
            transformation.get(fb16);

            texturematrixLocation = shaderLocations.getLocation("texturematrix");
            if (texturematrixLocation<0)
                throw new IllegalArgumentException("No shader variable for \" texturematrix \"");
            gl.glUniformMatrix4fv(texturematrixLocation, 1, false, textureTransform.get(fb16));


            projectionLocation = shaderLocations.getLocation("projection");
            if (projectionLocation<0)
                throw new IllegalArgumentException("No shader variable for \" projection \"");
            gl.glUniform3fv(materialSpecularLocation,1,material.getSpecular().get(fb4));

            modelviewLocation = shaderLocations.getLocation("modelview");
            if (modelviewLocation<0)
                throw new IllegalArgumentException("No shader variable for \" modelview \"");
            gl.glUniform3fv(materialSpecularLocation,1,material.getSpecular().get(fb4));

            normalmatrixLocation = shaderLocations.getLocation("normalmatrix");
            if (normalmatrixLocation<0)
                throw new IllegalArgumentException("No shader variable for \" normalmatrix \"");
            gl.glUniform3fv(materialSpecularLocation,1,material.getSpecular().get(fb4));


            tex.bind(gl);
            meshRenderers.get(name).draw(glContext);
        }
    }



    /**
     * Queries the shader program for all variables and locations, and adds them to itself
     * @param shaderProgram
     */
    @Override
    public void initShaderProgram(util.ShaderProgram shaderProgram,Map<String,String> shaderVarsToVertexAttribs)
    {
        Objects.requireNonNull(glContext);
        GL3 gl = glContext.getGL().getGL3();

        shaderLocations = shaderProgram.getAllShaderVariables(gl);
        this.shaderVarsToVertexAttribs = new HashMap<String,String>(shaderVarsToVertexAttribs);
        shaderLocationsSet = true;

    }


    @Override
    public int getShaderLocation(String name)
    {
        return shaderLocations.getLocation(name);
    }
}