package sgraph;

import com.jogamp.opengl.GL3;

import org.joml.Matrix4f;

import org.joml.Vector4f;
import util.IVertexData;
import util.Light;
import util.PolygonMesh;

import java.util.*;
import raytrace.HitRecord;
import raytrace.Ray3D;
import util.TextureImage;

/**
 * A specific implementation of this scene graph. This implementation is still
 * independent of the rendering technology (i.e. OpenGL)
 *
 * @author Amit Shesh
 */
public class Scenegraph<VertexType extends IVertexData> implements IScenegraph<VertexType> {
  /**
   * The root of the scene graph tree
   */
  protected INode root;

  /**
   * A map to store the (name,mesh) pairs. A map is chosen for efficient search
   */
  protected Map<String, util.PolygonMesh<VertexType>> meshes;

  /**
   * A map to store the (name,node) pairs. A map is chosen for efficient search
   */
  protected Map<String, INode> nodes;

  protected Map<String, String> textures;

  /**
   * The associated renderer for this scene graph. This must be set before
   * attempting to render the scene graph
   */
  protected IScenegraphRenderer renderer;


  public Scenegraph() {
    root = null;
    meshes = new HashMap<String, util.PolygonMesh<VertexType>>();
    nodes = new HashMap<String, INode>();
    textures = new HashMap<String, String>();
  }

  public void dispose() {
    renderer.dispose();
  }

  /**
   * Sets the renderer, and then adds all the meshes to the renderer. This
   * function must be called when the scene graph is complete, otherwise not all
   * of its meshes will be known to the renderer
   *
   * @param renderer The {@link IScenegraphRenderer} object that will act as its
   *                 renderer
   */
  @Override
  public void setRenderer(IScenegraphRenderer renderer) throws Exception {
    this.renderer = renderer;

    //now add all the meshes
    for (String meshName : meshes.keySet()) {
      this.renderer.addMesh(meshName, meshes.get(meshName));
    }

    //pass all the texture objects
    for (Map.Entry<String, String> entry : textures.entrySet()) {
      renderer.addTexture(entry.getKey(), entry.getValue());
    }

  }


  /**
   * Set the root of the scenegraph, and then pass a reference to this scene
   * graph object to all its node. This will enable any node to call functions
   * of its associated scene graph
   */

  @Override
  public void makeScenegraph(INode root) {
    this.root = root;
    this.root.setScenegraph(this);

  }

  /**
   * Draw this scene graph. It delegates this operation to the renderer
   */
  @Override
  public void draw(Stack<Matrix4f> modelView) {
    if ((root != null) && (renderer != null)) {
      List<Light> listOfLights = root.getLightsInView(modelView);
      renderer.initLightsInShader(listOfLights);
      renderer.draw(root, modelView);
    }
  }


  @Override
  public void addPolygonMesh(String name, util.PolygonMesh<VertexType> mesh) {
    meshes.put(name, mesh);
  }


  @Override
  public void animate(float time) {

  }

  @Override
  public void addNode(String name, INode node) {
    nodes.put(name, node);
  }


  @Override
  public INode getRoot() {
    return root;
  }

  @Override
  public Map<String, PolygonMesh<VertexType>> getPolygonMeshes() {
    Map<String, util.PolygonMesh<VertexType>> meshes = new HashMap<String, PolygonMesh<VertexType>>(this.meshes);
    return meshes;
  }

  @Override
  public Map<String, INode> getNodes() {
    Map<String, INode> nodes = new TreeMap<String, INode>();
    nodes.putAll(this.nodes);
    return nodes;
  }

  @Override
  public void addTexture(String name, String path) {
    textures.put(name, path);
  }


  @Override
    public List<HitRecord> raycast(Ray3D ray, Stack<Matrix4f> transforms) {
      List<HitRecord> hits = this.root.raycast(ray, transforms);
      return hits;
  }

  @Override
  public List<Light> getLights(Stack<Matrix4f> modelViews) {
      List<Light> lights = root.getLightsInView(modelViews);
      return lights;
  }

  @Override
  public Vector4f getTextureColor(String name, float s, float t) {
      return this.renderer.getTextureColor(name, s, 1f - t);
  }
}
